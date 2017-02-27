package com.fsck.k9.ui.crypto;


import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeUtility;
import okio.ByteString;
import org.openintents.openpgp.OpenPgpInlineKeyUpdate;
import org.openintents.openpgp.util.OpenPgpApi;
import timber.log.Timber;


public class AutocryptOperations {
    private static final String AUTOCRYPT_PARAM_KEY_DATA = "key";
    private static final String AUTOCRYPT_PARAM_TO = "to";
    private static final String AUTOCRYPT_HEADER = "Autocrypt";
    private static final String AUTOCRYPT_PARAM_TYPE = "type";


    public AutocryptOperations() {
    }


    private boolean addKeyFromAutocryptHeaderToIntentIfPresent(MimeMessage currentMessage, Intent decryptIntent) {
        AutocryptHeader autocryptHeader = getValidAutocryptHeader(currentMessage);
        if (autocryptHeader == null) {
            return false;
        }

        String messageFromAddress = currentMessage.getFrom()[0].getAddress();
        if (!autocryptHeader.to.equalsIgnoreCase(messageFromAddress)) {
            return false;
        }

        Date messageDate = currentMessage.getSentDate();
        Date internalDate = currentMessage.getInternalDate();
        Date effectiveDate = messageDate.before(internalDate) ? messageDate : internalDate;

        OpenPgpInlineKeyUpdate data = OpenPgpInlineKeyUpdate.createOpenPgpInlineKeyUpdate(autocryptHeader.keyData, effectiveDate);
        decryptIntent.putExtra(OpenPgpApi.EXTRA_INLINE_KEY_DATA, data);
        return true;
    }

    void processUnsignedMessage(OpenPgpApi openPgpApi, MimeMessage currentMessage) {
        Intent intent = new Intent(OpenPgpApi.ACTION_UPDATE_TRUST_ID);
        boolean hasInlineKeyData = addKeyFromAutocryptHeaderToIntentIfPresent(currentMessage, intent);
        if (hasInlineKeyData) {
            String trustId = currentMessage.getFrom()[0].getAddress();
            intent.putExtra(OpenPgpApi.EXTRA_TRUST_IDENTITY, trustId);
            openPgpApi.executeApi(intent, (InputStream) null, null);
        }
    }

    @Nullable
    @VisibleForTesting
    AutocryptHeader getValidAutocryptHeader(MimeMessage currentMessage) {
        String[] headers = currentMessage.getHeader(AUTOCRYPT_HEADER);
        ArrayList<AutocryptHeader> autocryptHeaders = parseAllAutocryptHeaders(headers);

        boolean isSingleValidHeader = autocryptHeaders.size() == 1;
        return isSingleValidHeader ? autocryptHeaders.get(0) : null;
    }

    @NonNull
    private ArrayList<AutocryptHeader> parseAllAutocryptHeaders(String[] headers) {
        ArrayList<AutocryptHeader> autocryptHeaders = new ArrayList<>();
        for (String header : headers) {
            AutocryptHeader autocryptHeader = parseAutocryptHeader(header);
            if (autocryptHeader != null) {
                autocryptHeaders.add(autocryptHeader);
            }
        }
        return autocryptHeaders;
    }

    @Nullable
    private AutocryptHeader parseAutocryptHeader(String headerValue) {
        Map<String,String> parameters = MimeUtility.getAllHeaderParameters(headerValue);

        String type = parameters.remove(AUTOCRYPT_PARAM_TYPE);
        if (type != null && !type.equals("p")) {
            Timber.e("autocrypt: unsupported type parameter %s", type);
            return null;
        }

        String base64KeyData = parameters.remove(AUTOCRYPT_PARAM_KEY_DATA);
        if (base64KeyData == null) {
            Timber.e("autocrypt: missing key parameter");
            return null;
        }

        ByteString byteString = ByteString.decodeBase64(base64KeyData);
        if (byteString == null) {
            Timber.e("autocrypt: error parsing base64 data");
            return null;
        }

        String to = parameters.remove(AUTOCRYPT_PARAM_TO);
        if (to == null) {
            Timber.e("autocrypt: no to header!");
            return null;
        }


        if (hasCriticalParameters(parameters)) {
            return null;
        }

        return new AutocryptHeader(parameters, to, byteString.toByteArray());
    }

    private boolean hasCriticalParameters(Map<String, String> parameters) {
        for (String parameterName : parameters.keySet()) {
            if (parameterName != null && !parameterName.startsWith("_")) {
                return true;
            }
        }
        return false;
    }

    boolean hasAutocryptHeader(MimeMessage currentMessage) {
        return currentMessage.getHeader(AUTOCRYPT_HEADER).length > 0;
    }

    public void attachKeyInOpenPgpHeader(OpenPgpApi openPgpApi, MimeMessage message, String autocryptAddress, long keyId) {
        HashMap<String,String> parameters = new HashMap<>();
        parameters.put(AUTOCRYPT_PARAM_TO, autocryptAddress);

        byte[] keyData = getKeyMaterialFromApi(openPgpApi, keyId, autocryptAddress);

        AutocryptHeader autocryptHeader = new AutocryptHeader(parameters, autocryptAddress, keyData);
        String rawAutocryptHeader = autocryptHeaderToString(autocryptHeader);

        message.addRawHeader(AUTOCRYPT_HEADER, rawAutocryptHeader);
    }

    private byte[] getKeyMaterialFromApi(OpenPgpApi openPgpApi, long keyId, String autocryptAddress) {
        Intent gimmeKeyIntent = new Intent(OpenPgpApi.ACTION_GET_KEY);
        gimmeKeyIntent.putExtra(OpenPgpApi.EXTRA_KEY_ID, keyId);
        gimmeKeyIntent.putExtra(OpenPgpApi.EXTRA_MINIMIZE, true);
        gimmeKeyIntent.putExtra(OpenPgpApi.EXTRA_MINIMIZE_USER_ID, autocryptAddress);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Intent result = openPgpApi.executeApi(gimmeKeyIntent, (InputStream) null, baos);

        if (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR) ==
                OpenPgpApi.RESULT_CODE_SUCCESS) {
            return baos.toByteArray();
        } else{
            return null;
        }
    }

    private String autocryptHeaderToString(AutocryptHeader autocryptHeader) {
        String autocryptHeaderString = AutocryptOperations.AUTOCRYPT_HEADER + ": ";
        autocryptHeaderString += AutocryptOperations.AUTOCRYPT_PARAM_TO + "=" + autocryptHeader.to + ";";
        autocryptHeaderString += AutocryptOperations.AUTOCRYPT_PARAM_KEY_DATA + "=" +
                ByteString.of(autocryptHeader.keyData).base64();
        StringBuilder headerLines = new StringBuilder();
        for (int i = 0, j = autocryptHeaderString.length(); i < j; i += 76) {
            if (i +76 > j) {
                headerLines.append(autocryptHeaderString.substring(i)).append("\n ");
            } else {
                headerLines.append(autocryptHeaderString.substring(i, i+76)).append("\n ");
            }
        }

        return headerLines.toString();
    }

    @VisibleForTesting
    class AutocryptHeader {
        final byte[] keyData;
        final String to;
        final Map<String,String> parameters;

        private AutocryptHeader(Map<String, String> parameters, String to, byte[] keyData) {
            this.parameters = parameters;
            this.to = to;
            this.keyData = keyData;
        }
    }
}

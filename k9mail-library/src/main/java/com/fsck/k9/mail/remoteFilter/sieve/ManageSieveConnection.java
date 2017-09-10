package com.fsck.k9.mail.remoteFilter.sieve;


import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.Set;

import android.net.ConnectivityManager;

import com.fluffypeople.managesieve.ManageSieveClient;
import com.fluffypeople.managesieve.ManageSieveResponse;
import com.fluffypeople.managesieve.ParseException;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.filter.PeekableInputStream;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import javax.net.ssl.SSLException;
import timber.log.Timber;

import static com.fsck.k9.mail.store.RemoteMailStore.SOCKET_CONNECT_TIMEOUT;
import static com.fsck.k9.mail.store.RemoteMailStore.SOCKET_READ_TIMEOUT;


class ManageSieveConnection {
    private static final int BUFFER_SIZE = 1024;


    private final ConnectivityManager connectivityManager;
    private final OAuth2TokenProvider oauthTokenProvider;
    private final TrustedSocketFactory socketFactory;
    private final int socketConnectTimeout;
    private final int socketReadTimeout;

    private Socket socket;
    private PeekableInputStream inputStream;
    private OutputStream outputStream;
    private SieveResponseParser responseParser;
    private int nextCommandTag;
    private Set<String> capabilities = new HashSet<String>();
    private SieveSettings settings;
    private Exception stacktraceForClose;
    private boolean open = false;
    private boolean retryXoauth2WithNewToken = true;
    private ManageSieveClient client;

    public ManageSieveConnection(SieveSettings sieveSettings, TrustedSocketFactory trustedSocketFactory,
            ConnectivityManager connectivityManager, OAuth2TokenProvider oauthTokenProvider) {
        this.settings = sieveSettings;
        this.connectivityManager = connectivityManager;
        this.socketFactory = trustedSocketFactory;
        this.oauthTokenProvider = oauthTokenProvider;
        this.socketConnectTimeout = SOCKET_CONNECT_TIMEOUT;
        this.socketReadTimeout = SOCKET_READ_TIMEOUT;
    }

    ManageSieveConnection(SieveSettings sieveSettings, TrustedSocketFactory trustedSocketFactory,
            ConnectivityManager connectivityManager, OAuth2TokenProvider oauthTokenProvider, int socketConnectTimeout, int socketReadTimeout) {
        this.settings = sieveSettings;
        this.connectivityManager = connectivityManager;
        this.socketFactory = trustedSocketFactory;
        this.oauthTokenProvider = oauthTokenProvider;
        this.socketConnectTimeout = socketConnectTimeout;
        this.socketReadTimeout = socketReadTimeout;
    }

    void open() throws IOException, MessagingException {
        if (open) {
            return;
        } else if (stacktraceForClose != null) {
            throw new IllegalStateException("open() called after close(). " +
                    "Check wrapped exception to see where close() was called.", stacktraceForClose);
        }

        open = true;
        boolean authSuccess = false;
        nextCommandTag = 1;

        try {
            client = new com.fluffypeople.managesieve.ManageSieveClient();
            client.connect(settings.getHost(), settings.getPort());
            if (settings.getConnectionSecurity().equals(ConnectionSecurity.STARTTLS_REQUIRED)) {
                ManageSieveResponse response = client.starttls();
                if (!response.isOk()) {
                    throw new AuthenticationFailedException("STARTTLS failed: " + response.getMessage());
                }
            }
            if (settings.getAuthType().equals(AuthType.PLAIN)) {
                ManageSieveResponse response = client.authenticate(settings.getUsername(), settings.getPassword());
                if (!response.isOk()) {
                    throw new AuthenticationFailedException("Authentication failed: " + response.getMessage());
                }
            }
            authSuccess = true;
        } catch (ParseException e) {
            throw new MessagingException("SIEVE parsing exception", e);
        } catch (SSLException e) {
            if (e.getCause() instanceof CertificateException) {
                throw new CertificateValidationException(e.getMessage(), e);
            } else {
                throw e;
            }
        } finally {
            if (!authSuccess) {
                Timber.e("Failed to login, closing connection for %s", getLogId());
                close();
            }
        }
    }

    protected String getLogId() {
        return "conn" + hashCode();
    }


    void close() {
        try {
            client.logout();
        } catch (IOException | ParseException ignored) {}
        client = null;
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public void sendNoop() throws IOException, ParseException {
        client.noop(null);
    }
}

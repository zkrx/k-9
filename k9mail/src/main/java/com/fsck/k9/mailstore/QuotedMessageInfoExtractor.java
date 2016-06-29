package com.fsck.k9.mailstore;


import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.WorkerThread;

import com.fsck.k9.Account.MessageFormat;
import com.fsck.k9.Account.QuoteStyle;
import com.fsck.k9.Globals;
import com.fsck.k9.helper.QuotedMessageHelper;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.message.InsertableHtmlContent;
import com.fsck.k9.message.SimpleMessageFormat;
import com.fsck.k9.ui.crypto.MessageCryptoAnnotations;
import com.fsck.k9.ui.message.LocalMessageExtractorLoader.MessageInfoExtractor;


public class QuotedMessageInfoExtractor implements MessageInfoExtractor<QuotedMessageInfo> {
    private final Context context;
    private final MessageFormat requestedMessageFormat;
    private final boolean shouldStripSignature;
    private final QuoteStyle quoteStyle;
    private final String quotePrefix;


    public static QuotedMessageInfoExtractor getInstance(MessageFormat requestedMessageFormat,
            boolean shouldStripSignature, QuoteStyle quoteStyle, String quotePrefix) {
        Context context = Globals.getContext();
        return new QuotedMessageInfoExtractor(
                context, requestedMessageFormat, shouldStripSignature, quoteStyle, quotePrefix);
    }

    private QuotedMessageInfoExtractor(Context context, MessageFormat requestedMessageFormat,
            boolean shouldStripSignature, QuoteStyle quoteStyle, String quotePrefix) {
        this.context = context;
        this.requestedMessageFormat = requestedMessageFormat;
        this.shouldStripSignature = shouldStripSignature;
        this.quoteStyle = quoteStyle;
        this.quotePrefix = quotePrefix;
    }


    @WorkerThread
    public QuotedMessageInfo extractMessageInfo(Message message, MessageCryptoAnnotations annotations)
            throws MessagingException {

        // TODO this method is not done!

        MessageViewInfo messageViewInfo =
                MessageViewInfoExtractor.getInstance().extractMessageInfo(message, annotations);
        Part rootPart = messageViewInfo.rootPart;

        SimpleMessageFormat quotedTextFormat;
        if (requestedMessageFormat == MessageFormat.TEXT) {
            // Use plain text for the quoted message
            quotedTextFormat = SimpleMessageFormat.TEXT;
        } else if (requestedMessageFormat == MessageFormat.AUTO) {
            // Figure out which message format to use for the quoted text by looking if the source
            // message contains a text/html part. If it does, we use that.
            quotedTextFormat =
                    (MimeUtility.findFirstPartByMimeType(rootPart, "text/html") == null) ?
                            SimpleMessageFormat.TEXT : SimpleMessageFormat.HTML;
        } else {
            quotedTextFormat = SimpleMessageFormat.HTML;
        }

        // Handle the original message in the reply
        // If we already have sourceMessageBody, use that.  It's pre-populated if we've got crypto going on.
        String content = QuotedMessageHelper.getBodyTextFromMessage(rootPart, quotedTextFormat);

        InsertableHtmlContent quotedHtmlText = null;
        String quotedPlainText;

        Resources resources = context.getResources();
        if (quotedTextFormat == SimpleMessageFormat.HTML) {
            // Strip signature.
            // closing tags such as </div>, </span>, </table>, </pre> will be cut off.
            if (shouldStripSignature) {
                content = QuotedMessageHelper.stripSignatureForHtmlMessage(content);
            }

            // Add the HTML reply header to the top of the content.
            quotedHtmlText = QuotedMessageHelper.quoteOriginalHtmlMessage(
                    resources, message, content, quoteStyle);

            // TODO: Also strip the signature from the text/plain part
            quotedPlainText = QuotedMessageHelper.quoteOriginalTextMessage(resources, message,
                    QuotedMessageHelper.getBodyTextFromMessage(rootPart, SimpleMessageFormat.TEXT),
                    quoteStyle, quotePrefix);
        } else {
            if (shouldStripSignature) {
                content = QuotedMessageHelper.stripSignatureForTextMessage(content);
            }

            quotedPlainText = QuotedMessageHelper.quoteOriginalTextMessage(
                    resources, message, content, quoteStyle, quotePrefix);
        }

        return QuotedMessageInfo.createWithExtractedContent(message, messageViewInfo.rootPart, quotedPlainText,
                quotedHtmlText, messageViewInfo.attachments, messageViewInfo.cryptoResultAnnotation,
                messageViewInfo.attachmentResolver);
    }

}

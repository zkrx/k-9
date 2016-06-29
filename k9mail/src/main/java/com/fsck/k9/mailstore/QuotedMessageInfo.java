package com.fsck.k9.mailstore;


import java.util.List;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import com.fsck.k9.message.InsertableHtmlContent;


public class QuotedMessageInfo {
    public final Message message;
    public final Part rootPart;
    public final String quotedPlainText;
    public final InsertableHtmlContent quotedHtmlText;
    public final CryptoResultAnnotation cryptoResultAnnotation;
    public final List<AttachmentViewInfo> attachments;
    public final AttachmentResolver attachmentResolver;


    private QuotedMessageInfo(Message message, Part rootPart, String quotedPlainText,
            InsertableHtmlContent quotedHtmlText, List<AttachmentViewInfo> attachments,
            CryptoResultAnnotation cryptoResultAnnotation, AttachmentResolver attachmentResolver) {
        this.message = message;
        this.rootPart = rootPart;
        this.quotedPlainText = quotedPlainText;
        this.quotedHtmlText = quotedHtmlText;
        this.cryptoResultAnnotation = cryptoResultAnnotation;
        this.attachments = attachments;
        this.attachmentResolver = attachmentResolver;
    }

    public static QuotedMessageInfo createWithExtractedContent(Message message, Part rootPart,
            String quotedPlainText, InsertableHtmlContent quotedHtmlText, List<AttachmentViewInfo> attachments,
            CryptoResultAnnotation cryptoResultAnnotation, AttachmentResolver attachmentResolver) {
        return new QuotedMessageInfo(
                message, rootPart, quotedPlainText, quotedHtmlText, attachments, cryptoResultAnnotation,
                attachmentResolver);
    }
}

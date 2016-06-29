package com.fsck.k9.ui.message;


import android.content.AsyncTaskLoader;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.ui.crypto.MessageCryptoAnnotations;


public class LocalMessageExtractorLoader<T> extends AsyncTaskLoader<T> {
    private final MessageInfoExtractor<T> messageInfoExtractor;
    private final Message message;

    private T messageViewInfo;


    @Nullable
    private MessageCryptoAnnotations annotations;

    public LocalMessageExtractorLoader(
            Context context, MessageInfoExtractor<T> messageInfoExtractor, Message message,
            @Nullable MessageCryptoAnnotations annotations) {
        super(context);
        this.messageInfoExtractor = messageInfoExtractor;
        this.message = message;
        this.annotations = annotations;
    }

    @Override
    protected void onStartLoading() {
        if (messageViewInfo != null) {
            super.deliverResult(messageViewInfo);
        }

        if (takeContentChanged() || messageViewInfo == null) {
            forceLoad();
        }
    }

    @Override
    public void deliverResult(T messageViewInfo) {
        this.messageViewInfo = messageViewInfo;
        super.deliverResult(messageViewInfo);
    }

    @Override
    @WorkerThread
    public T loadInBackground() {
        try {
            return messageInfoExtractor.extractMessageInfo(message, annotations);
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Error while decoding message", e);
            return null;
        }
    }

    public boolean isCreatedFor(MessageInfoExtractor<T> messageInfoExtractor, LocalMessage localMessage,
            MessageCryptoAnnotations messageCryptoAnnotations) {
        return messageInfoExtractor.equals(this.messageInfoExtractor) &&
                annotations == messageCryptoAnnotations && message.equals(localMessage);
    }

    public interface MessageInfoExtractor<T> {
        T extractMessageInfo(Message message, MessageCryptoAnnotations annotations) throws MessagingException;
    }
}

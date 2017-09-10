
package com.fsck.k9.mail.transport;

import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.store.StoreConfig;
import com.fsck.k9.mail.store.webdav.WebDavHttpClient;
import com.fsck.k9.mail.store.webdav.WebDavMailStore;
import timber.log.Timber;

import java.util.Collections;

public class WebDavTransport extends Transport {
    private WebDavMailStore store;

    public WebDavTransport(StoreConfig storeConfig) throws MessagingException {
        store = new WebDavMailStore(storeConfig, new WebDavHttpClient.WebDavHttpClientFactory());

        if (K9MailLib.isDebug())
            Timber.d(">>> New WebDavTransport creation complete");
    }

    @Override
    public void open() throws MessagingException {
        if (K9MailLib.isDebug())
            Timber.d( ">>> open called on WebDavTransport ");

        store.getHttpClient();
    }

    @Override
    public void close() {
    }

    @Override
    public void sendMessage(Message message) throws MessagingException {
        store.sendMessages(Collections.singletonList(message));
    }
}

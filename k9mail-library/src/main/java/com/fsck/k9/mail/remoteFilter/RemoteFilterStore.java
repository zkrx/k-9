package com.fsck.k9.mail.remoteFilter;


import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.net.ConnectivityManager;

import com.fsck.k9.mail.FilterStore;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.remoteFilter.sieve.ManageSieveStore;
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.StoreConfig;
import com.fsck.k9.mail.store.imap.ImapMailStore;
import com.fsck.k9.mail.store.pop3.Pop3MailStore;
import com.fsck.k9.mail.store.webdav.WebDavMailStore;


public abstract class RemoteFilterStore implements FilterStore {

    private static Map<String, RemoteFilterStore> stores = new HashMap<>();
    protected final StoreConfig storeConfig;
    protected final TrustedSocketFactory trustedSocketFactory;

    /**
     * Decodes the contents of store-specific URIs and puts them into a {@link com.fsck.k9.mail.ServerSettings}
     * object.
     *
     * @param uri
     *         the store-specific URI to decode
     *
     * @return A {@link com.fsck.k9.mail.ServerSettings} object holding the settings contained in the URI.
     *
     * @see ImapMailStore#decodeUri(String)
     * @see Pop3MailStore#decodeUri(String)
     * @see WebDavMailStore#decodeUri(String)
     */
    public static ServerSettings decodeStoreUri(String uri) {
        if (uri.startsWith("sieve")) {
            return ManageSieveStore.decodeUri(uri);
        } else {
            throw new IllegalArgumentException("Not a valid filter store URI");
        }
    }

    /**
     * Get an instance of a remote store.
     */
    public static synchronized RemoteFilterStore getInstance(Context context, StoreConfig storeConfig)
            throws MessagingException {
        String uri = storeConfig.getRemoteFilterStoreUri();

        if (uri.startsWith("local")) {
            throw new RuntimeException("Asked to get a local remote filter store");
        }

        RemoteFilterStore store = stores.get(uri);
        OAuth2TokenProvider oAuth2TokenProvider = null;
        if (store == null) {
            if (uri.startsWith("sieve")) {
                store = new ManageSieveStore(
                        storeConfig,
                        new DefaultTrustedSocketFactory(context),
                        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE),
                        oAuth2TokenProvider);
            }

            if (store != null) {
                stores.put(uri, store);
            }
        }

        if (store == null) {
            throw new MessagingException("Unable to locate an applicable Remote Filter MailStore for " + uri);
        }

        return store;
    }

    public RemoteFilterStore(StoreConfig storeConfig, TrustedSocketFactory trustedSocketFactory) {
        this.storeConfig = storeConfig;
        this.trustedSocketFactory = trustedSocketFactory;
    }
}

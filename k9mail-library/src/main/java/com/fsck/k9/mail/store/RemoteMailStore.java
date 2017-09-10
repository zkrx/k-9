package com.fsck.k9.mail.store;


import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.net.ConnectivityManager;

import com.fsck.k9.mail.MailStore;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.imap.ImapMailStore;
import com.fsck.k9.mail.store.pop3.Pop3MailStore;
import com.fsck.k9.mail.store.webdav.WebDavHttpClient;
import com.fsck.k9.mail.store.webdav.WebDavMailStore;


public abstract class RemoteMailStore extends MailStore {
    public static final int SOCKET_CONNECT_TIMEOUT = 30000;
    public static final int SOCKET_READ_TIMEOUT = 60000;

    protected StoreConfig mStoreConfig;
    protected TrustedSocketFactory mTrustedSocketFactory;

    /**
     * Remote stores indexed by Uri.
     */
    private static Map<String, RemoteMailStore> sStores = new HashMap<String, RemoteMailStore>();


    public RemoteMailStore(StoreConfig storeConfig, TrustedSocketFactory trustedSocketFactory) {
        mStoreConfig = storeConfig;
        mTrustedSocketFactory = trustedSocketFactory;
    }

    /**
     * Get an instance of a remote mail store.
     */
    public static synchronized RemoteMailStore getInstance(Context context, StoreConfig storeConfig) throws MessagingException {
        String uri = storeConfig.getStoreUri();

        if (uri.startsWith("local")) {
            throw new RuntimeException("Asked to get non-local MailStore object but given LocalStore URI");
        }

        RemoteMailStore store = sStores.get(uri);
        if (store == null) {
            if (uri.startsWith("imap")) {
                OAuth2TokenProvider oAuth2TokenProvider = null;
                store = new ImapMailStore(
                        storeConfig,
                        new DefaultTrustedSocketFactory(context),
                        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE),
                        oAuth2TokenProvider);
            } else if (uri.startsWith("pop3")) {
                store = new Pop3MailStore(storeConfig, new DefaultTrustedSocketFactory(context));
            } else if (uri.startsWith("webdav")) {
                store = new WebDavMailStore(storeConfig, new WebDavHttpClient.WebDavHttpClientFactory());
            }

            if (store != null) {
                sStores.put(uri, store);
            }
        }

        if (store == null) {
            throw new MessagingException("Unable to locate an applicable MailStore for " + uri);
        }

        return store;
    }

    /**
     * Release reference to a remote mail store instance.
     *
     * @param storeConfig {@link com.fsck.k9.mail.store.StoreConfig} instance that is used to get the remote mail store instance.
     */
    public static void removeInstance(StoreConfig storeConfig) {
        String uri = storeConfig.getStoreUri();
        if (uri.startsWith("local")) {
            throw new RuntimeException("Asked to get non-local MailStore object but given " +
                    "LocalStore URI");
        }
        sStores.remove(uri);
    }

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
        if (uri.startsWith("imap")) {
            return ImapMailStore.decodeUri(uri);
        } else if (uri.startsWith("pop3")) {
            return Pop3MailStore.decodeUri(uri);
        } else if (uri.startsWith("webdav")) {
            return WebDavMailStore.decodeUri(uri);
        } else {
            throw new IllegalArgumentException("Not a valid store URI");
        }
    }

    /**
     * Creates a store URI from the information supplied in the {@link com.fsck.k9.mail.ServerSettings} object.
     *
     * @param server
     *         The {@link com.fsck.k9.mail.ServerSettings} object that holds the server settings.
     *
     * @return A store URI that holds the same information as the {@code server} parameter.
     *
     * @see ImapMailStore#createUri(com.fsck.k9.mail.ServerSettings)
     * @see Pop3MailStore#createUri(com.fsck.k9.mail.ServerSettings)
     * @see WebDavMailStore#createUri(com.fsck.k9.mail.ServerSettings)
     */
    public static String createStoreUri(ServerSettings server) {
        if (Type.IMAP == server.type) {
            return ImapMailStore.createUri(server);
        } else if (Type.POP3 == server.type) {
            return Pop3MailStore.createUri(server);
        } else if (Type.WebDAV == server.type) {
            return WebDavMailStore.createUri(server);
        } else {
            throw new IllegalArgumentException("Not a valid store URI");
        }
    }
}

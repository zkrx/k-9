package com.fsck.k9.mail.remoteFilter.sieve;

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;

import android.net.ConnectivityManager;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.remoteFilter.RemoteFilterStore;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.StoreConfig;


/**
 * Implements remote filter management using the ManageSieve protocol.
 */
public class ManageSieveStore extends RemoteFilterStore {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final ConnectionSecurity connectionSecurity;
    private final ConnectivityManager connectivityManager;
    private final Deque<ManageSieveConnection> connections = new LinkedList<>();
    private OAuth2TokenProvider oauthTokenProvider;
    private String clientCertificateAlias;

    public ManageSieveStore(StoreConfig storeConfig, TrustedSocketFactory trustedSocketFactory,
            ConnectivityManager connectivityManager, OAuth2TokenProvider oAuth2TokenProvider) throws MessagingException {
        super(storeConfig, trustedSocketFactory);

        ManageSieveStoreSettings settings;
        try {
            settings = decodeUri(storeConfig.getRemoteFilterStoreUri());
        } catch (IllegalArgumentException e) {
            throw new MessagingException("Error while decoding store URI", e);
        }

        host = settings.host;
        port = settings.port;
        username = settings.username;
        password = settings.password;
        clientCertificateAlias = settings.clientCertificateAlias;
        oauthTokenProvider = oAuth2TokenProvider;

        connectionSecurity = settings.connectionSecurity;
        this.connectivityManager = connectivityManager;

    }

    ManageSieveConnection getConnection() throws MessagingException {
        ManageSieveConnection connection;
        while ((connection = pollConnection()) != null) {
            try {
                connection.sendNoop();
                break;
            } catch (Exception e) {
                connection.close();
            }
        }

        if (connection == null) {
            connection = createManageSieveConnection();
        }

        return connection;
    }

    private ManageSieveConnection pollConnection() {
        synchronized (connections) {
            return connections.poll();
        }
    }

    private ManageSieveConnection createManageSieveConnection() {

        return new ManageSieveConnection(
                new StoreSieveSettings(),
                trustedSocketFactory,
                connectivityManager,
                oauthTokenProvider);
    }

    public static ManageSieveStoreSettings decodeUri(String uri) {
        return ManageSieveStoreUriDecoder.decode(uri);
    }

    @Override
    public void checkSettings() throws MessagingException {
        try {
            ManageSieveConnection connection = createManageSieveConnection();
            connection.open();
            connection.close();
        } catch (IOException ioe) {
            throw new MessagingException("Unable to connect", ioe);
        }
    }

    private class StoreSieveSettings implements SieveSettings {
        @Override
        public ConnectionSecurity getConnectionSecurity() {
            return connectionSecurity;
        }

        @Override
        public String getHost() {
            return host;
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public String getClientCertificateAlias() {
            return clientCertificateAlias;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public AuthType getAuthType() {
            return null;
        }
    }
}

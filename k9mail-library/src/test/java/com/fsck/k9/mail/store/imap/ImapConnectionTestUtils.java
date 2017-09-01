package com.fsck.k9.mail.store.imap;


import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.net.ConnectivityManager;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.imap.mockserver.MockImapServer;
import okio.ByteString;

import static org.junit.Assert.assertEquals;


class ImapConnectionTestUtils {
    static final String USERNAME = "user";
    static final String PASSWORD = "123456";
    private static final int SOCKET_CONNECT_TIMEOUT = 10000;
    private static final int SOCKET_READ_TIMEOUT = 10000;
    private static final String XOAUTH_TOKEN = "token";
    private static final String XOAUTH_ANOTHER_TOKEN = "token2";
    static final String XOAUTH_STRING = ByteString.encodeUtf8(
            "user=" + USERNAME + "\001auth=Bearer " + XOAUTH_TOKEN + "\001\001").base64();
    static final String XOAUTH_STRING_RETRY = ByteString.encodeUtf8(
            "user=" + USERNAME + "\001auth=Bearer " + XOAUTH_ANOTHER_TOKEN + "\001\001").base64();


    static ImapConnection createImapConnection(ImapSettings settings, TrustedSocketFactory socketFactory,
            ConnectivityManager connectivityManager, OAuth2TokenProvider oAuth2TokenProvider) {
        return new ImapConnection(settings, socketFactory, connectivityManager, oAuth2TokenProvider,
                SOCKET_CONNECT_TIMEOUT, SOCKET_READ_TIMEOUT);
    }

    static ImapConnection startServerAndCreateImapConnection(
            MockImapServer server, SimpleImapSettings settings, TrustedSocketFactory socketFactory,
            ConnectivityManager connectivityManager, OAuth2TokenProvider oAuth2TokenProvider) throws IOException {
        server.start();
        settings.setHost(server.getHost());
        settings.setPort(server.getPort());
        return createImapConnection(settings, socketFactory, connectivityManager, oAuth2TokenProvider);
    }

    static ImapConnection simpleOpen(MockImapServer server, SimpleImapSettings settings, TrustedSocketFactory socketFactory,
            ConnectivityManager connectivityManager, OAuth2TokenProvider oAuth2TokenProvider) throws Exception {
        return simpleOpenWithCapabilities(server, settings, socketFactory, connectivityManager, oAuth2TokenProvider, "");
    }

    static ImapConnection simpleOpenWithCapabilities(MockImapServer server, SimpleImapSettings settings,
            TrustedSocketFactory socketFactory,
            ConnectivityManager connectivityManager, OAuth2TokenProvider oAuth2TokenProvider, String postAuthCapabilities)
            throws Exception {
        simpleOpenDialog(server, settings, postAuthCapabilities);

        ImapConnection imapConnection = startServerAndCreateImapConnection(server, settings, socketFactory, connectivityManager, oAuth2TokenProvider);
        imapConnection.open();

        return imapConnection;
    }

    static void preAuthenticationDialog(MockImapServer server) {
        preAuthenticationDialog(server, "");
    }

    static void preAuthenticationDialog(MockImapServer server, String capabilities) {
        server.output("* OK IMAP4rev1 Service Ready");
        server.expect("1 CAPABILITY");
        server.output("* CAPABILITY IMAP4 IMAP4REV1 " + capabilities);
        server.output("1 OK CAPABILITY");
    }

    static void postAuthenticationDialogRequestingCapabilities(MockImapServer server) {
        postAuthenticationDialogRequestingCapabilities(server, 3);
    }

    static void postAuthenticationDialogRequestingCapabilities(MockImapServer server, int tag) {
        requestCapabilities(server, tag);
        simplePostAuthenticationDialog(server, tag + 1);
    }

    static void requestCapabilities(MockImapServer server, int tag) {
        server.expect(tag + " CAPABILITY");
        server.output("* CAPABILITY IMAP4 IMAP4REV1 ");
        server.output(tag + " OK CAPABILITY");
    }

    static void simplePostAuthenticationDialog(MockImapServer server, int tag) {
        server.expect(tag + " LIST \"\" \"\"");
        server.output("* LIST () \"/\" foo/bar");
        server.output(tag + " OK");
    }

    static void simpleOpenDialog(MockImapServer server, SimpleImapSettings settings, String postAuthCapabilities) {
        simplePreAuthAndLoginDialog(server, settings, postAuthCapabilities);
        simplePostAuthenticationDialog(server, 3);
    }

    static void simplePreAuthAndLoginDialog(MockImapServer server, SimpleImapSettings settings, String postAuthCapabilities) {
        settings.setAuthType(AuthType.PLAIN);

        preAuthenticationDialog(server);

        server.expect("2 LOGIN \"" + USERNAME + "\" \"" + PASSWORD + "\"");
        server.output("2 OK [CAPABILITY " + postAuthCapabilities + "] LOGIN completed");
    }

    static OAuth2TokenProvider createOAuth2TokenProvider() throws AuthenticationFailedException {
        return new OAuth2TokenProvider() {
            private int invalidationCount = 0;

            @Override
            public String getToken(String username, long timeoutMillis) throws AuthenticationFailedException {
                assertEquals(USERNAME, username);
                assertEquals(OAUTH2_TIMEOUT, timeoutMillis);

                switch (invalidationCount) {
                    case 0: {
                        return XOAUTH_TOKEN;
                    }
                    case 1: {
                        return XOAUTH_ANOTHER_TOKEN;
                    }
                    default: {
                        throw new AssertionError("Ran out of auth tokens. invalidateToken() called too often?");
                    }
                }
            }

            @Override
            public void invalidateToken(String username) {
                assertEquals(USERNAME, username);
                invalidationCount++;
            }

            @Override
            public List<String> getAccounts() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void authorizeApi(String username, Activity activity, OAuth2TokenProviderAuthCallback callback) {
                throw new UnsupportedOperationException();
            }
        };
    }
}

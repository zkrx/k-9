package com.fsck.k9.mail.remoteFilter.sieve;


import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.net.ConnectivityManager;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.K9LibRobolectricTestRunner;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.helpers.TestTrustedSocketFactory;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.managesieve.Base64;
import okio.ByteString;
import org.bouncycastle.jce.provider.symmetric.ARC4.Base;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;


@RunWith(K9LibRobolectricTestRunner.class)
public class ManageSieveConnectionTest {
    private static final boolean DEBUGGING = false;

    private static final String USERNAME = "user";
    private static final String PASSWORD = "123456";
    private static final int SOCKET_CONNECT_TIMEOUT = 10000;
    private static final int SOCKET_READ_TIMEOUT = 10000;
    private static final String XOAUTH_TOKEN = "token";
    private static final String XOAUTH_ANOTHER_TOKEN = "token2";
    private static final String XOAUTH_STRING = ByteString.encodeUtf8(
            "user=" + USERNAME + "\001auth=Bearer " + XOAUTH_TOKEN + "\001\001").base64();
    private static final String XOAUTH_STRING_RETRY = ByteString.encodeUtf8(
            "user=" + USERNAME + "\001auth=Bearer " + XOAUTH_ANOTHER_TOKEN + "\001\001").base64();


    private TrustedSocketFactory socketFactory;
    private ConnectivityManager connectivityManager;
    private OAuth2TokenProvider oAuth2TokenProvider;
    private SimpleSieveSettings settings;


    @Before
    public void setUp() throws Exception {
        connectivityManager = mock(ConnectivityManager.class);
        oAuth2TokenProvider = createOAuth2TokenProvider();
        socketFactory = TestTrustedSocketFactory.newInstance();

        settings = new SimpleSieveSettings();
        settings.setConnectionSecurity(ConnectionSecurity.NONE);
        settings.setUsername(USERNAME);
        settings.setPassword(PASSWORD);

        if (DEBUGGING) {
            ShadowLog.stream = System.out;
            K9MailLib.setDebug(true);
            K9MailLib.setDebugSensitive(true);
        }
    }

    private OAuth2TokenProvider createOAuth2TokenProvider() throws AuthenticationFailedException {
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

    private ManageSieveConnection startServerAndCreateSieveConnection(MockSieveServer server) throws IOException {
        server.start();
        settings.setHost(server.getHost());
        settings.setPort(server.getPort());
        return createImapConnection(settings, socketFactory, connectivityManager, oAuth2TokenProvider);
    }

    private ManageSieveConnection createImapConnection(SieveSettings settings, TrustedSocketFactory socketFactory,
            ConnectivityManager connectivityManager, OAuth2TokenProvider oAuth2TokenProvider) {
        return new ManageSieveConnection(settings, socketFactory, connectivityManager, oAuth2TokenProvider,
                SOCKET_CONNECT_TIMEOUT, SOCKET_READ_TIMEOUT);
    }

    @Test
    public void open_withMD5_authenticatesWithMD5() throws Exception {
        settings.setAuthType(AuthType.PLAIN);
        MockSieveServer server = new MockSieveServer();
        server.output("\"IMPlemENTATION\" \"Example1 ManageSieved v001\"");
        server.output("\"SASl\" \"DIGEST-MD5 GSSAPI\"");
        server.output("\"SIeVE\" \"fileinto vacation\"");
        server.output("\"StaRTTLS\"");
        server.output("\"NOTIFY\" \"xmpp mailto\"");
        server.output("\"MAXREdIRECTS\" \"5\"");
        server.output("\"VERSION\" \"1.0\"");
        server.output("OK");
        server.expect("AUTHENTICATE \"DIGEST-MD5\"");
        server.output("\"" +
                Base64.encode("realm=\"elwood.innosoft.example.com\",nonce=\"OA6MG9tEQGm2hh\",qop=\"auth\",algorithm=md5-sess,charset=utf-8")
                + "\"");
        server.expect("{276+}");
        server.expectAnyString();
        server.output("OK");
        ManageSieveConnection manageSieveConnection = startServerAndCreateSieveConnection(server);

        manageSieveConnection.open();

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }
}

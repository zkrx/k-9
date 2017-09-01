package com.fsck.k9.mail.store.imap;

import java.util.List;

import android.net.ConnectivityManager;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.K9LibRobolectricTestRunner;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.helpers.TestTrustedSocketFactory;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.imap.mockserver.MockImapServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static com.fsck.k9.mail.store.imap.ImapConnectionTestUtils.*;


@RunWith(K9LibRobolectricTestRunner.class)
public class ImapConnectionTest {
    private static final boolean DEBUGGING = false;

    private TrustedSocketFactory socketFactory;
    private ConnectivityManager connectivityManager;
    private OAuth2TokenProvider oAuth2TokenProvider;
    private SimpleImapSettings settings;

    @Before
    public void setUp() throws Exception {
        connectivityManager = mock(ConnectivityManager.class);
        oAuth2TokenProvider = createOAuth2TokenProvider();
        socketFactory = TestTrustedSocketFactory.newInstance();

        settings = new SimpleImapSettings();
        settings.setUsername(USERNAME);
        settings.setPassword(PASSWORD);

        if (DEBUGGING) {
            ShadowLog.stream = System.out;
            K9MailLib.setDebug(true);
            K9MailLib.setDebugSensitive(true);
        }
    }

    @Test
    public void isConnected_withoutPreviousOpen_shouldReturnFalse() throws Exception {
        ImapConnection imapConnection = createImapConnection(
                settings, socketFactory, connectivityManager, oAuth2TokenProvider);

        boolean result = imapConnection.isConnected();

        assertFalse(result);
    }

    @Test
    public void isConnected_afterOpen_shouldReturnTrue() throws Exception {
        MockImapServer server = new MockImapServer();
        ImapConnection imapConnection = simpleOpen(server, settings, socketFactory, connectivityManager, oAuth2TokenProvider);

        boolean result = imapConnection.isConnected();

        assertTrue(result);
        server.verifyConnectionStillOpen();

        server.shutdown();
    }

    @Test
    public void isConnected_afterOpenAndClose_shouldReturnFalse() throws Exception {
        MockImapServer server = new MockImapServer();
        ImapConnection imapConnection = simpleOpen(server, settings, socketFactory, connectivityManager, oAuth2TokenProvider);
        imapConnection.close();

        boolean result = imapConnection.isConnected();

        assertFalse(result);
        server.verifyConnectionClosed();

        server.shutdown();
    }

    @Test
    public void close_withoutOpen_shouldNotThrow() throws Exception {
        ImapConnection imapConnection = createImapConnection(
                settings, socketFactory, connectivityManager, oAuth2TokenProvider);

        imapConnection.close();
    }

    @Test
    public void close_afterOpen_shouldCloseConnection() throws Exception {
        MockImapServer server = new MockImapServer();
        ImapConnection imapConnection = simpleOpen(server, settings, socketFactory, connectivityManager, oAuth2TokenProvider);

        imapConnection.close();

        server.verifyConnectionClosed();

        server.shutdown();
    }

    @Test
    public void isIdleCapable_withoutIdleCapability() throws Exception {
        MockImapServer server = new MockImapServer();
        ImapConnection imapConnection = simpleOpen(server, settings, socketFactory, connectivityManager, oAuth2TokenProvider);

        boolean result = imapConnection.isIdleCapable();

        assertFalse(result);

        server.shutdown();
    }

    @Test
    public void isIdleCapable_withIdleCapability() throws Exception {
        MockImapServer server = new MockImapServer();
        ImapConnection imapConnection = simpleOpenWithCapabilities(server, settings, socketFactory, connectivityManager, oAuth2TokenProvider, "IDLE");

        boolean result = imapConnection.isIdleCapable();

        assertTrue(result);

        server.shutdown();
    }

    @Test
    public void sendContinuation() throws Exception {
        settings.setAuthType(AuthType.PLAIN);
        MockImapServer server = new MockImapServer();
        simpleOpenDialog(server, settings, "IDLE");
        server.expect("4 IDLE");
        server.output("+ idling");
        server.expect("DONE");
        ImapConnection imapConnection = startServerAndCreateImapConnection(server, settings, socketFactory, connectivityManager, oAuth2TokenProvider);
        imapConnection.open();
        imapConnection.sendCommand("IDLE", false);
        imapConnection.readResponse();

        imapConnection.sendContinuation("DONE");

        server.waitForInteractionToComplete();
        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void executeSingleCommand_withOkResponse_shouldReturnResult() throws Exception {
        MockImapServer server = new MockImapServer();
        simpleOpenDialog(server, settings, "");
        server.expect("4 CREATE Folder");
        server.output("4 OK Folder created");
        ImapConnection imapConnection = startServerAndCreateImapConnection(server, settings,
                socketFactory, connectivityManager, oAuth2TokenProvider);

        List<ImapResponse> result = imapConnection.executeSimpleCommand("CREATE Folder");

        assertEquals(result.size(), 1);
        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void executeSingleCommand_withNoResponse_shouldThrowNegativeImapResponseException() throws Exception {
        MockImapServer server = new MockImapServer();
        simpleOpenDialog(server, settings, "");
        server.expect("4 CREATE Folder");
        server.output("4 NO Folder exists");
        ImapConnection imapConnection = startServerAndCreateImapConnection(server, settings,
                socketFactory, connectivityManager, oAuth2TokenProvider);

        try {
            imapConnection.executeSimpleCommand("CREATE Folder");

            fail("Expected exception");
        } catch (NegativeImapResponseException e) {
            assertEquals("Folder exists", e.getLastResponse().getString(1));
        }
        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }


}

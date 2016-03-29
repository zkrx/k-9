package com.fsck.k9.mail.ssl;


import java.net.Socket;

import android.content.Context;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class DefaultTrustedSocketFactoryTest {
    private DefaultTrustedSocketFactory defaultTrustedSocketFactory;


    @Before
    public void setUp() throws Exception {
        Context appContext = ShadowApplication.getInstance().getApplicationContext();
        defaultTrustedSocketFactory = new DefaultTrustedSocketFactory(appContext);
    }

    @Test
    public void isSecure_withSocket_isFalse() {
        Socket socket = mock(Socket.class);

        boolean result = defaultTrustedSocketFactory.isSecure(socket);

        assertFalse(result);
    }

    @Test
    public void isSecure_withSSLSocketWithSSLv3_isFalse() {
        SSLSocket socket = mock(SSLSocket.class);
        SSLSession sslSession = mock(SSLSession.class);
        when(socket.getSession()).thenReturn(sslSession);
        when(sslSession.getProtocol()).thenReturn("SSLv3");

        boolean result = defaultTrustedSocketFactory.isSecure(socket);

        assertFalse(result);
    }

    @Test
    public void isSecure_withSSLSocketWithBadCipher_isFalse() {
        SSLSocket socket = mock(SSLSocket.class);
        SSLSession sslSession = mock(SSLSession.class);
        when(socket.getSession()).thenReturn(sslSession);
        when(sslSession.getProtocol()).thenReturn("TLSv1.2");
        when(sslSession.getCipherSuite()).thenReturn("TLS_ECDHE_RSA_WITH_RC4_128_SHA");

        boolean result = defaultTrustedSocketFactory.isSecure(socket);

        assertFalse(result);
    }

    @Test
    public void isSecure_withSSLSocketWithTLSandGoodCipher_isTrue() {
        SSLSocket socket = mock(SSLSocket.class);
        SSLSession sslSession = mock(SSLSession.class);
        when(socket.getSession()).thenReturn(sslSession);
        when(sslSession.getProtocol()).thenReturn("TLSv1.2");
        when(sslSession.getProtocol()).thenReturn("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");

        boolean result = defaultTrustedSocketFactory.isSecure(socket);

        assertTrue(result);
    }
}

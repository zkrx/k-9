package com.fsck.k9.mail.store.webdav;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class WebDavSocketFactoryTest {
    private static final String DEFAULT_HOST = "defaultHost.com";
    private static final int DEFAULT_PORT = 1000;
    private static final String OTHER_HOST = "otherHost.com";
    private static final int OTHER_PORT = 10001;
    private static final int LOCAL_PORT = 10001;
    private static final int CONNECTION_TIMEOUT = 1030;
    private static final String CERTIFICATE_ALIAS = "certificateAlias";


    @Mock
    private TrustedSocketFactory trustedSocketFactory;
    @Mock
    private SSLSocket trustedSocket;
    @Mock
    private SSLSocket customAddressTrustedSocket;
    @Mock
    private SSLSocket unknownSocket;
    @Mock
    private SSLSession sslSession;
    @Mock
    private HttpParams params;
    @Mock
    private InetAddress localAddress;

    private WebDavSocketFactory webDavSocketFactory;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        webDavSocketFactory = new WebDavSocketFactory(trustedSocketFactory,
                DEFAULT_HOST, DEFAULT_PORT, CERTIFICATE_ALIAS);
        when(trustedSocketFactory.createSocket(null, DEFAULT_HOST, DEFAULT_PORT, CERTIFICATE_ALIAS))
                .thenReturn(trustedSocket);

        when(trustedSocketFactory.createSocket(null, OTHER_HOST, OTHER_PORT, CERTIFICATE_ALIAS))
                .thenReturn(customAddressTrustedSocket);
        when(params.getIntParameter(eq(CoreConnectionPNames.CONNECTION_TIMEOUT), anyInt()))
                .thenReturn(CONNECTION_TIMEOUT);
        when(customAddressTrustedSocket.getSession()).thenReturn(sslSession);
    }

    @Test
    public void createSocket_usesTrustedSocketFactoryToCreateSocket() throws Exception {
        Socket socket = webDavSocketFactory.createSocket();

        assertSame(trustedSocket, socket);
    }

    @Test
    public void createSocket_withCustomHostPort_usesTrustedSocketFactoryToCreateSocket() throws Exception {
        Socket customAddressTrustedSocket = mock(Socket.class);
        String host = OTHER_HOST;
        int port = OTHER_PORT;
        when(trustedSocketFactory.createSocket(null, host, port, CERTIFICATE_ALIAS))
                .thenReturn(customAddressTrustedSocket);

        Socket socket = webDavSocketFactory.createSocket(null, host, port, true);

        assertSame(customAddressTrustedSocket, socket);
    }

    @Test
    public void connectSocket_withNoSocket_providesWrappedCustomSocket() throws Exception {
        Socket socket = webDavSocketFactory.connectSocket(
                null, OTHER_HOST, OTHER_PORT, localAddress, LOCAL_PORT, params);

        assertSame(customAddressTrustedSocket, socket);
    }

    @Test
    public void connectSocket_withNoSocket_passesTrustedSocketToApacheSocketFactory() throws Exception {
        webDavSocketFactory.connectSocket(null, OTHER_HOST, OTHER_PORT, localAddress, LOCAL_PORT, params);

        verify(customAddressTrustedSocket).connect(
                eq(new InetSocketAddress(OTHER_HOST, OTHER_PORT)), eq(CONNECTION_TIMEOUT));
    }

    @Test
    public void connectSocket_bindsProvidedSocketToLocalAddressAndUsesHttpParams() throws Exception {
        webDavSocketFactory.connectSocket(unknownSocket, OTHER_HOST, OTHER_PORT, localAddress, LOCAL_PORT, params);

        verify(unknownSocket).bind(eq(new InetSocketAddress(localAddress, LOCAL_PORT)));
    }

    @Test
    public void isSecure_whenTrustedSaysNo_isFalse() throws Exception {
        when(trustedSocketFactory.isSecure(unknownSocket)).thenReturn(false);

        boolean result = webDavSocketFactory.isSecure(unknownSocket);

        assertFalse(result);
    }

    @Test
    public void isSecure_whenTrustedSaySecure_isTrue() throws Exception {
        when(trustedSocketFactory.isSecure(unknownSocket)).thenReturn(true);

        boolean result = webDavSocketFactory.isSecure(unknownSocket);

        assertTrue(result);
    }
}

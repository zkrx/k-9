package com.fsck.k9.mail.store.webdav;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import javax.net.ssl.SSLSocket;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;


class WebDavSocketFactory implements LayeredSocketFactory {
    private final String certificateAlias;
    private final String defaultHost;
    private final int defaultPort;
    private final TrustedSocketFactory trustedSocketFactory;


    public WebDavSocketFactory(TrustedSocketFactory trustedSocketFactory, String defaultHost, int defaultPort,
            String certificateAlias) {
        this.trustedSocketFactory = trustedSocketFactory;
        this.certificateAlias = certificateAlias;
        this.defaultHost = defaultHost;
        this.defaultPort = defaultPort;
    }

    @Override
    public Socket createSocket() throws IOException {
        return createSocket(null, defaultHost, defaultPort);
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        if (!autoClose) {
            throw new IOException("We don't support non-auto close sockets");
        }

        return createSocket(socket, host, port);
    }

    private Socket createSocket(Socket socket, String host, int port) throws IOException {
        try {
            return trustedSocketFactory.createSocket(socket, host, port, certificateAlias);
        } catch (Exception e) {
            throw new IOException("Exception creating trusted socket", e);
        }
    }

    @Override
    public Socket connectSocket(Socket socket, String host, int port, InetAddress localAddress, int localPort,
            HttpParams params) throws IOException {
        if (socket == null) {
            socket = createSocket(null, host, port);
        }

        if (!(socket instanceof SSLSocket)) {
            throw new IOException("Not an SSLSocket instance");
        }

        SSLSocket sslSocket = (SSLSocket) socket;

        if (localAddress != null || localPort > 0) {
            if (localPort < 0) {
                localPort = 0;
            }

            InetSocketAddress localSocketAddress = new InetSocketAddress(localAddress, localPort);
            sslSocket.bind(localSocketAddress);
        }

        int connectionTimeout = HttpConnectionParams.getConnectionTimeout(params);
        int soTimeout = HttpConnectionParams.getSoTimeout(params);

        InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
        sslSocket.connect(remoteAddress, connectionTimeout);

        sslSocket.setSoTimeout(soTimeout);

        return sslSocket;
    }

    @Override
    public boolean isSecure(Socket sock) throws IllegalArgumentException {
        return trustedSocketFactory.isSecure(sock);
    }
}

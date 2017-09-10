package com.fsck.k9.mail.remoteFilter.sieve;


import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;


class SimpleSieveSettings implements SieveSettings {

    private String username;
    private String password;
    private AuthType authType;
    private String host;
    private int port;
    private ConnectionSecurity connectionSecurity;
    private String clientCertificateAlias;

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

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
        return authType;
    }

    public void setConnectionSecurity(ConnectionSecurity connectionSecurity) {
        this.connectionSecurity = connectionSecurity;
    }
}

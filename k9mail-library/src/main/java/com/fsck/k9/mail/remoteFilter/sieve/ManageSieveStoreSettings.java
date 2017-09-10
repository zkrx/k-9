package com.fsck.k9.mail.remoteFilter.sieve;


import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;


class ManageSieveStoreSettings extends ServerSettings {
    String host;
    int port;
    ConnectionSecurity connectionSecurity;
    String clientCertificateAlias;
    String username;
    String password;

    public ManageSieveStoreSettings(String host, int port, ConnectionSecurity connectionSecurity,
            AuthType authenticationType, String username, String password,
            String clientCertificateAlias) {
        super(Type.SIEVE, host, port, connectionSecurity, authenticationType, username, password, clientCertificateAlias);
    }
}

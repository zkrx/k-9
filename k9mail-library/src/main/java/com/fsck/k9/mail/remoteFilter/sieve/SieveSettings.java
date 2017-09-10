package com.fsck.k9.mail.remoteFilter.sieve;


import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;


interface SieveSettings {

    ConnectionSecurity getConnectionSecurity();

    String getHost();

    int getPort();

    String getClientCertificateAlias();

    String getUsername();

    String getPassword();

    AuthType getAuthType();
}
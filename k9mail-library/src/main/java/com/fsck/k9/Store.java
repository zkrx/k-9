package com.fsck.k9;


import com.fsck.k9.mail.MessagingException;


public interface Store {
    void checkSettings() throws MessagingException;
}

package com.fsck.k9.mail.helper;


public class ArrayHelper {
    public static <T> boolean contains(T[] array, T value) {
        for (T element : array) {
            if (element.equals(value)) {
                return true;
            }
        }

        return false;
    }
}

package com.example.agrialert.utils;

import android.text.TextUtils;
import android.util.Patterns;

public class ValidationUtil {

    public static boolean isValidEmail(String email) {
        return (!TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches());
    }

    public static boolean isValidPassword(String password) {
        return (!TextUtils.isEmpty(password) && password.length() >= 6);
    }

    public static boolean isEmpty(String str) {
        return TextUtils.isEmpty(str);
    }
}

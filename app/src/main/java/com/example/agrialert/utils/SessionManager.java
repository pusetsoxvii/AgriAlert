package com.example.agrialert.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "AgriAlertSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ROLE = "user_role";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void saveUserSession(int id, String name, String email, String role) {
        editor.putInt(KEY_USER_ID, id);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }

    public int getUserId() { return pref.getInt(KEY_USER_ID, -1); }
    public String getUserName() { return pref.getString(KEY_USER_NAME, "User"); }
    public String getUserEmail() { return pref.getString(KEY_USER_EMAIL, ""); }
    public String getUserRole() { return pref.getString(KEY_USER_ROLE, ""); }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}

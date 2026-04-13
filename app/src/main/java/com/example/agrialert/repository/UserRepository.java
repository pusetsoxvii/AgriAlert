package com.example.agrialert.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.agrialert.database.DatabaseHelper;
import com.example.agrialert.model.User;
import com.example.agrialert.utils.HashUtil;

public class UserRepository {
    private DatabaseHelper dbHelper;

    public UserRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public User login(String email, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String hashedPassword = HashUtil.hashPassword(password);
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, null, 
                DatabaseHelper.KEY_EMAIL + "=? AND " + DatabaseHelper.KEY_PASSWORD + "=?",
                new String[]{email, hashedPassword}, null, null, null);
                
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_NAME));
            String role = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ROLE));
            cursor.close();
            return new User(id, name, email, hashedPassword, role);
        }
        
        if (cursor != null) {
            cursor.close();
        }
        return null;
    }

    public boolean register(String name, String email, String password, String role) {
        if (checkEmailExists(email)) {
            return false;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String hashedPassword = HashUtil.hashPassword(password);

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.KEY_NAME, name);
        values.put(DatabaseHelper.KEY_EMAIL, email);
        values.put(DatabaseHelper.KEY_PASSWORD, hashedPassword);
        values.put(DatabaseHelper.KEY_ROLE, role);

        long result = db.insert(DatabaseHelper.TABLE_USERS, null, values);
        return result != -1;
    }

    private boolean checkEmailExists(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, 
                new String[]{DatabaseHelper.KEY_ID}, 
                DatabaseHelper.KEY_EMAIL + "=?",
                new String[]{email}, null, null, null);
                
        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) cursor.close();
        return exists;
    }
}

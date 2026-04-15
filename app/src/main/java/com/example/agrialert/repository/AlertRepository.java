package com.example.agrialert.repository;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.agrialert.database.DatabaseHelper;
import com.example.agrialert.model.Alert;

import java.util.ArrayList;
import java.util.List;

public class AlertRepository {
    private DatabaseHelper dbHelper;

    public AlertRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public List<Alert> getAllAlerts() {
        List<Alert> alerts = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_ALERTS + " ORDER BY " + DatabaseHelper.KEY_DATE_CREATED + " DESC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Alert alert = new Alert(
                        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_MESSAGE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_DATE_CREATED))
                );
                alerts.add(alert);
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
        return alerts;
    }
}
package com.example.agrialert.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.agrialert.database.DatabaseHelper;

public class ReportRepository {
    private DatabaseHelper dbHelper;

    public ReportRepository(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public int getTotalReportsCount(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_REPORTS + 
                " WHERE " + DatabaseHelper.KEY_USER_ID + "=?", new String[]{String.valueOf(userId)});
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public int getPendingReportsCount(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_REPORTS + 
                " WHERE " + DatabaseHelper.KEY_USER_ID + "=? AND " + DatabaseHelper.KEY_STATUS + "='Pending'", 
                new String[]{String.valueOf(userId)});
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public boolean insertReport(int userId, String animalType, String symptoms, int numberAffected, String dateObserved, double latitude, double longitude, String imagePath) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.KEY_USER_ID, userId);
        values.put(DatabaseHelper.KEY_ANIMAL_TYPE, animalType);
        values.put(DatabaseHelper.KEY_SYMPTOMS, symptoms);
        values.put(DatabaseHelper.KEY_NUMBER_AFFECTED, numberAffected);
        values.put(DatabaseHelper.KEY_DATE_OBSERVED, dateObserved);
        values.put(DatabaseHelper.KEY_STATUS, "Pending");
        values.put(DatabaseHelper.KEY_LATITUDE, latitude);
        values.put(DatabaseHelper.KEY_LONGITUDE, longitude);
        values.put(DatabaseHelper.KEY_IMAGE_PATH, imagePath);

        long result = db.insert(DatabaseHelper.TABLE_REPORTS, null, values);
        db.close();
        return result != -1;
    }

    public java.util.List<com.example.agrialert.model.Report> getReportsForUser(int userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_REPORTS +
                " WHERE " + DatabaseHelper.KEY_USER_ID + "=?", new String[]{String.valueOf(userId)});

        java.util.List<com.example.agrialert.model.Report> reports = new java.util.ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                com.example.agrialert.model.Report report = new com.example.agrialert.model.Report(
                        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_USER_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ANIMAL_TYPE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_SYMPTOMS)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_NUMBER_AFFECTED)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_DATE_OBSERVED)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_STATUS)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_LATITUDE)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_LONGITUDE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_IMAGE_PATH))
                );
                reports.add(report);
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
        return reports;
    }
}

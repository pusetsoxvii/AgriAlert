package com.example.agrialert.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "AgriAlert.db";
    // Bump version when schema changes (e.g., adding is_deleted column)
    private static final int DATABASE_VERSION = 2;

    // Table Names
    public static final String TABLE_USERS = "users";
    public static final String TABLE_REPORTS = "reports";
    public static final String TABLE_RESPONSES = "responses";
    public static final String TABLE_ALERTS = "alerts";

    // Common column names
    public static final String KEY_ID = "id";

    // USERS Table - column names
    public static final String KEY_NAME = "name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_ROLE = "role";

    // REPORTS Table - column names
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_ANIMAL_TYPE = "animal_type";
    public static final String KEY_SYMPTOMS = "symptoms";
    public static final String KEY_NUMBER_AFFECTED = "number_affected";
    public static final String KEY_DATE_OBSERVED = "date_observed";
    public static final String KEY_STATUS = "status";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";
    public static final String KEY_IMAGE_PATH = "image_path";
    public static final String KEY_IS_DELETED = "is_deleted";

    // RESPONSES Table - column names
    public static final String KEY_REPORT_ID = "report_id";
    public static final String KEY_VET_ID = "vet_id";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_RESPONSE_DATE = "response_date";

    // ALERTS Table - column names
    public static final String KEY_TITLE = "title";
    // Reuse KEY_MESSAGE for alerts message
    public static final String KEY_DATE_CREATED = "date_created";

    // Table Create Statements
    // 1. Users table create statement
    private static final String CREATE_TABLE_USERS = "CREATE TABLE "
            + TABLE_USERS + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_NAME + " TEXT,"
            + KEY_EMAIL + " TEXT UNIQUE,"
            + KEY_PASSWORD + " TEXT,"
            + KEY_ROLE + " TEXT" + ")";

    // 2. Reports table create statement
    private static final String CREATE_TABLE_REPORTS = "CREATE TABLE "
            + TABLE_REPORTS + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_USER_ID + " INTEGER,"
            + KEY_ANIMAL_TYPE + " TEXT,"
            + KEY_SYMPTOMS + " TEXT,"
            + KEY_NUMBER_AFFECTED + " INTEGER,"
            + KEY_DATE_OBSERVED + " TEXT,"
            + KEY_STATUS + " TEXT DEFAULT 'Pending',"
            + KEY_LATITUDE + " REAL,"
            + KEY_LONGITUDE + " REAL,"
            + KEY_IMAGE_PATH + " TEXT,"
            + KEY_IS_DELETED + " INTEGER DEFAULT 0,"
            + "FOREIGN KEY(" + KEY_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_ID + ")"
            + ")";

    // 3. Responses table create statement
    private static final String CREATE_TABLE_RESPONSES = "CREATE TABLE "
            + TABLE_RESPONSES + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_REPORT_ID + " INTEGER,"
            + KEY_VET_ID + " INTEGER,"
            + KEY_MESSAGE + " TEXT,"
            + KEY_RESPONSE_DATE + " TEXT,"
            + "FOREIGN KEY(" + KEY_REPORT_ID + ") REFERENCES " + TABLE_REPORTS + "(" + KEY_ID + "),"
            + "FOREIGN KEY(" + KEY_VET_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_ID + ")"
            + ")";

    // 4. Alerts table create statement
    private static final String CREATE_TABLE_ALERTS = "CREATE TABLE "
            + TABLE_ALERTS + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_TITLE + " TEXT,"
            + KEY_MESSAGE + " TEXT,"
            + KEY_DATE_CREATED + " TEXT"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // creating required tables
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_REPORTS);
        db.execSQL(CREATE_TABLE_RESPONSES);
        db.execSQL(CREATE_TABLE_ALERTS);

        // Insert predefined admin user
        insertPredefinedAdmin(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle incremental migrations to preserve existing data
        if (oldVersion < 2) {
            // Add soft-delete flag for reports if it doesn't exist yet
            try {
                db.execSQL("ALTER TABLE " + TABLE_REPORTS + " ADD COLUMN "
                        + KEY_IS_DELETED + " INTEGER DEFAULT 0");
            } catch (Exception e) {
                // Column may already exist; log and continue
                Log.w("DatabaseHelper", "onUpgrade: failed to add is_deleted column", e);
            }
        }
        // Future schema changes: handle with additional if (oldVersion < X) blocks
    }

    private void insertPredefinedAdmin(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, "Admin");
        values.put(KEY_EMAIL, "admin@agrialert.com");
        values.put(KEY_PASSWORD, com.example.agrialert.utils.HashUtil.hashPassword("admin123")); // Hashed password
        values.put(KEY_ROLE, "ADMIN");

        try {
            long result = db.insert(TABLE_USERS, null, values);
            if (result != -1) {
                Log.d("DatabaseHelper", "Predefined Admin inserted successfully.");
            } else {
                Log.e("DatabaseHelper", "Failed to insert predefined Admin.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

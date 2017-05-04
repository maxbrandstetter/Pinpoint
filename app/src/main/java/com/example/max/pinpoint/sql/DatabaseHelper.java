package com.example.max.pinpoint.sql;

/**
 * Created by Max on 5/3/2017.
 *
 * A helper class to manage databse creation and version management,
 * as well as manipulate data within the database.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.max.pinpoint.User;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    // Database version
    private static final int DATABASE_VERSION = 1;

    // Database name
    private static final String DATABASE_NAME = "UserManager.db";

    // User table name
    private static final String TABLE_USER = "user";

    // User table column names
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_USER_NAME = "user_name";
    private static final String COLUMN_USER_EMAIL = "user_email";
    private static final String COLUMN_USER_PASSWORD = "user_password";

    // SQL query to create table
    private String CREATE_USER_TABLE = "CREATE TABLE " + TABLE_USER + "(" + COLUMN_USER_ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT," + COLUMN_USER_NAME + " TEXT," + COLUMN_USER_EMAIL
            + " TEXT," + COLUMN_USER_PASSWORD + " TEXT" + ")";

    // SQL query to drop table
    private String DROP_USER_TABLE = "DROP TABLE IF EXISTS " + TABLE_USER;

    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop table if it exists
        db.execSQL(DROP_USER_TABLE);

        // Recreate
        onCreate(db);
    }

    // Create user record
    public void addUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_NAME, user.getName());
        values.put(COLUMN_USER_EMAIL, user.getEmail());
        values.put(COLUMN_USER_PASSWORD, user.getPassword());

        // Insert row
        db.insert(TABLE_USER, null, values);
        db.close();
    }

    // Fetch all users and return list of records; might use later
    public List<User> getAllUser() {
        // columns to fetch
        String[] columns = {
                COLUMN_USER_ID,
                COLUMN_USER_EMAIL,
                COLUMN_USER_NAME,
                COLUMN_USER_PASSWORD
        };
        // Sorting order for users
        String sortOrder = COLUMN_USER_NAME + " ASC";

        List<User> userList = new ArrayList<User>();

        SQLiteDatabase db = this.getReadableDatabase();

        // Query the user table to fetch records
        // Equivalent to SELECT query
        Cursor cursor = db.query(TABLE_USER, // Table to query
                columns, // columns to return
                null, // columns for the WHERE clause
                null, // values of the WHERE caluse
                null, // group the rows
                null, // filter by row groups
                sortOrder);

        // Traverse through the rows and add to the list
        if (cursor.moveToFirst()) {
            do {
                User user = new User();
                user.setId(Integer.parseInt(cursor.getString(cursor.getColumnIndex(COLUMN_USER_ID))));
                user.setName(cursor.getString(cursor.getColumnIndex(COLUMN_USER_NAME)));
                user.setEmail((cursor.getString(cursor.getColumnIndex(COLUMN_USER_EMAIL))));
                user.setPassword((cursor.getString(cursor.getColumnIndex(COLUMN_USER_PASSWORD))));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        // return the list of users
        return userList;
    }

    // Update user record
    public void updateUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_NAME, user.getName());
        values.put(COLUMN_USER_EMAIL, user.getEmail());
        values.put(COLUMN_USER_PASSWORD, user.getPassword());

        // Update the row
        db.update(TABLE_USER, values, COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(user.getId())});
        db.close();
    }

    // Delete user record
    public void deleteUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete user record by id
        db.delete(TABLE_USER, COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(user.getId())});
        db.close();
    }

    // Check if a user exists or not
    public boolean checkUser(String email) {

        // Columns to fetch
        String[] columns = {
                COLUMN_USER_ID
        };
        SQLiteDatabase db = this.getReadableDatabase();

        // Selection criteria
        String selection = COLUMN_USER_EMAIL + " = ?";

        // Selection argument
        String[] selectionArgs = {email};

        // Query user table with condition (SQL select equivalent)
        Cursor cursor = db.query(TABLE_USER, //Table to query
                columns,
                selection,
                selectionArgs,
                null,
                null,
                null);
        int cursorCount = cursor.getCount();
        cursor.close();
        db.close();

        if (cursorCount > 0) {
            return true;
        }

        return false;
    }

    // Check if user and password exist or not
    public boolean checkUser(String email, String password) {

        // columns to fetch
        String[] columns = {
                COLUMN_USER_ID
        };
        SQLiteDatabase db = this.getReadableDatabase();

        // selection criteria
        String selection = COLUMN_USER_EMAIL + " = ?" + " AND " + COLUMN_USER_PASSWORD + " = ?";

        // selection arguments
        String[] selectionArgs = {email, password};

        // query user table with conditions (SQL select equivalent)
        Cursor cursor = db.query(TABLE_USER,
                columns,
                selection,
                selectionArgs,
                null,
                null,
                null);

        int cursorCount = cursor.getCount();

        cursor.close();
        db.close();

        if (cursorCount > 0) {
            return true;
        }

        return false;
    }
}

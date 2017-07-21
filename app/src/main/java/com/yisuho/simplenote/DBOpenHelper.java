package com.yisuho.simplenote;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by xon23 on 2016-07-04.
 */
public class DBOpenHelper extends SQLiteOpenHelper {

    //Constants for db name and version
    private static final String DATABASE_NAME = "notes.db";
    private static final int DATABASE_VERSION = 5;

    //Constants for identifying table and columns
    public static final String TABLE_NOTES = "notes";
    public static final String NOTE_ID = "_id";
    public static final String NOTE_TEXT = "noteText";
    public static final String NOTE_IMPORTANT = "noteImportant";
    public static final String NOTE_CREATED = "noteCreated";

    public static final String[] TABLE_NOTES_ALL_COLUMNS =
            {NOTE_ID, NOTE_TEXT, NOTE_IMPORTANT, NOTE_CREATED};

    public static final String TABLE_TAGS = "tags";
    public static final String TAGS_TEXT = "tagText";
    public static final String TAGS_NOTE_ID = "noteId";

    public static final String TAGS_TAG_COUNT = "tagCount";

    public static final String[] TABLE_TAGS_ALL_COLUMNS =
            {TAGS_TEXT, TAGS_NOTE_ID};

    //SQL to create table
    private static final String TABLE_NOTES_CREATE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NOTES + " (" +
                    NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    NOTE_TEXT + " TEXT, " +
                    NOTE_IMPORTANT + " INTEGER default 0, " +
                    NOTE_CREATED + " TEXT default CURRENT_TIMESTAMP" +
                    ")";

    private static final String TABLE_TAGS_CREATE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_TAGS + " (" +
                    TAGS_TEXT + " TEXT, " +
                    TAGS_NOTE_ID + " INTEGER default 0" +
                    ")";

    //SQL to drop table
    private static final String TABLE_NOTES_DROP = "DROP TABLE IF EXISTS " +
            TABLE_NOTES;

    private static final String TABLE_TAGS_DROP = "DROP TABLE IF EXISTS " +
            TABLE_TAGS;

    public DBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d("DBOpenHelper", "onCreate");
        sqLiteDatabase.execSQL(TABLE_NOTES_CREATE);
        sqLiteDatabase.execSQL(TABLE_TAGS_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.d("DBOpenHelper", "onUpgrade: " + oldVersion);
        if(oldVersion < 5){
            sqLiteDatabase.execSQL(TABLE_TAGS_CREATE);
        }
    }
}

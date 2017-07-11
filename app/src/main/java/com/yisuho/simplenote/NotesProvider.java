package com.yisuho.simplenote;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;

/**
 * Created by xon23 on 2016-07-04.
 */
public class NotesProvider extends ContentProvider {

    private static final String AUTHORITY = "com.yisuho.simplenote.notesprovider";
    private static final String BASE_PATH_NOTES = "notes";
    private static final String BASE_PATH_TAGS = "tags";
    public static final Uri CONTENT_URI_NOTES = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_NOTES);
    public static final Uri CONTENT_URI_TAGS = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH_TAGS);

    // Constant to identify the requested operation
    private static final int NOTES = 1;
    private static final int NOTES_ID = 2;
    private static final int TAGS = 3;
    private static final int TAGS_NOTE_ID = 4;


    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    public static final String CONTENT_ITEM_TYPE ="Note";

    static {
        uriMatcher.addURI(AUTHORITY, BASE_PATH_NOTES, NOTES);
        uriMatcher.addURI(AUTHORITY, BASE_PATH_NOTES + "/#", NOTES_ID);
        uriMatcher.addURI(AUTHORITY, BASE_PATH_TAGS, TAGS);
        uriMatcher.addURI(AUTHORITY, BASE_PATH_TAGS + "/#", TAGS_NOTE_ID);
    }

    private SQLiteDatabase database;

    @Override
    public boolean onCreate() {

        //Init DB
        DBOpenHelper helper = new DBOpenHelper(getContext());
        database = helper.getWritableDatabase();
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings1, String s1) {

        switch (uriMatcher.match(uri)){
            case NOTES:
                return database.query(DBOpenHelper.TABLE_NOTES, DBOpenHelper.TABLE_NOTES_ALL_COLUMNS,
                        s, null, null, null, DBOpenHelper.NOTE_IMPORTANT + " DESC, " +
                                DBOpenHelper.NOTE_CREATED + " DESC");
            case NOTES_ID:
                s = DBOpenHelper.NOTE_ID + "=" + uri.getLastPathSegment();
                return database.query(DBOpenHelper.TABLE_NOTES, DBOpenHelper.TABLE_NOTES_ALL_COLUMNS,
                        s, null, null, null, DBOpenHelper.NOTE_IMPORTANT + " DESC, " +
                                DBOpenHelper.NOTE_CREATED + " DESC");
            case TAGS:
                String[] p = {"COUNT(" + DBOpenHelper.TAGS_TEXT +") as " + DBOpenHelper.TAGS_TAG_COUNT, DBOpenHelper.TAGS_TEXT};
                return database.query(DBOpenHelper.TABLE_TAGS, p,
                        null, null, DBOpenHelper.TAGS_TEXT, null, DBOpenHelper.TAGS_TEXT + " ASC");
            case TAGS_NOTE_ID:
                s = DBOpenHelper.TAGS_NOTE_ID + "=" + uri.getLastPathSegment();
                return database.query(DBOpenHelper.TABLE_TAGS, DBOpenHelper.TABLE_TAGS_ALL_COLUMNS,
                        s, null, null, null, DBOpenHelper.TAGS_TEXT + " ASC");
        }

        return null;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        if(uriMatcher.match(uri) == NOTES){
            long id = database.insert(DBOpenHelper.TABLE_NOTES,
                    null, contentValues);
            return Uri.parse(BASE_PATH_NOTES + "/" + id);
        } else if(uriMatcher.match(uri) == TAGS){
            long id = database.insert(DBOpenHelper.TABLE_TAGS,
                    null, contentValues);
            return Uri.parse(BASE_PATH_TAGS + "/" + id);
        }

        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        if(uriMatcher.match(uri) == NOTES){
            return database.delete(DBOpenHelper.TABLE_NOTES, s, strings);
        } else if(uriMatcher.match(uri) == TAGS){
            return database.delete(DBOpenHelper.TABLE_TAGS, s, strings);
        }

        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        if(uriMatcher.match(uri) == NOTES) {
            return database.update(DBOpenHelper.TABLE_NOTES, contentValues, s, strings);
        }  else if(uriMatcher.match(uri) == TAGS){
            return database.update(DBOpenHelper.TAGS_TEXT, contentValues, s, strings);
        }

        return 0;
    }
}

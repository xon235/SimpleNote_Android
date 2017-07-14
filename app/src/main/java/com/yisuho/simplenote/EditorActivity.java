package com.yisuho.simplenote;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditorActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_DELETED = "NOTE DELETED";

    private String mAction;
    private EditText mEditText;
    private CheckBox mCheckBox;
    private String mNoteFilter;
    private String mOldText;
    private boolean mOldIsChecked;

    private Uri mUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mEditText = (EditText) findViewById(R.id.editText);

        mCheckBox = (CheckBox) findViewById(R.id.checkBox);

        Intent intent = getIntent();
        mUri = intent.getParcelableExtra(NotesProvider.CONTENT_ITEM_TYPE);

        if(mUri == null){
            mAction = Intent.ACTION_INSERT;
            setTitle(getString(R.string.new_note));
        } else {
            mAction = Intent.ACTION_EDIT;
            setTitle(getString(R.string.edit_note));
            mNoteFilter = DBOpenHelper.NOTE_ID + "=" + mUri.getLastPathSegment();

            Cursor cursor = getContentResolver().query(mUri,
                    DBOpenHelper.TABLE_NOTES_ALL_COLUMNS, mNoteFilter, null, null);
            try{
                cursor.moveToFirst();
                mOldText = cursor.getString(cursor.getColumnIndex(DBOpenHelper.NOTE_TEXT));
                mEditText.setText(mOldText);

                mOldIsChecked = (cursor.getInt(cursor.getColumnIndex(DBOpenHelper.NOTE_IMPORTANT)) == 1);
                mCheckBox.setChecked(mOldIsChecked);
            } catch (CursorIndexOutOfBoundsException e) {
                e.printStackTrace();
                mAction = Intent.ACTION_INSERT;
                setTitle(getString(R.string.new_note));
            } finally {
                cursor.close();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(mAction.equals(Intent.ACTION_EDIT)){
            getMenuInflater().inflate(R.menu.menu_editor, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case android.R.id.home:
                finishEditing();
                break;
            case R.id.action_delete:
                deleteNote();
                break;
        }

        return true;
    }

    private void finishEditing(){
        String newText = mEditText.getText().toString().trim();
        boolean isChecked = mCheckBox.isChecked();

        switch (mAction) {
            case Intent.ACTION_INSERT:
                if(newText.length() == 0){
                    cancelNote();
                } else {
                    Uri uri = insertNote(newText, isChecked);
                    updateHashtags(newText, Integer.parseInt(uri.getLastPathSegment()));
                    setResult(RESULT_OK);
                    finish();
                }
                break;
            case Intent.ACTION_EDIT:
                if(newText.length() == 0){
                    deleteNote();
                } else if(mOldText.equals(newText) && isChecked == mOldIsChecked){
                    cancelNote();
                } else {
                    updateNote(newText, isChecked);
                    updateHashtags(newText, Integer.parseInt(mUri.getLastPathSegment()));
                    setResult(RESULT_OK);
                    finish();
                }
                break;
            default:
                cancelNote();
        }
    }

    private void deleteNote() {
        DialogInterface.OnClickListener dialogClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int button) {
                        if (button == DialogInterface.BUTTON_POSITIVE) {
                            //Insert Data management code here
                            getContentResolver().delete(NotesProvider.CONTENT_URI_NOTES, mNoteFilter, null);
                            deleteAllHashTags(Integer.parseInt(mUri.getLastPathSegment()));
                            Toast.makeText(EditorActivity.this, R.string.note_deleted,
                                    Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(EditorActivity.this, SimpleNoteAppWidgetProvider.class);
                            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                            intent.putExtra(NotesProvider.CONTENT_ITEM_TYPE, mUri);
                            intent.putExtra(EXTRA_NOTE_DELETED, true);
                            int[] ids = AppWidgetManager.getInstance(EditorActivity.this)
                                    .getAppWidgetIds(new ComponentName(getPackageName(), "com.yisuho.simplenote.SimpleNoteAppWidgetProvider"));
                            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
                            sendBroadcast(intent);

                            setResult(RESULT_OK);
                            finish();
                        }
                    }
                };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.are_you_sure))
                .setPositiveButton(getString(android.R.string.yes), dialogClickListener)
                .setNegativeButton(getString(android.R.string.no), dialogClickListener)
                .show();
    }

    private void updateHashtags(String newText, int noteId) {
        //First delet all hashtags for this note
        deleteAllHashTags(noteId);
        //Add all hashtags from this note
        Pattern p = Pattern.compile(getString(R.string.hashtags_pattern));
        Matcher m = p.matcher(newText);
        while (m.find()) {
            String h = m.group(1);

            ContentValues values = new ContentValues();
            values.put(DBOpenHelper.TAGS_NOTE_ID, noteId);
            values.put(DBOpenHelper.TAGS_TEXT, h);

            getContentResolver().insert(NotesProvider.CONTENT_URI_TAGS, values);
        }
    }

    private void deleteAllHashTags(int noteIde){
        String s = DBOpenHelper.TAGS_NOTE_ID + "=" + noteIde;
        getContentResolver().delete(NotesProvider.CONTENT_URI_TAGS, s, null);
    }

    private void cancelNote() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void updateNote(String noteText, boolean isChecked) {
        int important = isChecked ? 1 : 0;

        ContentValues values = new ContentValues();
        values.put(DBOpenHelper.NOTE_TEXT, noteText);
        values.put(DBOpenHelper.NOTE_IMPORTANT, important);

        getContentResolver().update(NotesProvider.CONTENT_URI_NOTES, values, mNoteFilter, null);
        Toast.makeText(this, R.string.note_updated, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, SimpleNoteAppWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(NotesProvider.CONTENT_ITEM_TYPE, mUri);
        int[] ids = AppWidgetManager.getInstance(this)
                .getAppWidgetIds(new ComponentName(getPackageName(), "com.yisuho.simplenote.SimpleNoteAppWidgetProvider"));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
        sendBroadcast(intent);
    }

    private Uri insertNote(String noteText, boolean isChecked) {
        int important = isChecked ? 1 : 0;

        ContentValues values = new ContentValues();
        values.put(DBOpenHelper.NOTE_TEXT, noteText);
        values.put(DBOpenHelper.NOTE_IMPORTANT, important);

        Uri uri = getContentResolver().insert(NotesProvider.CONTENT_URI_NOTES, values);
        Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show();
        return uri;
    }

    @Override
    public void onBackPressed() {
        finishEditing();
    }
}

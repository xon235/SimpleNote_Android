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

public class EditorActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_DELETED = "NOTE DELETED";

    private String action;
    private EditText editor;
    private CheckBox checkBox;
    private String noteFilter;
    private String oldText;
    private boolean oldIsChecked;

    private Uri mUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        editor = (EditText) findViewById(R.id.editText);
        checkBox = (CheckBox) findViewById(R.id.checkBox);

        Intent intent = getIntent();

        mUri = intent.getParcelableExtra(NotesProvider.CONTENT_ITEM_TYPE);

        if(mUri == null){
            action = Intent.ACTION_INSERT;
            setTitle(getString(R.string.new_note));
        } else {
            action = Intent.ACTION_EDIT;
            setTitle(getString(R.string.edit_note));
            noteFilter = DBOpenHelper.NOTE_ID + "=" + mUri.getLastPathSegment();

            Cursor cursor = getContentResolver().query(mUri,
                    DBOpenHelper.ALL_COLUMNS, noteFilter, null, null);
            try{
                cursor.moveToFirst();
                oldText = cursor.getString(cursor.getColumnIndex(DBOpenHelper.NOTE_TEXT));
                editor.setText(oldText);

                oldIsChecked = (cursor.getInt(cursor.getColumnIndex(DBOpenHelper.NOTE_IMPORTANT)) == 1);
                checkBox.setChecked(oldIsChecked);
            } catch (CursorIndexOutOfBoundsException e) {
                e.printStackTrace();
                action = Intent.ACTION_INSERT;
                setTitle(getString(R.string.new_note));
            } finally {
                cursor.close();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(action.equals(Intent.ACTION_EDIT)){
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

    private void deleteNote() {
        DialogInterface.OnClickListener dialogClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int button) {
                        if (button == DialogInterface.BUTTON_POSITIVE) {
                            //Insert Data management code here
                            getContentResolver().delete(NotesProvider.CONTENT_URI, noteFilter, null);
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

    private void finishEditing(){
        String newText = editor.getText().toString().trim();
        boolean isChecked = checkBox.isChecked();

        switch (action) {
            case Intent.ACTION_INSERT:
                if(newText.length() == 0){
                    cancelNote();
                } else {
                    insertNote(newText, isChecked);
                }
                break;
            case Intent.ACTION_EDIT:
                if(newText.length() == 0){
                    deleteNote();
                } else if(oldText.equals(newText) && isChecked == oldIsChecked){
                    cancelNote();
                } else {
                    updateNote(newText, isChecked);
                }
                break;
            default:
                cancelNote();
        }
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

        getContentResolver().update(NotesProvider.CONTENT_URI, values, noteFilter, null);
        Toast.makeText(this, R.string.note_updated, Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, SimpleNoteAppWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(NotesProvider.CONTENT_ITEM_TYPE, mUri);
        int[] ids = AppWidgetManager.getInstance(this)
                .getAppWidgetIds(new ComponentName(getPackageName(), "com.yisuho.simplenote.SimpleNoteAppWidgetProvider"));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,ids);
        sendBroadcast(intent);

        setResult(RESULT_OK);
        finish();
    }

    private void insertNote(String noteText, boolean isChecked) {
        int important = isChecked ? 1 : 0;

        ContentValues values = new ContentValues();
        values.put(DBOpenHelper.NOTE_TEXT, noteText);
        values.put(DBOpenHelper.NOTE_IMPORTANT, important);

        getContentResolver().insert(NotesProvider.CONTENT_URI, values);
        Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onBackPressed() {
        finishEditing();
    }


}

package com.yisuho.simplenote;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class ExportActivity extends AppCompatActivity {

    public static final int MY_WRITE_PERMISSION_CODE = 111;

    public static final String JSON_NOTES = "notes";
    public static final String JSON_CREATED = "created";
    public static final String JSON_IMPORTANT = "important";
    public static final String JSON_TEXT = "text";
    public static final String JSON_HASHTAGS = "hashtags";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button eB = (Button) findViewById(R.id.exportBt);
        eB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityCompat.requestPermissions(ExportActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_WRITE_PERMISSION_CODE);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_WRITE_PERMISSION_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity", Environment.getExternalStorageState());
                    try {
                        File simpleNoteDir = new File(Environment.getExternalStorageDirectory() +
                                File.separator + getString(R.string.export_dir_name));

                        if(!simpleNoteDir.exists()){
                            simpleNoteDir.mkdir();
                        }

                        if (simpleNoteDir.exists()) {
                            File f = new File(simpleNoteDir, getString(R.string.export_file_name));
                            OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(f), getString(R.string.utf_8));

                            Cursor c = getContentResolver().query(NotesProvider.CONTENT_URI_NOTES, null, null, null, null);
                            if(c != null){
                                JSONArray notes = new JSONArray();
                                while(c.moveToNext()){
                                    String created = c.getString(c.getColumnIndex(DBOpenHelper.NOTE_CREATED));
                                    int important = c.getInt(c.getColumnIndex(DBOpenHelper.NOTE_IMPORTANT));
                                    String text = c.getString(c.getColumnIndex(DBOpenHelper.NOTE_TEXT));
                                    JSONObject item = new JSONObject();
                                    item.put(JSON_CREATED, created);
                                    item.put(JSON_IMPORTANT, important);
                                    item.put(JSON_TEXT, text);
                                    notes.put(item);
                                }
                                JSONObject obj = new JSONObject();
                                obj.put(JSON_NOTES, notes);
                                w.append(obj.toString());
                                w.flush();
                                w.close();
                            } else {
                                throw new Exception();
                            }

                            Toast.makeText(getApplicationContext(),
                                    R.string.export_successful, Toast.LENGTH_SHORT).show();
                        } else {
                            throw new FileNotFoundException();
                        }
                    } catch (FileNotFoundException e){
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(),
                                R.string.failed_to_create_directory, Toast.LENGTH_SHORT).show();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(),
                                R.string.export_failed, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(),
                            R.string.no_permission, Toast.LENGTH_SHORT).show();
                }
                return;
            }

        }
    }
}

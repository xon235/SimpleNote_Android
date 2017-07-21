package com.yisuho.simplenote;

import android.Manifest;
import android.content.ContentValues;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                    try {
//                        File downloads = getFilesDir();
                        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        Log.d("ExportActivity", downloads.getAbsolutePath());

                        if (downloads.exists()) {
                            File f = new File(downloads, getString(R.string.export_file_name));
                            OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(f), getString(R.string.utf_8));

                            Cursor c = getContentResolver().query(NotesProvider.CONTENT_URI_NOTES, null, null, null, null);
                            if(c != null){
                                JSONArray notes = new JSONArray();
                                while(c.moveToNext()){
                                    String created = c.getString(c.getColumnIndex(DBOpenHelper.NOTE_CREATED));
                                    int important = c.getInt(c.getColumnIndex(DBOpenHelper.NOTE_IMPORTANT));
                                    String text = c.getString(c.getColumnIndex(DBOpenHelper.NOTE_TEXT));
                                    JSONArray hashtags = new JSONArray();
                                    Pattern p = Pattern.compile(getString(R.string.hashtags_pattern));
                                    Matcher m = p.matcher(text);
                                    while (m.find()) {
                                        String h = m.group(1);
                                        hashtags.put(h);
                                    }
                                    JSONObject item = new JSONObject();
                                    item.put(JSON_CREATED, created);
                                    item.put(JSON_IMPORTANT, important);
                                    item.put(JSON_TEXT, text);
                                    item.put(JSON_HASHTAGS, hashtags);
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
                                R.string.downloads_directory_not_found, Toast.LENGTH_SHORT).show();
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

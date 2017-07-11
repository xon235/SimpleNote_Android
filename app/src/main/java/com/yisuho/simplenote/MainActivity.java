package com.yisuho.simplenote;

import android.Manifest;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, SearchView.OnQueryTextListener {

    public static final int EDITOR_REQUEST_CODE = 100;
    public static final String KEY_PREF_CURRENT = "pref_current";

    private static final String QUERY_TYPE_PLAIN = "plain:";
    private static final String QUERY_TYPE_ALL = "all:";
    private static final String QUERY_TYPE_NO_TAGS = "noTags:";

    private static final int LOADER_NOTES_ID = 0;
    private static final int LOADER_TAGS_ID = 1;

    private CursorAdapter mCursorAdapter;
    private SharedPreferences mSharedPref;

    private LinearLayout mLinearLayoutTags;
    private ArrayList<Button> mTagButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Init floating action button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                startActivityForResult(intent, EDITOR_REQUEST_CODE);
            }
        });

        mCursorAdapter = new NotesCursorAdapter(this, null, 0, getEnableImportant());
        mCursorAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence charSequence) {
                String s = charSequence.toString();
                String filter = "";
                if(s.matches("^"+ QUERY_TYPE_PLAIN + ".*")){
                    filter = DBOpenHelper.NOTE_TEXT
                            + " LIKE \"%" + s.replaceFirst(QUERY_TYPE_PLAIN, "") + "%\"";
                } else if(s.matches("^" + QUERY_TYPE_ALL +".*")){
                    filter = "";
                } else if(s.matches("^" + QUERY_TYPE_NO_TAGS +".*")){
                    filter = DBOpenHelper.NOTE_TEXT
                            + " NOT LIKE \"%" + "#_" + "%\"";
                } else {
                    Log.d("MainActivity", "Query type not set");
                }

                return getContentResolver().query(NotesProvider.CONTENT_URI_NOTES,
                        DBOpenHelper.TABLE_NOTES_ALL_COLUMNS, filter, null, null);
            }
        });

        final ListView list = (ListView) findViewById(R.id.listView);
        final View h = getLayoutInflater().inflate(R.layout.note_list_header, null, false);
        mLinearLayoutTags = (LinearLayout) h.findViewById(R.id.lLtags);
        mLinearLayoutTags.addView(createAllButton(mLinearLayoutTags));
        mLinearLayoutTags.addView(createNoTagsButton(mLinearLayoutTags));
        mTagButtons = new ArrayList<>();
        setHashtagButtons(mLinearLayoutTags, null);

        list.addHeaderView(h);
        list.setAdapter(mCursorAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                Uri uri = Uri.parse(NotesProvider.CONTENT_URI_NOTES + "/" + l);
                intent.putExtra(NotesProvider.CONTENT_ITEM_TYPE, uri);
                startActivityForResult(intent, EDITOR_REQUEST_CODE);
            }
        });

        getLoaderManager().initLoader(LOADER_NOTES_ID, null, this);
        getLoaderManager().initLoader(LOADER_TAGS_ID, null, this);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        if (mSharedPref.getInt(KEY_PREF_CURRENT, -1) < 0) {
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putInt(KEY_PREF_CURRENT, 0);
            editor.apply();
            startActivity(new Intent(this, AboutActivity.class));
        } else {
            //Volley
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = getString(R.string.my_news_url) + "/checkCurrent";

            JsonObjectRequest jsObjRequest = new JsonObjectRequest
                    (Request.Method.GET, url, (String) null, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            checkCurrent(response);
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO Auto-generated method stub

                        }
                    });

            queue.add(jsObjRequest);
        }
    }

    //override this method to change it
    protected boolean getEnableImportant(){
        return true;
    }

    private View createNewTagButton(
            final String tagText, final String queryType,final String filterText, ViewGroup viewGroup) {
        Button b = (Button) getLayoutInflater().inflate(R.layout.tag_button, viewGroup, false);
        b.setText(tagText);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCursorAdapter.getFilter().filter(queryType + filterText);
            }
        });
        return b;
    }

    private View createAllButton(ViewGroup viewGroup) {
        return createNewTagButton(getString(R.string.all), QUERY_TYPE_ALL, "", viewGroup);
    }

    private View createNoTagsButton(ViewGroup viewGroup) {
        return createNewTagButton(getString(R.string.noTags), QUERY_TYPE_NO_TAGS, "", viewGroup);
    }

    private void setHashtagButtons(ViewGroup viewGroup, Cursor c) {
        for(Button b: mTagButtons){
            viewGroup.removeView(b);
        }
        mTagButtons.clear();
//        Cursor c = getContentResolver().query(NotesProvider.CONTENT_URI_TAGS, null, null, null, null);
        if(c != null){
            int tagsTextIndex = c.getColumnIndex(DBOpenHelper.TAGS_TEXT);
            while(c.moveToNext()){
                String hashTag = c.getString(tagsTextIndex);
                Button b = (Button) createNewTagButton(hashTag, QUERY_TYPE_PLAIN, hashTag, viewGroup);
                mTagButtons.add(b);
                viewGroup.addView(b);
            }
            c.close();
        }
    }

    private void checkCurrent(JSONObject responce) {
        try {
            int newCurrent = responce.getInt("current");
            int myCurrent = mSharedPref.getInt(KEY_PREF_CURRENT, -1);

            if (newCurrent > myCurrent) {
                SharedPreferences.Editor editor = mSharedPref.edit();
                editor.putInt(KEY_PREF_CURRENT, newCurrent);
                editor.apply();
                startActivity(new Intent(this, NewsActivity.class));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem alarmMenu = menu.findItem(R.id.action_alarm);

        if (mSharedPref.getBoolean(AlarmSettingsFragment.KEY_PREF_SET_ALARM, false)) {
            alarmMenu.setIcon(R.drawable.ic_alarm_check);
        } else {
            alarmMenu.setIcon(R.drawable.ic_alarm);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_alarm:
                startActivity(new Intent(this, AlarmSettingsActivity.class));
                break;
            case R.id.action_backup:
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 111);
                break;
            case R.id.action_restore:
                restoreNotes();
                break;
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            case R.id.action_news:
                startActivity(new Intent(this, NewsActivity.class));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        mCursorAdapter.getFilter().filter(QUERY_TYPE_PLAIN + s);
        return false;
    }

    @Override
    protected void onResume() {
        invalidateOptionsMenu();
        restartLoader();
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 111: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        File folder = new File(Environment.getExternalStorageDirectory() +
                                File.separator + "SimpleNote");
                        File data = Environment.getDataDirectory();

                        boolean success = true;
                        if (!folder.exists()) {
                            success = folder.mkdir();
                        }

                        if (success) {
                            File currentDB =
                                    new File(data, "/data/com.yisuho.simplenote/databases/notes.db");
                            File backupDB = new File(folder, "SimpleNote.sn");

                            FileChannel src = new FileInputStream(currentDB).getChannel();
                            FileChannel dst = new FileOutputStream(backupDB).getChannel();

                            dst.transferFrom(src, 0, src.size());
                            src.close();
                            dst.close();

                            Toast.makeText(getApplicationContext(),
                                    "저장 OK", Toast.LENGTH_SHORT).show();
                        } else {
                            throw new Exception();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(),
                                "저장 Fail", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(),
                            "권한 없음", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void restoreNotes() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), 100);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    public void restartLoader() {
        getLoaderManager().restartLoader(LOADER_NOTES_ID, null, this);
        getLoaderManager().restartLoader(LOADER_TAGS_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        switch (i){
            case LOADER_NOTES_ID:
                return new CursorLoader(this, NotesProvider.CONTENT_URI_NOTES, null, null, null, null);
            case LOADER_TAGS_ID:
                return new CursorLoader(this, NotesProvider.CONTENT_URI_TAGS, null, null, null, null);
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()){
            case LOADER_NOTES_ID:
                mCursorAdapter.swapCursor(cursor);
                break;
            case LOADER_TAGS_ID:
                setHashtagButtons(mLinearLayoutTags, cursor);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()){
            case LOADER_NOTES_ID:
                mCursorAdapter.swapCursor(null);
            case LOADER_TAGS_ID:
                setHashtagButtons(mLinearLayoutTags, null);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EDITOR_REQUEST_CODE && resultCode == RESULT_OK) {
            restartLoader();
        }
    }
}

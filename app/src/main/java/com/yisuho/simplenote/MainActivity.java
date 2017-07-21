package com.yisuho.simplenote;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, SearchView.OnQueryTextListener {

    public static final int EDITOR_REQUEST_CODE = 100;
    public static final int CHOOSE_FILE_REQUEST_CODE = 101;
    public static final String KEY_PREF_CURRENT = "pref_current";
    public static final String KEY_PREF_SHOW_HELP = "pref_show_help";

    private static final String QUERY_TYPE_PLAIN = "plain:";
    private static final String QUERY_TYPE_ALL = "all:";
    private static final String QUERY_TYPE_NO_TAGS = "noTags:";

    private static final int LOADER_NOTES_ID = 0;
    private static final int LOADER_TAGS_ID = 1;

    private CursorAdapter mCursorAdapter;
    private SharedPreferences mSharedPref;

    private LinearLayout mLinearLayoutTags;
    private ArrayList<Button> mTagButtons;

    private Button mCurrentPressedButton;

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
        mCurrentPressedButton = createAllButton(mLinearLayoutTags);
        mCurrentPressedButton.setEnabled(false);
        mCurrentPressedButton.setBackgroundResource(R.drawable.round_button_background_disabled);
        mLinearLayoutTags.addView(mCurrentPressedButton);
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
            editor.putBoolean(KEY_PREF_SHOW_HELP, false);
            editor.apply();
            startActivity(new Intent(this, AboutActivity.class));
        } else if (mSharedPref.getBoolean(KEY_PREF_SHOW_HELP, true)) {
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putBoolean(KEY_PREF_SHOW_HELP, false);
            editor.apply();
            startActivity(new Intent(this, HelpActivity.class));
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

    private Button createNewTagButton(
            final String tagText, final String queryType,final String filterText, ViewGroup viewGroup) {
        Button b = (Button) getLayoutInflater().inflate(R.layout.tag_button, viewGroup, false);
        b.setText(tagText);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCursorAdapter.getFilter().filter(queryType + filterText);
                mCurrentPressedButton.setEnabled(true);
                mCurrentPressedButton.setBackgroundResource(R.drawable.round_button_background_enabled);
                mCurrentPressedButton = (Button) v;
                mCurrentPressedButton.setEnabled(false);
                mCurrentPressedButton.setBackgroundResource(R.drawable.round_button_background_disabled);
            }
        });
        return b;
    }

    private Button createAllButton(ViewGroup viewGroup) {
        return createNewTagButton(getString(R.string.all), QUERY_TYPE_ALL, "", viewGroup);
    }

    private Button createNoTagsButton(ViewGroup viewGroup) {
        return createNewTagButton(getString(R.string.noTags), QUERY_TYPE_NO_TAGS, "", viewGroup);
    }

    private void setHashtagButtons(ViewGroup viewGroup, Cursor c) {
        for(Button b: mTagButtons){
            viewGroup.removeView(b);
        }
        mTagButtons.clear();
        if(c != null){
            int tagsTextIndex = c.getColumnIndex(DBOpenHelper.TAGS_TEXT);
            while(c.moveToNext()){
                String hashTag = c.getString(tagsTextIndex);
                Button b = createNewTagButton(hashTag, QUERY_TYPE_PLAIN, hashTag, viewGroup);
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
            case R.id.action_export:
                startActivity(new Intent(this, ExportActivity.class));
                break;
            case R.id.action_import:
                importNotes();
                break;
            case R.id.action_help:
                startActivity(new Intent(this, HelpActivity.class));
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

    private void importNotes() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");

        try {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_file)), CHOOSE_FILE_REQUEST_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, R.string.please_install_a_file_browser, Toast.LENGTH_SHORT).show();
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
        } else if (requestCode == CHOOSE_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            try {
                Uri uri = data.getData();
                InputStream is = getContentResolver().openInputStream(uri);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                String jsonString = new String(buffer, getString(R.string.utf_8));
                JSONObject json = new JSONObject(jsonString);
                JSONArray notes = json.getJSONArray(ExportActivity.JSON_NOTES);

                ArrayList<ImportedNote> importedNotes = new ArrayList<>();

                for(int i = 0; i < notes.length(); i++){
                    JSONObject note = notes.getJSONObject(i);
                    String created = note.getString(ExportActivity.JSON_CREATED);
                    int important = note.getInt(ExportActivity.JSON_IMPORTANT) > 0? 1: 0;
                    String text = note.getString(ExportActivity.JSON_TEXT);
                    JSONArray hashtags = note.getJSONArray(ExportActivity.JSON_HASHTAGS);
                    ArrayList<String> hashtagsArray = new ArrayList<>();
                    Pattern p = Pattern.compile(getString(R.string.hashtags_pattern));
                    for(int j = 0; j < hashtags.length(); j++){
                        String h = hashtags.getString(j);
                        Matcher m = p.matcher(text);

                        if(m.matches()){
                            hashtagsArray.add(h);
                        } else {
                            Log.d("MainActivity", "Hashtag format wrong");
                            throw new Exception();
                        }
                    }

                    importedNotes.add(new ImportedNote(created, important, text, hashtagsArray));
                }

                for(ImportedNote iN: importedNotes){
                    ContentValues values = new ContentValues();
                    values.put(DBOpenHelper.NOTE_CREATED, iN.getCreated());
                    values.put(DBOpenHelper.NOTE_TEXT, iN.getText());
                    values.put(DBOpenHelper.NOTE_IMPORTANT, iN.getImportant());
                    String nId = getContentResolver().insert(NotesProvider.CONTENT_URI_NOTES, values).getLastPathSegment();
                    for(String h: iN.getHashtags()){
                        values = new ContentValues();
                        values.put(DBOpenHelper.TAGS_NOTE_ID, nId);
                        values.put(DBOpenHelper.TAGS_TEXT, h);
                        getContentResolver().insert(NotesProvider.CONTENT_URI_TAGS, values);
                    }
                }

                Toast.makeText(getApplicationContext(),
                        R.string.import_successful, Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(),
                        R.string.import_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class ImportedNote{
        private String created;
        private int important;
        private String text;
        private ArrayList<String> hashtags;

        public ImportedNote(String c, int i, String t, ArrayList<String> h){
            created = c;
            important = i;
            text = t;
            hashtags = h;
        }

        public String getCreated() {
            return created;
        }

        public int getImportant() {
            return important;
        }

        public String getText() {
            return text;
        }

        public ArrayList<String> getHashtags() {
            return hashtags;
        }
    }
}

package com.yisuho.simplenote;

import android.app.LoaderManager;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, SearchView.OnQueryTextListener{

    public static final int EDITOR_REQUEST_CODE = 100;
    public static final String KEY_PREF_CURRENT = "pref_current";
    private CursorAdapter mCursorAdapter;
    private SharedPreferences mSharedPref;

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

        mCursorAdapter = new NotesCursorAdapter(this, null, 0, true);
        mCursorAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence charSequence) {
                String filter = DBOpenHelper.NOTE_TEXT + " Like \"%" + charSequence +"%\"";
                return getContentResolver().query(NotesProvider.CONTENT_URI,
                        DBOpenHelper.ALL_COLUMNS, filter, null, null);
            }
        });

        final ListView list = (ListView) findViewById(R.id.listView);
        list.setAdapter(mCursorAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(MainActivity.this, EditorActivity.class);
                Uri uri = Uri.parse(NotesProvider.CONTENT_URI + "/" + l);
                intent.putExtra(NotesProvider.CONTENT_ITEM_TYPE, uri);
                startActivityForResult(intent, EDITOR_REQUEST_CODE);
            }
        });

        getLoaderManager().initLoader(0, null, this);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        if(mSharedPref.getInt(KEY_PREF_CURRENT, -1) < 0){
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putInt(KEY_PREF_CURRENT, 0);
            editor.apply();
            startActivity(new Intent(this, AboutActivity.class));
        } else {
            //Volley
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = getString(R.string.my_news_url) + "/checkCurrent";

            JsonObjectRequest jsObjRequest = new JsonObjectRequest
                    (Request.Method.GET, url, (String)null, new Response.Listener<JSONObject>() {

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

    private void checkCurrent(JSONObject responce) {
        try {
            int newCurrent = responce.getInt("current");
            int myCurrent = mSharedPref.getInt(KEY_PREF_CURRENT, -1);

            if(newCurrent > myCurrent){
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

        if(mSharedPref.getBoolean(AlarmSettingsFragment.KEY_PREF_SET_ALARM, false)){
            alarmMenu.setIcon(R.drawable.ic_alarm_check);
        } else {
            alarmMenu.setIcon(R.drawable.ic_alarm);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        invalidateOptionsMenu();
        restartLoader();
        super.onResume();
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
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            case R.id.action_news:
                startActivity(new Intent(this, NewsActivity.class));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void restartLoader() {
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this, NotesProvider.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mCursorAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == EDITOR_REQUEST_CODE && resultCode == RESULT_OK){
            restartLoader();
        }
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        mCursorAdapter.getFilter().filter(s);
        return false;
    }
}

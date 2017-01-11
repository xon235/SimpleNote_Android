package com.yisuho.simplenote;

import android.app.LoaderManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.RemoteViews;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class AppWidgetConfigureActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>{

    private CursorAdapter mCursorAdapter;

    int mAppWidgetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_widget_configure);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        mCursorAdapter = new NotesCursorAdapter(this, null, 0, false);
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
                Uri uri = Uri.parse(NotesProvider.CONTENT_URI + "/" + l);
                String noteFilter = DBOpenHelper.NOTE_ID + "=" + uri.getLastPathSegment();

                Cursor cursor = getContentResolver().query(uri,
                        DBOpenHelper.ALL_COLUMNS, noteFilter, null, null);

                cursor.moveToFirst();
                String text = cursor.getString(cursor.getColumnIndex(DBOpenHelper.NOTE_TEXT));
                cursor.close();

                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(AppWidgetConfigureActivity.this);
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.appwidget_note);

                Intent intent = new Intent(AppWidgetConfigureActivity.this, EditorActivity.class);
                intent.putExtra(NotesProvider.CONTENT_ITEM_TYPE, uri);
                PendingIntent pendingIntent = PendingIntent.getActivity(AppWidgetConfigureActivity.this, mAppWidgetId, intent, 0);
                views.setOnClickPendingIntent(R.id.contentTv, pendingIntent);

                views.setTextViewText(R.id.contentTv, text);

                appWidgetManager.updateAppWidget(mAppWidgetId, views);

                SharedPreferences sharedPreferences = PreferenceManager
                        .getDefaultSharedPreferences(AppWidgetConfigureActivity.this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(String.valueOf(mAppWidgetId), uri.toString());
                editor.apply();

                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });

        getLoaderManager().initLoader(0, null, this);
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
}

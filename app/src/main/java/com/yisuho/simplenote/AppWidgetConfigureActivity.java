package com.yisuho.simplenote;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RemoteViews;

public class AppWidgetConfigureActivity extends MainActivity {

    int mAppWidgetId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        final ListView list = (ListView) findViewById(R.id.listView);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Uri uri = Uri.parse(NotesProvider.CONTENT_URI_NOTES + "/" + l);
                String noteFilter = DBOpenHelper.NOTE_ID + "=" + uri.getLastPathSegment();

                Cursor cursor = getContentResolver().query(uri,
                        DBOpenHelper.TABLE_NOTES_ALL_COLUMNS, noteFilter, null, null);

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        menu.findItem(R.id.action_alarm).setVisible(false);
        menu.findItem(R.id.action_backup).setVisible(false);
        menu.findItem(R.id.action_restore).setVisible(false);
        menu.findItem(R.id.action_news).setVisible(false);
        menu.findItem(R.id.action_about).setVisible(false);

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    protected boolean getEnableImportant() {
        return false;
    }
}
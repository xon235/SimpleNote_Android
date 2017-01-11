package com.yisuho.simplenote;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by YiSuHo on 10/01/2017.
 */

public class SimpleNoteAppWidgetProvider extends AppWidgetProvider {

    private Uri mUri;
    private boolean mNoteDeleted;

    @Override
    public void onReceive(Context context, Intent intent) {
        mUri = intent.getParcelableExtra(NotesProvider.CONTENT_ITEM_TYPE);
        mNoteDeleted = intent.getBooleanExtra(EditorActivity.EXTRA_NOTE_DELETED, false);
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        if(mUri == null){
            return;
        }

        for(int i = 0; i < appWidgetIds.length; i++){
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            String uriString = sharedPreferences.getString(String.valueOf(appWidgetIds[i]), "");

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget_note);
            Intent intent = new Intent(context, EditorActivity.class);
            intent.putExtra(NotesProvider.CONTENT_ITEM_TYPE, mUri);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetIds[i], intent, 0);

            if(!uriString.equals(mUri.toString())){
                continue;
            }

            if(mNoteDeleted){
                pendingIntent.cancel();

                views.setTextViewText(R.id.contentTv, "");
                views.setInt(R.id.frameLayout, "setBackgroundResource", R.color.widgetTopDisabled);
                views.setInt(R.id.contentTv, "setBackgroundResource", R.color.widgetBackgroundDisabled);
            } else {
                String noteFilter = DBOpenHelper.NOTE_ID + "=" + mUri.getLastPathSegment();

                Cursor cursor = context.getContentResolver().query(mUri,
                        DBOpenHelper.ALL_COLUMNS, noteFilter, null, null);

                cursor.moveToFirst();
                String text = cursor.getString(cursor.getColumnIndex(DBOpenHelper.NOTE_TEXT));
                cursor.close();

                views.setOnClickPendingIntent(R.id.contentTv, pendingIntent);

                views.setTextViewText(R.id.contentTv, text);
            }

            // Tell the AppWidgetManager to perform an update on the current app widget
            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetIds[i], views);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        for(int i = 0; i < appWidgetIds.length; i++){
            editor.putString(String.valueOf(appWidgetIds[i]), "");
        }

        editor.apply();
    }
}

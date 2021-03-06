package com.yisuho.simplenote;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Calendar;

public class AlarmSettingsActivity extends AppCompatActivity {

    private SharedPreferences mSharedPref;
    private boolean mLastSetAlarm;
    private String mLastSetTime;
    private int mLastSetRepeat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.pref_content, new AlarmSettingsFragment())
                .commit();

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mLastSetAlarm = mSharedPref.getBoolean(AlarmSettingsFragment.KEY_PREF_SET_ALARM, false);
        mLastSetTime = mSharedPref.getString(AlarmSettingsFragment.KEY_PREF_SET_TIME, "00:00");
        mLastSetRepeat = mSharedPref.getInt(AlarmSettingsFragment.KEY_PREF_SET_REPEAT, 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_alarm, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case R.id.action_save:
                configureAlarm();
                break;
            case android.R.id.home:
                resetPreference();
                setResult(RESULT_OK);
                finish();
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        resetPreference();
        super.onBackPressed();
    }

    private void resetPreference(){
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putBoolean(AlarmSettingsFragment.KEY_PREF_SET_ALARM, mLastSetAlarm);
        editor.putString(AlarmSettingsFragment.KEY_PREF_SET_TIME, mLastSetTime);
        editor.putInt(AlarmSettingsFragment.KEY_PREF_SET_REPEAT, mLastSetRepeat);
        editor.apply();
    }

    private void configureAlarm(){
        Intent intent;
        PendingIntent pendingIntent;
        AlarmManager alarmManager;

        intent = new Intent(this, AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if(mSharedPref.getBoolean(AlarmSettingsFragment.KEY_PREF_SET_ALARM, false)){
            String time = mSharedPref.getString(AlarmSettingsFragment.KEY_PREF_SET_TIME, "00:00");
            int repeat = mSharedPref.getInt(AlarmSettingsFragment.KEY_PREF_SET_REPEAT, 1);


            int hour = TimePreference.getHour(time);
            int minute = TimePreference.getMinute(time);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);

            if(calendar.before(Calendar.getInstance())){
                calendar.add(Calendar.DATE, 1);
            }

            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY * repeat, pendingIntent);

            Toast.makeText(this, getString(R.string.alarm_set) + " " + time, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
        } else {
            alarmManager.cancel(pendingIntent);
        }

        finish();
    }
}

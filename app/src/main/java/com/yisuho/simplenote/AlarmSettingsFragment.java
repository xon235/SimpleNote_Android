package com.yisuho.simplenote;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;

/**
 * Created by xon23 on 2016-07-11.
 */
public class AlarmSettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String KEY_PREF_SET_ALARM = "pref_set_alarm";
    public static final String KEY_PREF_SET_TIME = "pref_set_time";
    public static final String KEY_PREF_SET_REPEAT= "pref_set_repeat";
    private SharedPreferences mSharedPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mSharedPref.registerOnSharedPreferenceChangeListener(this);
        findPreference(KEY_PREF_SET_TIME).setSummary(mSharedPref.getString(KEY_PREF_SET_TIME,
                getString(R.string.set_time_default)));
        int val = mSharedPref.getInt(KEY_PREF_SET_REPEAT, 0);
        if(val != 0){
            findPreference(KEY_PREF_SET_REPEAT).setSummary(getRepeatSummaryString(val));
        }
    }

    private String getRepeatSummaryString(int val){
        switch (val){
            case 1:
                return getString(R.string.repeat_summary_1);
            case 2:
                return getString(R.string.repeat_summary_2);
            case 3:
                return getString(R.string.repeat_summary_3);
            case 4:
                return getString(R.string.repeat_summary_4);
            case 5:
                return getString(R.string.repeat_summary_5);
            case 6:
                return getString(R.string.repeat_summary_6);
            case 7:
                return getString(R.string.repeat_summary_7);
            default:
                return getString(R.string.repeat_summary_1);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key){
            case KEY_PREF_SET_TIME:
                findPreference(key).setSummary(sharedPreferences.getString(key, ""));
                break;
            case KEY_PREF_SET_REPEAT:
                findPreference(key).setSummary(getRepeatSummaryString(sharedPreferences.getInt(key, 1)));
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mSharedPref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }
}

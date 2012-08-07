package com.robertjscott.earthquake;

import java.util.List;

import android.content.SharedPreferences;
import android.preference.PreferenceActivity;

public class FragmentPreferences extends PreferenceActivity {
    public static final String USER_PREFERENCE = "USER_PREFERENCE";
    public static final String PREF_AUTO_UPDATE = "PREF_AUTO_UPDATE";
    public static final String PREF_MIN_MAG = "PREF_MIN_MAG";
    public static final String PREF_UPDATE_FREQ = "PREF_UPDATE_FREQ";
  
    SharedPreferences prefs;

    @Override
    public void onBuildHeaders(List<Header> target) {
      loadHeadersFromResource(R.xml.preference_headers, target);
    }
}

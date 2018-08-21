package br.com.dercilima.firebackup.prefs;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {

    public static final String PREFERENCES_NAME = "my_preferences";

    private SharedPreferences preferences;

    public Preferences(Context context) {
        this.preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public SharedPreferences getPreferences() {
        return preferences;
    }

    public void putString(String key, String value) {
        getPreferences().edit().putString(key, value).apply();
    }

}

package me.mvdw.device.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import me.mvdw.device.R;

/**
 * Created by Martijn van der Woude on 12-09-15.
 */
public class IceDevicePreferenceActivity extends AppCompatActivity {

    public static Intent getIntentToStartActivity(Context context){
        Intent intent = new Intent(context, IceDevicePreferenceActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        toolbar.setTitle(getString(R.string.screenTitle_preferences));

        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_OK);
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Preference fragment
     */
    public static class DevicePreferenceFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            onSharedPreferenceChanged(getPreferenceScreen().getSharedPreferences(), "");
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference deviceIdPreference = findPreference("preference_proxy_device_id");
            if (deviceIdPreference instanceof EditTextPreference) {
                if(sharedPreferences.getString("preference_proxy_device_id", "") != null) {
                    String summary = sharedPreferences.getString("preference_proxy_device_id", "");

                    if(summary != null && !summary.contentEquals("")) {
                        deviceIdPreference.setSummary(summary);
                    } else {
                        deviceIdPreference.setSummary(getString(R.string.preference_proxy_device_id_summary));
                    }
                }
            }

            Preference customUrlCheckboxPreference = findPreference("preference_proxy_custom_url_checkbox");
            if (customUrlCheckboxPreference instanceof CheckBoxPreference) {
                customUrlCheckboxPreference.setSummary(getString(R.string.preference_proxy_custom_url_checkbox_summary) + getString(R.string.default_proxy_url));
            }

            Preference urlPreference = findPreference("preference_proxy_custom_url");
            if (urlPreference instanceof EditTextPreference) {
                if(sharedPreferences.getString("preference_proxy_custom_url", "") != null) {
                    String summary = sharedPreferences.getString("preference_proxy_custom_url", "");

                    if(summary != null && !summary.contentEquals("")) {
                        urlPreference.setSummary(summary);
                    } else {
                        urlPreference.setSummary(getString(R.string.preference_proxy_custom_url_summary));
                    }
                }
            }

            Preference portPreference = findPreference("preference_proxy_port");
            if (portPreference instanceof EditTextPreference) {
                if(sharedPreferences.getString("preference_proxy_port", "0") != null) {
                    try {
                        int summary = Integer.valueOf(sharedPreferences.getString("preference_proxy_port", "0"));
                        portPreference.setSummary("" + summary);
                    }catch(Exception e) {
                        portPreference.setSummary(getString(R.string.preference_proxy_port_summary));
                    }
                }
            }
        }
    }
}
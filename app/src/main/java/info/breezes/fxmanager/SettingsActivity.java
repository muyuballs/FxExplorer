package info.breezes.fxmanager;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import info.breezes.fxmanager.countly.CountlyActivity;
import info.breezes.toolkit.ui.LayoutViewHelper;
import info.breezes.toolkit.ui.annotation.LayoutView;

public class SettingsActivity extends CountlyActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        LayoutViewHelper.initLayout(this);
        setupSupportActionBar();
        setupActionBar();
        getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new GeneralPreferenceFragment()).commit();
    }


    private void setupActionBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    public static class GeneralPreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            findPreference(getString(R.string.pref_key_theme)).setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            startActivity(new Intent(getActivity(), ThemeChooserActivity.class));
            return true;
        }
    }

}

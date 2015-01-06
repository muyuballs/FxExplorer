package info.breezes.fxmanager;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.preference.PreferenceFragment;
import android.support.v4.app.NavUtils;
import android.support.v4.os.EnvironmentCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import info.breezes.fxmanager.countly.CountlyActivity;
import info.breezes.toolkit.log.Log;
import info.breezes.toolkit.ui.LayoutViewHelper;
import info.breezes.toolkit.ui.annotation.LayoutView;

public class SettingsActivity extends CountlyActivity {

    @LayoutView(R.id.toolbar)
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        LayoutViewHelper.initLayout(this);
        setupActionBar();
        getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new GeneralPreferenceFragment()).commit();

        File f = Environment.getDataDirectory();
        Log.d(null, f.getAbsolutePath());
        StorageManager manager = (StorageManager) getSystemService(STORAGE_SERVICE);
        String[] volumes =StorageTool.getVolumes(manager);
        for (String s : volumes) {
            Log.d(null, s + ":" +StorageTool.getVolumeState(manager, s));
        }
    }



    private void setupActionBar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
        }
    }

}

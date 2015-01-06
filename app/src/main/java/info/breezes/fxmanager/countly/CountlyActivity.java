package info.breezes.fxmanager.countly;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import info.breezes.PreferenceUtil;
import info.breezes.fxmanager.R;
import ly.count.android.api.Countly;

/**
 * Countly Activity
 * Created by Qiao on 2015/1/5.
 */
public class CountlyActivity extends ActionBarActivity {
    protected final static String ACTION_THEME_CHANGED = "info.breezes.fx.theme_changed";
    private BroadcastReceiver themeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_THEME_CHANGED.equals(intent.getAction())) {
                recreate();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(PreferenceUtil.findPreference(this, R.string.pref_key_theme, R.style.AppTheme_Blue));
        super.onCreate(savedInstanceState);
        registerReceiver(themeChangeReceiver, new IntentFilter(ACTION_THEME_CHANGED));
    }

    @Override
    protected void onStop() {
        Countly.sharedInstance().onStop();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Countly.sharedInstance().onStart();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(themeChangeReceiver);
        super.onDestroy();
    }
}

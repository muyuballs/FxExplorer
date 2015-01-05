package info.breezes.fxmanager.countly;

import android.support.v7.app.ActionBarActivity;

import ly.count.android.api.Countly;

/**
 * Countly Activity
 * Created by Qiao on 2015/1/5.
 */
public class CountlyActivity extends ActionBarActivity {

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
}

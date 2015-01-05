package info.breezes.fxmanager.countly;

import android.support.v4.app.Fragment;

import ly.count.android.api.Countly;

/**
 * Countly Fragment
 * Created by Qiao on 2015/1/5.
 */
public class CountlyFragment extends Fragment {

    @Override
    public void onStart() {
        super.onStart();
        Countly.sharedInstance().onStart();
    }

    @Override
    public void onStop() {
        Countly.sharedInstance().onStop();
        super.onStop();
    }

}

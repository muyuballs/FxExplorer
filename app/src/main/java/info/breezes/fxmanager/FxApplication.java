package info.breezes.fxmanager;

import android.app.Application;

import info.breezes.toolkit.log.Log;
import info.breezes.toolkit.log.SystemLogHandler;
import ly.count.android.api.Countly;

/**
 * Created by Qiao on 2014/12/30.
 */
public class FxApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Countly.sharedInstance().setLoggingEnabled(true);
        Countly.sharedInstance().init(this, "http://countly.breezes.info", "74ad07ed8b677d7e7d8516ff8a76cfb9b15586a4");
    }
}

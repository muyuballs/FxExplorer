package info.breezes.fxmanager;

import android.app.Application;

import info.breezes.toolkit.log.Log;
import info.breezes.toolkit.log.SystemLogHandler;

/**
 * Created by Qiao on 2014/12/30.
 */
public class FxApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            //Log.addLogHandler(new SystemLogHandler());
        }
    }
}

package info.breezes.fxmanager.countly;

import ly.count.android.api.Countly;

/**
 * Created by Qiao on 2015/1/5.
 */
public class CountlyUtils {
    public static void addEvent(CountlyEvent event, String value) {
        Countly.sharedInstance().recordEvent(event.toString() + "," + value);
    }
}

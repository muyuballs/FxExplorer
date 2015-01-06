package info.breezes.fxmanager;

import android.os.Environment;
import android.os.storage.StorageManager;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Storage Tools
 * Created by admin on 2015/1/6.
 */
public class StorageTool {
    public static String getVolumeState(StorageManager manager, String s) {
        try {
            Method method = StorageManager.class.getMethod("getVolumeState", String.class);
            return (String) method.invoke(manager, s);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String[] getVolumes(StorageManager manager) {
        try {
            Method method = StorageManager.class.getMethod("getVolumePaths");
            return (String[]) method.invoke(manager, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String[] getMountedVolumes(StorageManager manager) {
        ArrayList<String> mVols = new ArrayList<>();
        String[] vols = getVolumes(manager);
        for (String str : vols) {
            if (Environment.MEDIA_MOUNTED.equals(getVolumeState(manager, str))) {
                mVols.add(str);
            }
        }
        return mVols.toArray(new String[mVols.size()]);
    }
}


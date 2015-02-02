/*
 * Copyright 2015. Qiao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
            return (String[]) method.invoke(manager, (Object[])null);
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


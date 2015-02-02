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

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import info.breezes.PreferenceUtil;
import info.breezes.fxapi.countly.CountlyUtils;
import info.breezes.fxmanager.service.ftp.FtpFileService;
import ly.count.android.api.Countly;

public class FxApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (PreferenceUtil.findPreference(this, info.breezes.fxapi.R.string.pref_key_theme, -1) == -1) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = pref.edit();
            editor.putInt(getString(info.breezes.fxapi.R.string.pref_key_theme), R.style.AppTheme_Purple);
            editor.commit();
        }
        CountlyUtils.COUNTLY_ENABLE = true;
        Countly.sharedInstance().setLoggingEnabled(true);
        Countly.sharedInstance().init(this, "http://countly.breezes.info", "74ad07ed8b677d7e7d8516ff8a76cfb9b15586a4");
        //startService(new Intent(this, FtpFileService.class));
    }
}

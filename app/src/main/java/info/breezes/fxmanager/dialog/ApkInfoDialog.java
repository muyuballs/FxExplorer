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

package info.breezes.fxmanager.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import info.breezes.ComputerUnitUtils;
import info.breezes.fxapi.MediaItem;
import info.breezes.fxapi.countly.CountlyEvent;
import info.breezes.fxapi.countly.CountlyUtils;
import info.breezes.fxmanager.PackageUtils;
import info.breezes.fxmanager.R;

/**
 * apk detail information dialog
 * Created by Qiao on 2015/1/1.
 */
public class ApkInfoDialog {
    public static void showApkInfoDialog(final Context context, final MediaItem item) {
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(item.path, 0);
        if (info == null) {
            return;
        }
        ApplicationInfo appInfo = info.applicationInfo;
        appInfo.sourceDir = item.path;
        appInfo.publicSourceDir = item.path;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(item.title);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        View view = layoutInflater.inflate(R.layout.dialog_apk_info, null);
        ((ImageView) view.findViewById(R.id.icon)).setImageDrawable(pm.getApplicationIcon(appInfo));
        ((TextView) view.findViewById(R.id.title)).setText(pm.getApplicationLabel(appInfo));
        ((TextView) view.findViewById(R.id.version)).setText(info.versionName);
        ((TextView) view.findViewById(R.id.size)).setText(ComputerUnitUtils.toReadFriendly(item.length));
        ((TextView) view.findViewById(R.id.packageName)).setText(info.packageName);
        try {
            PackageInfo pInfo = pm.getPackageInfo(info.packageName, 0);
            ((TextView) view.findViewById(R.id.installVersion)).setText(String.format(context.getString(R.string.apk_detail_installed), pInfo.versionName));
        } catch (PackageManager.NameNotFoundException e) {
            ((TextView) view.findViewById(R.id.installVersion)).setText(context.getString(R.string.apk_detail_not_install));
        }
        builder.setView(view);
        builder.setPositiveButton(context.getString(R.string.btn_install), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CountlyUtils.addEvent(CountlyEvent.INSTALL, "");
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(android.content.Intent.ACTION_VIEW);
                Uri uri = Uri.fromFile(new File(item.path));
                intent.setDataAndType(uri, MimeTypeMap.getSingleton().getMimeTypeFromExtension("apk"));
                context.startActivity(intent);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    public static void showApkInfoDialog(final Context context, final PackageInfo info) {
        PackageManager pm = context.getPackageManager();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(pm.getApplicationLabel(info.applicationInfo));
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        View view = layoutInflater.inflate(R.layout.dialog_installed_apk_info, null);
        ((ImageView) view.findViewById(R.id.icon)).setImageDrawable(pm.getApplicationIcon(info.applicationInfo));
        ((TextView) view.findViewById(R.id.title)).setText(pm.getApplicationLabel(info.applicationInfo));
        ((TextView) view.findViewById(R.id.version)).setText(info.versionName);
        ((TextView) view.findViewById(R.id.size)).setText(ComputerUnitUtils.toReadFriendly(getApkSize(info.applicationInfo.sourceDir)));
        ((TextView) view.findViewById(R.id.packageName)).setText(info.packageName);
        ((TextView) view.findViewById(R.id.packageType)).setText(getPackageType(info.applicationInfo));
        builder.setView(view);
        builder.setNegativeButton(context.getString(R.string.action_detail), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", info.packageName, null);
                intent.setData(uri);
                context.startActivity(intent);
            }
        });
        if ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
            builder.setNeutralButton(R.string.uninstall, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    PackageUtils.unInstall(context, info.packageName);
                }
            });
        }
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private static long getApkSize(String sourceDir) {
        File file = new File(sourceDir);
        if (file.exists()) {
            return file.length();
        }
        return -1;
    }

    private static int getPackageType(ApplicationInfo app) {
        if ((app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
            return R.string.system_update;
        }

        if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
            return R.string.system;
        }

        return R.string.downloaded;
    }
}

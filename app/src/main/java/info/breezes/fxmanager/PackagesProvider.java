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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import info.breezes.StreamUtils;
import info.breezes.fxapi.MediaItem;
import info.breezes.fxapi.MediaItemViewer;
import info.breezes.fxapi.MediaProvider;
import info.breezes.fxmanager.dialog.ApkInfoDialog;
import info.breezes.toolkit.ui.Toast;

public class PackagesProvider extends MediaProvider {
    public PackagesProvider(Context context) {
        super(context);
    }

    @Override
    public List<MediaItem> loadMedia(String path, boolean showHidden) {
        PackageManager pm = mContext.getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);
        List<MediaItem> mediaItems = new ArrayList<>();
        for (PackageInfo packageInfo : packages) {
            MediaItem mediaItem = new MediaItem();
            mediaItem.type = MediaItem.MediaType.File;
            mediaItem.path = packageInfo.packageName;
            mediaItem.subType = MediaItem.SubMediaType.Apk;
            mediaItem.tag = packageInfo;
            mediaItem.title = pm.getApplicationLabel(packageInfo.applicationInfo).toString();
            mediaItem.length = getApkSize(packageInfo.applicationInfo.sourceDir);
            mediaItems.add(mediaItem);
        }
        return mediaItems;
    }

    private static long getApkSize(String sourceDir) {
        File file = new File(sourceDir);
        if (file.exists()) {
            return file.length();
        }
        return -1;
    }

    @Override
    public void launch(Activity activity, MediaItem item) {
        ApkInfoDialog.showApkInfoDialog(activity, (PackageInfo) item.tag);
    }

    @Override
    public void loadActionMenu(MenuInflater menuInflater, Menu menu, List<MediaItem> selectedItems) {
        menuInflater.inflate(R.menu.menu_packages, menu);
        if (selectedItems.size() > 1) {
            MenuItem item = menu.findItem(R.id.action_send);
            if (item != null) {
                item.setVisible(false);
            }
            item = menu.findItem(R.id.action_delete);
            if (item != null) {
                item.setVisible(false);
            }
        }
    }

    @Override
    public boolean onActionItemClicked(Activity activity, MediaItemViewer viewer, MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_send) {
            try {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(((PackageInfo) viewer.getSelectedItems().get(0).tag).applicationInfo.sourceDir)));
                intent.setType("application/vnd.android.package-archive");
                mContext.startActivity(Intent.createChooser(intent, "发送到"));
            } catch (Exception exp) {
                Toast.showText(mContext, exp.getMessage());
            }
        } else if (menuItem.getItemId() == R.id.action_delete) {
            PackageUtils.unInstall(mContext, ((PackageInfo) viewer.getSelectedItems().get(0).tag).packageName);
        } else if (menuItem.getItemId() == R.id.action_backup) {
            backupPackages(activity, viewer.getSelectedItems());
        }
        return true;
    }

    private void backupPackages(final Activity activity, final List<MediaItem> selectedItems) {
        new AsyncTask<Void, String, Void>() {
            private ProgressDialog progressDialog;
            private int successCount = 0;
            private int current;

            @Override
            protected void onPreExecute() {
                progressDialog = new ProgressDialog(activity);
                progressDialog.setCancelable(false);
                progressDialog.show();
            }

            @Override
            protected void onPostExecute(Void result) {
                Toast.showText(mContext, String.format(mContext.getString(R.string.backup_result_tip), successCount, selectedItems.size() - successCount));
                progressDialog.dismiss();
            }

            @Override
            protected void onProgressUpdate(String... values) {
                super.onProgressUpdate(values);
                current++;
                progressDialog.setMessage("(" + current + "/" + selectedItems.size() + ") " + values[0]);
            }

            @Override
            protected Void doInBackground(Void... params) {
                File appBackupDir = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "backups" + File.separator + "apps");
                if (!appBackupDir.exists()) {
                    appBackupDir.mkdirs();
                }
                for (MediaItem item : selectedItems) {
                    publishProgress(item.title);
                    String path = ((PackageInfo) item.tag).applicationInfo.sourceDir;
                    File file = new File(path);
                    FileInputStream fis = null;
                    FileOutputStream fos = null;
                    try {
                        fis = new FileInputStream(file);
                        fos = new FileOutputStream(appBackupDir + File.separator + item.title + "_" + file.getName());
                        StreamUtils.copy(fis, fos, 8096, null);
                        successCount++;
                    } catch (Exception exp) {
                        Log.d(null, exp.getMessage(), exp);
                    } finally {
                        StreamUtils.safeClose(fis);
                        StreamUtils.safeClose(fos);
                    }
                }
                return null;
            }
        }.execute();
    }

    @Override
    public Drawable loadMediaIcon(MediaItem item) {
        Drawable icon = mIconCache.get(item.path);
        if (icon == null) {
            PackageManager pm = mContext.getPackageManager();
            try {
                icon = pm.getApplicationIcon(item.path);
            } catch (PackageManager.NameNotFoundException e) {
                icon = pm.getDefaultActivityIcon();
            }
            mIconCache.put(item.path, icon);
        }
        return icon;
    }

    @Override
    public String getMimeType(MediaItem item) {
        return "application/vnd.android.package-archive";
    }

}

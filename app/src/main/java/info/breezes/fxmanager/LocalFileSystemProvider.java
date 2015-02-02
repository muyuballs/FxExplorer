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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import info.breezes.ComputerUnitUtils;
import info.breezes.ImageUtility;
import info.breezes.fxapi.MediaItem;
import info.breezes.fxapi.MediaItemViewer;
import info.breezes.fxapi.MediaProvider;
import info.breezes.fxapi.countly.CountlyEvent;
import info.breezes.fxapi.countly.CountlyUtils;
import info.breezes.fxmanager.android.app.QAlertDialog;
import info.breezes.fxmanager.dialog.ApkInfoDialog;
import info.breezes.fxmanager.dialog.FileInfoDialog;
import info.breezes.fxmanager.service.FileService;
import info.breezes.toolkit.log.Log;
import info.breezes.toolkit.ui.Toast;

public class LocalFileSystemProvider extends MediaProvider {

    public LocalFileSystemProvider(Context context) {
        super(context);
    }

    @Override
    public List<MediaItem> loadMedia(String path, boolean showHidden) {
        File root = new File(path);
        Log.d(null, path);
        if (!root.canRead()) {
            throw new RuntimeException(mContext.getString(R.string.tip_no_read_permission));
        }
        if (root.isDirectory()) {
            List<MediaItem> mediaItems = new ArrayList<>();
            File[] files = root.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isHidden() && !showHidden) {
                        continue;
                    }
                    MediaItem mediaItem = new MediaItem();
                    mediaItem.path = f.getAbsolutePath();
                    mediaItem.title = f.getName();
                    mediaItem.lastModify = f.lastModified();
                    if (f.isDirectory()) {
                        String[] fs = f.list();
                        mediaItem.childCount = fs != null ? fs.length : 0;
                        mediaItem.type = MediaItem.MediaType.Folder;
                    } else if (f.isFile()) {
                        mediaItem.length = f.length();
                        mediaItem.type = MediaItem.MediaType.File;
                    }
                    mediaItems.add(mediaItem);
                }
            }
            return mediaItems;
        }
        return null;
    }

    @Override
    public Drawable loadMediaIcon(MediaItem item) {
        Drawable icon = mIconCache.get(item.path);
        if (icon == null) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(item.path);
            if (extension != null) {
                String mime = getMimeType(item);
                if ("apk".equalsIgnoreCase(extension)) {
                    icon = getApkIcon(item.path, mContext);
                } else if (mime != null) {
                    if (mime.startsWith("image/")) {
                        icon = getImageThumbnail(item.path, 48, 48);
                    } else if (mime.startsWith("video/")) {
                        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(item.path, MediaStore.Images.Thumbnails.MICRO_KIND);
                        if (bitmap != null) {
                            icon = new BitmapDrawable(mContext.getResources(), bitmap);
                        } else {
                            icon = mContext.getResources().getDrawable(R.drawable.ic_movies);
                        }
                    } else if (mime.startsWith("audio/")) {
                        icon = mContext.getResources().getDrawable(R.drawable.ic_music);
                    } else if (mime.startsWith("text/")) {
                        icon = mContext.getResources().getDrawable(R.drawable.ic_text);
                    }
                }
            }
            if (icon != null) {
                mIconCache.put(item.path, icon);
                Log.d(null, "CacheSize:" + ComputerUnitUtils.toReadFriendly(mIconCache.size()) + "/" + ComputerUnitUtils.toReadFriendly(mIconCache.maxSize()));
            }
        }
        return icon;
    }

    @Override
    public void launch(Activity activity, MediaItem item) {
        try {
            if (FileUtils.isApk(getMimeType(item), item)) {
                ApkInfoDialog.showApkInfoDialog(activity, item);
            } else {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(android.content.Intent.ACTION_VIEW);
                Uri uri = Uri.fromFile(new File(item.path));
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(item.path));
                Log.d(null, "MimeType:" + mimeType);
                intent.setDataAndType(uri, TextUtils.isEmpty(mimeType) ? "*/*" : mimeType);
                mContext.startActivity(intent);
            }
        } catch (Exception exp) {
            Toast.showText(mContext, exp.getMessage());
        }
    }

    @Override
    public String getMimeType(MediaItem item) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(item.path);
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (TextUtils.isEmpty(extension) || TextUtils.isEmpty(mime)) {
            mime = isImageFile(item.path);
        }
        return mime;
    }

    @Override
    public Rect getImageSize(MediaItem item) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(item.path, options);
        return new Rect(0, 0, options.outWidth, options.outHeight);
    }

    @Override
    public void loadActionMenu(MenuInflater menuInflater, Menu menu, List<MediaItem> selectedItems) {
        menuInflater.inflate(selectedItems.size() > 1 ? R.menu.menu_mutil_item : R.menu.menu_single_item, menu);
        if (selectedItems.get(0).type == MediaItem.MediaType.File) {
            MenuItem item = menu.findItem(R.id.action_add_bookmark);
            if (item != null) {
                item.setEnabled(false);
            }
        }
    }

    @Override
    public boolean onActionItemClicked(Activity activity, MediaItemViewer viewer, MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_select_all) {
            CountlyUtils.addEvent(CountlyEvent.SELECT_ALL, "");
            viewer.setSelectAll();
        } else if (menuItem.getItemId() == R.id.action_detail) {
            CountlyUtils.addEvent(CountlyEvent.OPEN_DETAIL, "");
            if (viewer.getSelectedCount() > 1) {
                Toast.showText(activity, mContext.getString(R.string.tip_cannt_show_multi_detail));
            } else {
                showItemDetailInfo(activity, viewer.getSelectedItems().get(0));
            }
        } else if (menuItem.getItemId() == R.id.action_delete) {
            CountlyUtils.addEvent(CountlyEvent.DELETE, "");
            deleteMediaItems(viewer, activity, viewer.getSelectedItems());
        } else if (menuItem.getItemId() == R.id.action_zip) {
            CountlyUtils.addEvent(CountlyEvent.COMPRESS, "");
            compressMediaItems(viewer, activity, viewer.getSelectedItems());
        } else if (menuItem.getItemId() == R.id.action_rename) {
            CountlyUtils.addEvent(CountlyEvent.RENAME, "");
            renameMediaItem(viewer, activity, viewer.getSelectedItems().get(0));
        } else if (menuItem.getItemId() == R.id.action_add_bookmark) {
            CountlyUtils.addEvent(CountlyEvent.PIN_START, "");
            pinToStart(viewer.getSelectedItems().get(0));
        } else if (menuItem.getItemId() == R.id.action_qrcode) {
            showQrCode(activity, viewer.getSelectedItems().get(0));
        }
        return true;
    }

    private void showItemDetailInfo(Activity activity, MediaItem mediaItem) {
        if (mediaItem.type == MediaItem.MediaType.File) {
            if (FileUtils.isApk(getMimeType(mediaItem), mediaItem)) {
                ApkInfoDialog.showApkInfoDialog(activity, mediaItem);
            } else {
                FileInfoDialog.showSingleFileInfoDialog(activity, mediaItem, this, true);
            }
        } else {
            FileInfoDialog.showSingleFolderInfoDialog(activity, mediaItem, this);
        }
    }

    private void pinToStart(MediaItem item) {
        Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        //快捷方式的名称
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, item.title);
        shortcut.putExtra("duplicate", false); //不允许重复创建
        Intent innerIntent = new Intent(mContext, MainActivity.class);
        innerIntent.putExtra(MediaItemViewer.EXTRA_INIT_DIR, item.path);
        innerIntent.putExtra(MediaItemViewer.EXTRA_DIR_NAME, item.title);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, innerIntent);
        //快捷方式的图标
        Intent.ShortcutIconResource icon = Intent.ShortcutIconResource.fromContext(mContext, R.drawable.ic_action_collection);
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
        mContext.sendBroadcast(shortcut);
        Toast.showText(mContext, String.format(mContext.getString(R.string.tip_pin_start_ok), item.title))
        ;
    }

    private void deleteMediaItems(final MediaItemViewer viewer, final Activity activity, final List<MediaItem> items) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(android.R.string.dialog_alert_title);
        builder.setMessage(mContext.getString(R.string.dialog_msg_are_you_confirm_delete_them));
        builder.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int deleted = MediaItemUtil.delete(true, items);
                if (deleted > 0) {
                    if (deleted < items.size()) {
                        Toast.showText(mContext, mContext.getString(R.string.tip_part_of_delete_ok));
                    } else {
                        Toast.showText(mContext, mContext.getString(R.string.tip_delete_ok));
                    }
                    viewer.reloadMediaList();
                }
            }
        });
        builder.setPositiveButton(android.R.string.cancel, null);
        builder.show();
    }

    private void renameMediaItem(final MediaItemViewer viewer, final Activity activity, final MediaItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("");
        @SuppressLint("InflateParams")
        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_content, null);
        final EditText editText = (EditText) content.findViewById(R.id.editText);
        editText.setText(item.title);
        builder.setView(content);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = editText.getText().toString();
                if (TextUtils.isEmpty(newName)) {
                    Toast.showText(mContext, mContext.getString(R.string.tip_file_name_cannt_null));
                    return;
                }
                dialog.dismiss();
                if (MediaItemUtil.rename(item, newName)) {
                    viewer.reloadMediaList();
                } else {
                    Toast.showText(mContext, mContext.getString(R.string.tip_rename_failed));
                }
            }
        });
        AlertDialog dialog = builder.create();
        QAlertDialog.setAutoDismiss(dialog, false);
        dialog.show();
    }

    private void compressMediaItems(final MediaItemViewer viewer, final Activity activity, final List<MediaItem> items) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(mContext.getString(R.string.dialog_title_tip));
        @SuppressLint("InflateParams")
        View content = LayoutInflater.from(mContext).inflate(R.layout.dialog_edit_content, null);
        final EditText editText = (EditText) content.findViewById(R.id.editText);
        editText.setHint(mContext.getString(R.string.hint_target_file_name));
        builder.setView(content);
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = editText.getText().toString();
                if (TextUtils.isEmpty(newName)) {
                    Toast.showText(mContext, mContext.getString(R.string.tip_file_name_cannt_null));
                    return;
                }
                String out = viewer.getCurrentPath() + File.separator + newName;
                if (!out.endsWith("\\.zip")) {
                    out += ".zip";
                }
                File file = new File(out);
                if (file.exists()) {
                    Toast.showText(mContext, mContext.getString(R.string.tip_file_already_exists));
                    return;
                }
                MediaItemUtil.compress(out, new MediaItemUtil.OnProgressChangeListener() {
                    private ProgressDialog pd;

                    @Override
                    public void onPreExecute() {
                        pd = new ProgressDialog(activity);
                        pd.setCancelable(false);
                        pd.setTitle(mContext.getString(R.string.dialog_title_compressing));
                        pd.setIndeterminate(true);
                        pd.show();
                    }

                    @Override
                    public void onProgressChanged(String file, long max, long current) {
                        pd.setMessage(file);
                    }

                    @Override
                    public void onPostExecute(boolean success) {
                        pd.dismiss();
                        if (success) {
                            Toast.showText(mContext, mContext.getString(R.string.tip_compress_ok));
                            viewer.reloadMediaList();
                        } else {
                            Toast.showText(mContext, mContext.getString(R.string.tip_compress_failed));
                        }
                    }
                }, items);
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        QAlertDialog.setAutoDismiss(dialog, false);
        dialog.show();
    }

    private void showQrCode(final Activity activity, final MediaItem item) {
        new AsyncTask<Void, Void, Void>() {
            private Dialog dialog;
            private ImageView imageView;

            @Override
            protected void onPreExecute() {
                imageView = new ImageView(activity);
                ProgressBar pd = new ProgressBar(activity);
                pd.setIndeterminate(true);
                dialog = new Dialog(activity, R.style.Dialog_NoTitle);
                dialog.setContentView(pd);
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        FileService.removeFile(activity, item.path);
                    }
                });
                if (!activity.isFinishing()) {
                    dialog.show();
                }
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                dialog.setContentView(imageView);
            }

            @Override
            protected Void doInBackground(Void... params) {
//                String path = FileService.startServeFile(activity, item.path, 0);
//                try {
//                    QRCodeWriter writer = new QRCodeWriter();
//                    BitMatrix matrix = writer.encode(path, BarcodeFormat.QR_CODE, 512, 512);
//                    Bitmap bitmap = Bitmap.createBitmap(matrix.getWidth(), matrix.getHeight(), Bitmap.Config.RGB_565);
//                    Canvas canvas = new Canvas(bitmap);
//                    canvas.drawColor(Color.WHITE);
//                    Paint paint = new Paint();
//                    TypedArray array = activity.getTheme().obtainStyledAttributes(R.styleable.Theme);
//                    paint.setColor(array.getColor(R.styleable.Theme_colorPrimary, Color.BLACK));
//                    array.recycle();
//                    for (int i = 0; i < matrix.getHeight(); i++) {
//                        for (int x = 0; x < matrix.getWidth(); x++) {
//                            if (matrix.get(x, i)) {
//                                canvas.drawPoint(x, i, paint);
//                            }
//                        }
//                    }
//                    imageView.setImageBitmap(bitmap);
//                } catch (WriterException e) {
//                    e.printStackTrace();
//                }
                return null;
            }
        }.execute();

    }

    private String isImageFile(String path) {
        File file = new File(path);
        if (file.isFile() && file.exists() && file.canRead()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                byte[] buf = new byte[11];
                int c = fis.read(buf);
                while (c < 11) {
                    c += fis.read(buf, c, 11 - c);
                }
                fis.close();
                return FileUtils.isImage(buf);
            } catch (Exception e) {
                Log.d(null, e.getMessage(), e);
            }
        }
        return null;
    }

    private Drawable getImageThumbnail(String imagePath, int width, int height) {
        Bitmap bitmap = ImageUtility.getImageWithFilePathAndSize(imagePath, width, height);

        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return new BitmapDrawable(mContext.getResources(), bitmap);
    }

    public static Drawable getApkIcon(String apkPath, Context mContext) {
        PackageManager pm = mContext.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
        if (info != null) {
            ApplicationInfo appInfo = info.applicationInfo;
            appInfo.sourceDir = apkPath;
            appInfo.publicSourceDir = apkPath;
            return appInfo.loadIcon(pm);
        }
        return null;
    }
}

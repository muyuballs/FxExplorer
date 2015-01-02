package info.breezes.fxmanager;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import info.breezes.ComputerUnitUtils;
import info.breezes.ImageUtility;
import info.breezes.fxmanager.model.MediaItem;
import info.breezes.toolkit.log.Log;

/**
 * Created by Qiao on 2014/12/30.
 */
public class LocalFileSystemProvider extends MediaProvider {

    public LocalFileSystemProvider(Context context) {
        super(context);
    }

    @Override
    public List<MediaItem> loadMedia(String path, boolean showHidden) {
        File root = new File(path);
        Log.d(null, path);
        if (!root.canRead()) {
            throw new RuntimeException("没有读取权限");
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
                } else if (mime != null && mime.startsWith("image/")) {
                    icon = getImageThumbnail(item.path, 48, 48);
                } else if (mime != null && mime.startsWith("video/")) {
                    Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(item.path, MediaStore.Images.Thumbnails.MICRO_KIND);
                    if (bitmap != null) {
                        icon = new BitmapDrawable(mContext.getResources(), bitmap);
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
    public String getMimeType(MediaItem item) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(item.path);
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (TextUtils.isEmpty(mime)) {
            mime = testFileIsImage(item.path);
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

    private String testFileIsImage(String path) {
        File file = new File(path);
        if (file.isFile() && file.exists() && file.canRead()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                byte[] buf = new byte[8];
                fis.read(buf);
                fis.close();
                byte[] gifbuf = new byte[6];
                System.arraycopy(buf, 0, gifbuf, 0, 6);
                if (Arrays.equals(gifbuf, new byte[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61}) || Arrays.equals(buf, new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61})) {//gif
                    return "image/gif";
                }
                if (buf[0] == 0xff && buf[1] == 0xd8) {//jpg
                    return "image/jpeg";
                }
                if (Arrays.equals(buf, new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})) {//png
                    return "image/png";
                }
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

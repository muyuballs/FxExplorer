package info.breezes.fxmanager;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import java.util.List;

import info.breezes.fxmanager.model.MediaItem;

/**
 * Created by admin on 2015/1/5.
 */
public class PackagesProvider extends MediaProvider {
    public PackagesProvider(Context context) {
        super(context);
    }

    @Override
    public List<MediaItem> loadMedia(String path, boolean showHidden) {
        List<PackageInfo> packages = mContext.getPackageManager().getInstalledPackages(0);
        return null;
    }

    @Override
    public Drawable loadMediaIcon(MediaItem item) {
        return null;
    }

    @Override
    public Drawable loadMediaBitmapIcon(MediaItem item) {
        return null;
    }

    @Override
    public String getMimeType(MediaItem item) {
        return "application/vnd.android.package-archive";
    }

}

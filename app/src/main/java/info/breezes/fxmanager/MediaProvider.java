package info.breezes.fxmanager;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

import java.io.Serializable;
import java.util.List;

import info.breezes.ComputerUnitUtils;
import info.breezes.fxmanager.model.MediaItem;
import info.breezes.toolkit.log.Log;

/**
 * Created by Qiao on 2014/12/30.
 */
public abstract class MediaProvider implements Serializable {
    protected final Context mContext;
    protected final IconCache mIconCache;

    public MediaProvider(Context context) {
        this.mContext = context;
        this.mIconCache = new IconCache(20 * 1024 * 1024);
    }

    public abstract List<MediaItem> loadMedia(String path, boolean showHidden);

    public abstract Drawable loadMediaIcon(MediaItem item);

    public abstract String getMimeType(MediaItem item);

    public abstract Rect getImageSize(MediaItem item);

    class IconCache extends LruCache<String, Drawable> {
        public IconCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(String key, Drawable value) {
            if (value instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) value;
                if (bitmapDrawable.getBitmap() != null && !bitmapDrawable.getBitmap().isRecycled()) {
                    int size = bitmapDrawable.getBitmap().getRowBytes() * bitmapDrawable.getBitmap().getHeight();
                    Log.d(null, "bitmap drawable:" + ComputerUnitUtils.toReadFriendly(size));
                    return size;
                }
            }
            return super.sizeOf(key, value);
        }

    }
}


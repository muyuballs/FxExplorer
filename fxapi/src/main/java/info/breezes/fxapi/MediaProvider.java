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

package info.breezes.fxapi;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.io.Serializable;
import java.util.List;

import info.breezes.ComputerUnitUtils;
import info.breezes.toolkit.log.Log;

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

    public abstract void launch(Activity activity, MediaItem item);

    public Rect getImageSize(MediaItem item) {
        return null;
    }

    public abstract void loadActionMenu(MenuInflater menuInflater, Menu menu, List<MediaItem> selectedItems);

    public abstract boolean onActionItemClicked(Activity activity, MediaItemViewer viewer, MenuItem menuItem);

    public class IconCache extends LruCache<String, Drawable> {
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


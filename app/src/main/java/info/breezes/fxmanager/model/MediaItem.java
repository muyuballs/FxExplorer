package info.breezes.fxmanager.model;

import android.graphics.drawable.Drawable;

import java.io.Serializable;

/**
 * Created by Qiao on 2014/12/30.
 */
public class MediaItem implements Serializable {


    public static enum MediaType {
        Folder, File
    }

    public String path;
    public String title;
    public MediaType type;
    public long length;
    public int childCount;
    public long lastModify;
    public String subType;
    public transient Drawable icon;

    @Override
    public String toString() {
        return title;
    }
}

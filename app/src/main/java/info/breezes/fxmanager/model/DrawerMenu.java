package info.breezes.fxmanager.model;

import android.graphics.drawable.Drawable;

import java.io.Serializable;

/**
 * Created by admin on 2014/12/30.
 */
public class DrawerMenu implements Serializable{
    public int id;
    public String title;
    public transient  Drawable icon;
    public String mediaProvider;
    public String path;

    public DrawerMenu(String title, String path) {
        this(0, title, path, null);
    }

    public DrawerMenu(String title, String path, Drawable icon) {
        this(0, title, path, icon);
    }

    public DrawerMenu(int id, String title, String path, Drawable icon) {
        this(id, title, icon, path, null);
    }

    public DrawerMenu(int id, String title, Drawable icon, String path, String mediaProvider) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.path = path;
        this.mediaProvider = mediaProvider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DrawerMenu that = (DrawerMenu) o;

        if (id != that.id) return false;
        if (mediaProvider != null ? !mediaProvider.equals(that.mediaProvider) : that.mediaProvider != null)
            return false;
        if (path != null ? !path.equals(that.path) : that.path != null) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (mediaProvider != null ? mediaProvider.hashCode() : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        return result;
    }
}

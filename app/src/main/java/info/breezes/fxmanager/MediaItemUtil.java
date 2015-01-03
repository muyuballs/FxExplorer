package info.breezes.fxmanager;

import java.io.File;

import info.breezes.fxmanager.model.MediaItem;

/**
 * MediaItem Operations
 * Created by Qiao on 2015/1/2.
 */
public class MediaItemUtil {
    public static void delete(boolean fall, MediaItem... items) {

    }

    public static boolean rename(MediaItem item, String newName) {
        File file = new File(item.path);
        return file.renameTo(new File(file.getParent() + File.separator + newName));
    }

    public static void compress(String outFile, MediaItem... items) {

    }
}


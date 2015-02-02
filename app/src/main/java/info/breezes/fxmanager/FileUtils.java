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


import java.util.Arrays;

import info.breezes.fxapi.MediaItem;

public class FileUtils {

    public static boolean isApk(String mime, MediaItem item) {
        return "application/vnd.android.package-archive".equalsIgnoreCase(mime) || "apk".equalsIgnoreCase(MimeTypeMap.getFileExtensionFromUrl(item.path));
    }

    public static String isImage(byte[] buf) {
        if (buf == null || buf.length < 8) {
            return null;
        }
        byte[] bytes = new byte[6];
        System.arraycopy(buf, 0, bytes, 0, 6);
        if (isGif(bytes)) {
            return "image/gif";
        }
        bytes = new byte[4];
        System.arraycopy(buf, 6, bytes, 0, 4);
        if (isJpeg(bytes)) {
            return "image/jpeg";
        }
        bytes = new byte[8];
        System.arraycopy(buf, 0, bytes, 0, 8);
        if (isPng(bytes)) {
            return "image/png";
        }
        return null;
    }

    private static final byte[] pngMagic = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
    private static final byte[] jpegMagic = new byte[]{0x4a, 0x46, 0x49, 0x46};
    private static final byte[] gifMagic0 = new byte[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61};
    private static final byte[] getGifMagic1 = new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61};

    /**
     * @param data first 6 bytes of file
     * @return gif image file true,other false
     */
    public static boolean isGif(byte[] data) {
        return Arrays.equals(data, gifMagic0) || Arrays.equals(data, getGifMagic1);
    }

    /**
     * @param data first 4 bytes of file
     * @return jpeg image file true,other false
     */
    public static boolean isJpeg(byte[] data) {
        return Arrays.equals(data, jpegMagic);
    }

    /**
     * @param data first 8 bytes of file
     * @return png image file true,other false
     */
    public static boolean isPng(byte[] data) {
        return Arrays.equals(data, pngMagic);
    }
}

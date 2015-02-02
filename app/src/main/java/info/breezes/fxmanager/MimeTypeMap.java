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

import android.text.TextUtils;

import java.util.HashMap;

/**
 * Created by Qiao on 2015/1/3.
 */
public class MimeTypeMap {
    private static MimeTypeMap instance;
    private static HashMap<String, String> innerMimeMap = new HashMap<>();

    static {
        innerMimeMap.put("ogg","audio/ogg");
    }

    public static String getFileExtensionFromUrl(String path) {
        String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(path);
        if (TextUtils.isEmpty(extension)) {
            if (path.contains(".") && !path.endsWith("\\.")) {
                extension = path.substring(path.lastIndexOf('.') + 1);
            }
        }
        return extension;
    }

    public static synchronized MimeTypeMap getSingleton() {
        if (instance == null) {
            instance = new MimeTypeMap();
        }
        return instance;
    }

    public String getMimeTypeFromExtension(String extension) {
        String mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (TextUtils.isEmpty(mime)) {
            mime = innerMimeMap.get(extension);
        }
        return mime;
    }
}

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

import android.graphics.drawable.Drawable;

import java.io.Serializable;

public class MediaItem implements Serializable {


    public static enum MediaType {
        Folder, File
    }

    public static enum SubMediaType{
        File,Apk,Image,Video
    }

    public String path;
    public String title;
    public MediaType type;
    public long length;
    public int childCount;
    public long lastModify;
    public SubMediaType subType;
    public transient Drawable icon;
    public transient Object tag;

    @Override
    public String toString() {
        return title;
    }
}

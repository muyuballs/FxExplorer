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

package info.breezes.fxmanager.service.ftp;

import org.mockftpserver.fake.filesystem.AbstractFileSystemEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystemEntry;

import info.breezes.fxapi.MediaItem;
import info.breezes.fxapi.MediaProvider;


public class FxFileEntity extends AbstractFileSystemEntry {

    private final MediaItem mediaItem;
    private final MediaProvider mediaProvider;

    public FxFileEntity(MediaItem item, MediaProvider provider) {
        this.mediaItem = item;
        this.mediaProvider = provider;
        setPath(item.path);
    }

    @Override
    public boolean isDirectory() {
        return mediaItem.type == MediaItem.MediaType.Folder;
    }

    @Override
    public long getSize() {
        return mediaItem.length;
    }

    @Override
    public FileSystemEntry cloneWithNewPath(String path) {
        return null;
    }
}

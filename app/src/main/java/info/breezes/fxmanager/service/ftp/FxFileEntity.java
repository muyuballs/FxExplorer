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

import org.mockftpserver.fake.filesystem.FileEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import info.breezes.toolkit.log.Log;


public class FxFileEntity extends FileEntry {
    public FxFileEntity(String path) {
        super(path);
    }

    @Override
    public long getSize() {
        return new File(getPath()).length();
    }

    @Override
    public InputStream createInputStream() {
        Log.d(null, "createInputStream");
        try {
            return new FileInputStream(getPath());
        } catch (FileNotFoundException e) {
            return super.createInputStream();
        }
    }


}

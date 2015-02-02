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

import org.mockftpserver.fake.filesystem.FileSystemEntry;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import java.util.List;

public class ProviderBasedFileSystem  extends UnixFakeFileSystem {

    public ProviderBasedFileSystem() {
    }


    @Override
    public List listFiles(String path) {
        return null;
    }

    @Override
    public List listNames(String path) {
        return null;
    }

    @Override
    public boolean delete(String path) {
        return false;
    }

    @Override
    public void rename(String fromPath, String toPath) {

    }


    @Override
    public boolean exists(String path) {
        return false;
    }


    @Override
    public FileSystemEntry getEntry(String path) {
        return null;
    }

    @Override
    public String getParent(String path) {
        return null;
    }

}

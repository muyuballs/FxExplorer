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

import android.app.IntentService;
import android.content.Intent;

import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import info.breezes.fxmanager.NetUtils;
import info.breezes.fxmanager.service.FileService;
import info.breezes.toolkit.log.Log;


public class FtpFileService extends FileService {

    private static FakeFtpServer fakeFtpServer;

    public FtpFileService() {
        super("Ftp Service");
        if (fakeFtpServer == null) {
            fakeFtpServer = new FakeFtpServer();
            fakeFtpServer.setServerControlPort(0);
            fakeFtpServer.setFileSystem(new UnixFakeFileSystem());
            fakeFtpServer.start();
            Log.d(null, NetUtils.getLocalIpAddress(this) + ":" + fakeFtpServer.getServerControlPort());
        }
    }

    @Override
    protected void handleStop() {
        if (fakeFtpServer != null) {
            fakeFtpServer.stop();
        }
    }

    @Override
    protected void handleRemoveFile(String path) {

    }

    @Override
    protected void handleServeFile(String fs, String path, long timeout) {
        FileSystem fileSystem=fakeFtpServer.getFileSystem();
        String root="/";
        UserAccount userAccount=new UserAccount("","",root);
        FileEntry fileEntry=new FileEntry("");
    }

}

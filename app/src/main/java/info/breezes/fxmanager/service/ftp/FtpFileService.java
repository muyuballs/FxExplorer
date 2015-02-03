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

import org.mockftpserver.core.command.CommandNames;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import java.io.File;

import info.breezes.fxmanager.NetUtils;
import info.breezes.fxmanager.StringUtils;
import info.breezes.fxmanager.service.FileService;


public class FtpFileService extends FileService {

    private static FakeFtpServer fakeFtpServer;

    public FtpFileService() {
        super("Ftp Service");
        if (fakeFtpServer == null) {
            fakeFtpServer = new FakeFtpServer();
            fakeFtpServer.setServerControlPort(0);
            fakeFtpServer.setCommandHandler(CommandNames.RETR,new RetrCommandHandler());
            fakeFtpServer.setFileSystem(new UnixFakeFileSystem());
            fakeFtpServer.start();
            //Log.d(null, NetUtils.getLocalIpAddress(getApplicationContext()) + ":" + fakeFtpServer.getServerControlPort());
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
        FileSystem fileSystem = fakeFtpServer.getFileSystem();
        fileSystem.delete(path);
    }

    @Override
    protected void handleServeFile(String fs, String path, long timeout) {
        FileSystem fileSystem = fakeFtpServer.getFileSystem();
        String root = new File(path).getParent();
        String userName = StringUtils.randomString(8);
        String password = StringUtils.randomString(8);
        UserAccount userAccount = new UserAccount(userName, password, root);
        if (fileSystem.getEntry(root) == null) {
            DirectoryEntry directoryEntry = new DirectoryEntry(root);
            directoryEntry.setPermissionsFromString("r-xr-xr-x");
            fileSystem.add(directoryEntry);
        }
        if (fileSystem.getEntry(path) == null) {
            FxFileEntity fileEntry = new FxFileEntity(path);
            fileSystem.add(fileEntry);
        }
        fakeFtpServer.addUserAccount(userAccount);
        notifyServeApply("ftp", userName, password, NetUtils.getLocalIpAddress(this), fakeFtpServer.getServerControlPort(), path);
    }


}

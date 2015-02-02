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

package info.breezes.fxmanager.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Base64;

import net.gescobar.httpserver.Handler;
import net.gescobar.httpserver.HttpServer;
import net.gescobar.httpserver.Request;
import net.gescobar.httpserver.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import info.breezes.StreamUtils;
import info.breezes.fxmanager.MimeTypeMap;
import info.breezes.fxmanager.NetUtils;
import info.breezes.toolkit.log.Log;

public abstract class FileService extends IntentService{

    public static final String ACTION_STOP = "info.breezes.fxmanager.service.action.STOP";
    public static final String ACTION_REMOVE_FILE = "info.breezes.fxmanager.service.action.CLEAR";
    public static final String ACTION_ADD_FILE = "info.breezes.fxmanager.service.action.ADD_FILE";

    public static final String EXTRA_PATH = "info.breezes.fxmanager.service.extra.PATH";
    public static final String EXTRA_FS_PROVIDER = "info.breezes.fxmanager.service.extra.FS_PROVIDER";
    public static final String EXTRA_TIMEOUT = "info.breezes.fxmanager.service.extra.TIMEOUT";

    public static void stop(Context context) {
        Intent intent = new Intent(context, FileService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    public static void removeFile(Context context, String path) {
        Intent intent = new Intent(context, FileService.class);
        intent.setAction(ACTION_REMOVE_FILE);
        intent.putExtra(EXTRA_PATH, path);
        context.startService(intent);
    }

    public static String startServeFile(Context context, String path, String fs, long timeout) {
        Intent intent = new Intent(context, FileService.class);
        intent.setAction(ACTION_ADD_FILE);
        intent.putExtra(EXTRA_PATH, path);
        intent.putExtra(EXTRA_FS_PROVIDER, fs);
        intent.putExtra(EXTRA_TIMEOUT, timeout);
        context.startService(intent);
        return "";
    }

    public FileService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_ADD_FILE.equals(action)) {
                handleServeFile(intent.getStringExtra(EXTRA_FS_PROVIDER), intent.getStringExtra(EXTRA_PATH), intent.getLongExtra(EXTRA_TIMEOUT, 0));
            }  else if (ACTION_REMOVE_FILE.equals(action)) {
                handleRemoveFile(intent.getStringExtra(EXTRA_PATH));
            } else if (ACTION_STOP.equals(action)) {
                handleStop();
            }
        }
    }

    protected abstract void handleStop();
    protected abstract void handleRemoveFile(String path);
    protected abstract void handleServeFile(String fs, String path, long timeout);
}

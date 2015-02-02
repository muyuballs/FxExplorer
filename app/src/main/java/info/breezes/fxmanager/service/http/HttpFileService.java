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

package info.breezes.fxmanager.service.http;

import android.text.TextUtils;

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

import info.breezes.StreamUtils;
import info.breezes.fxmanager.MimeTypeMap;
import info.breezes.fxmanager.service.FileService;
import info.breezes.toolkit.log.Log;

public class HttpFileService extends FileService implements Handler {

    private static Timer timer = new Timer("TimeoutWatcher");
    private static HashMap<String, String> contextMap = new HashMap<>();

    private static HttpServer httpServer;

    public HttpFileService() {
        super("Http File Service");
    }

    @Override
    protected void handleStop() {
        if (httpServer != null) {
            httpServer.stop();
        }
    }

    @Override
    protected void handleRemoveFile(String path) {
        String context = null;
        for (String key : contextMap.keySet()) {
            if (contextMap.get(key).equals(path)) {
                context = key;
                break;
            }
        }
        if (context != null) {
            contextMap.remove(context);
        }
        checkState();
    }

    @Override
    protected void handleServeFile(String fs, final String path, long timeout) {
        contextMap.put(fs, path);
        setStopWatcher(timeout, path);
        startService();
    }


    private void setStopWatcher(long timeout, final String context) {
        if (timeout > 0) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (contextMap.containsKey(context)) {
                        contextMap.remove(context);
                    }
                    checkState();
                }
            }, timeout);
        }
    }

    private synchronized void startService() {
        if (httpServer == null) {
            httpServer = new HttpServer(10086);
            httpServer.setHandler(this);
            try {
                httpServer.start(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void handle(Request request, Response response) {
        if (contextMap.containsKey(request.getPath())) {
            String realPath = contextMap.get(request.getPath());
            File file = new File(realPath);
            if (file.isFile()) {
                serveFile(response, file);
            } else {
                response.status(Response.HttpStatus.UNAUTHORIZED);
            }
        }
    }

    private void serveFile(Response response, File file) {
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(file.getPath()));
        if (TextUtils.isEmpty(mimeType)) {
            mimeType = "application/octet-stream";
        }
        response = response.ok().contentType(mimeType);
        response.addHeader("Content-Disposition", "attachment;filename=" + file.getName());
        response.addHeader("Content-Length", String.valueOf(file.length()));
        FileInputStream fileInputStream = null;
        try {
            response.writeHeader();
            fileInputStream = new FileInputStream(file);
            byte[] buf = new byte[80960];
            int c;
            while ((c = fileInputStream.read(buf)) != -1) {
                response.getOutputStream().write(buf, 0, c);
            }
            response.finish();
        } catch (IOException e) {
            Log.e(null, e.getMessage(), e);
        } finally {
            StreamUtils.safeClose(fileInputStream);
        }
    }

    private synchronized void checkState() {
        if (contextMap.size() < 1) {
            httpServer.stop();
            httpServer = null;
        }
    }

}

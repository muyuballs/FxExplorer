package info.breezes.fxmanager.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import net.gescobar.httpserver.Handler;
import net.gescobar.httpserver.HttpServer;
import net.gescobar.httpserver.Request;
import net.gescobar.httpserver.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import info.breezes.DigestUtils;
import info.breezes.StreamUtils;
import info.breezes.fxmanager.NetUtils;
import info.breezes.toolkit.log.Log;

public class FileService extends IntentService implements Handler {

    private static final String ACTION_STOP_SERVE = "info.breezes.fxmanager.service.action.CLEAR";
    private static final String ACTION_SERVE_FILE = "info.breezes.fxmanager.service.action.SERVE_FILE";
    private static final String ACTION_SERVE_FOLDER = "info.breezes.fxmanager.service.action.SERVE_FOLDER";

    private static final String EXTRA_CONTEXT = "info.breezes.fxmanager.service.extra.CONTEXT";
    private static final String EXTRA_PATH = "info.breezes.fxmanager.service.extra.PATH";
    private static final String EXTRA_DIRS = "info.breezes.fxmanager.service.extra.DIRS";
    private static final String EXTRA_TIMEOUT = "info.breezes.fxmanager.service.extra.TIMEOUT";

    public static void stopServe(Context context, String path) {
        Intent intent = new Intent(context, FileService.class);
        intent.setAction(ACTION_STOP_SERVE);
        intent.putExtra(EXTRA_PATH, path);
        context.startService(intent);
    }

    public static String startServeFile(Context context, String path, long timeout) {
        Intent intent = new Intent(context, FileService.class);
        intent.setAction(ACTION_SERVE_FILE);
        intent.putExtra(EXTRA_PATH, path);
        intent.putExtra(EXTRA_TIMEOUT, timeout);
        intent.putExtra(EXTRA_CONTEXT, "/" + DigestUtils.sha1(path + System.nanoTime()));
        context.startService(intent);
        return String.format("http://%s:10086%s", NetUtils.getLocalIpAddress(context), intent.getStringExtra(EXTRA_CONTEXT));
    }


    public static String startServeFolder(Context context, String path, long timeout, boolean dirs) {
        Intent intent = new Intent(context, FileService.class);
        intent.setAction(ACTION_SERVE_FOLDER);
        intent.putExtra(EXTRA_PATH, path);
        intent.putExtra(EXTRA_DIRS, dirs);
        intent.putExtra(EXTRA_TIMEOUT, timeout);
        intent.putExtra(EXTRA_CONTEXT, "/" + DigestUtils.sha1(path + System.nanoTime()));
        context.startService(intent);
        return intent.getStringExtra(EXTRA_CONTEXT);
    }

    private Timer timer;
    private ArrayList<String> inService;
    private HashMap<String, String> contextMap;

    private HttpServer httpServer;

    public FileService() {
        super("FileService");
        timer = new Timer("TimeoutWatcher");
        inService = new ArrayList<>();
        contextMap = new HashMap<>();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SERVE_FILE.equals(action)) {
                handleServeFile(intent.getStringExtra(EXTRA_CONTEXT), intent.getStringExtra(EXTRA_PATH), intent.getLongExtra(EXTRA_TIMEOUT, 0));
            } else if (ACTION_SERVE_FOLDER.equals(action)) {
                handleServeFolder(intent.getStringExtra(EXTRA_PATH), intent.getLongExtra(EXTRA_TIMEOUT, 0), intent.getBooleanExtra(EXTRA_DIRS, false));
            } else if (ACTION_STOP_SERVE.equals(action)) {
                handleStopServe(intent.getStringExtra(EXTRA_PATH));
            }
        }
    }

    private void handleStopServe(String path) {
        inService.remove(path);
        checkState();
    }


    private void handleServeFile(String context, final String path, long timeout) {
        inService.add(path);
        contextMap.put(context, path);
        setStopWatcher(timeout, path);
        startService();
    }


    private void handleServeFolder(String path, long timeout, boolean dirs) {

    }

    private void setStopWatcher(long timeout, final String path) {
        if (timeout > 0) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    inService.remove(path);
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
            }, timeout);
        }
    }


    private synchronized void startService() {
        if (httpServer == null) {
            httpServer = new HttpServer(10086);
            httpServer.setHandler(this);
            try {
                httpServer.start(false);
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
                serveFile(request, response, file);
            } else if (file.isDirectory()) {
                serveFolder(request, response, file);
            } else {
                response.status(Response.HttpStatus.UNAUTHORIZED);
            }
        }
    }

    private void serveFile(Request request, Response response, File file) {
        response = response.ok().contentType("application/octet-stream");
        response.addHeader("Content-Disposition", "attachment;filename=" + file.getName());
        response.addHeader("Content-Length", String.valueOf(file.length()));
        FileInputStream fileInputStream = null;
        try {
            response.writeHeader();
            fileInputStream = new FileInputStream(file);
            byte[] buf = new byte[80960];
            int c = -1;
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

    private void serveFolder(Request request, Response response, File file) {

    }


    private synchronized void checkState() {
        if (inService.size() < 1) {
            httpServer.setRunning(false);
        }
    }

}

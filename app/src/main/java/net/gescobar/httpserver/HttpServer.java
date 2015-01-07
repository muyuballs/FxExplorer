package net.gescobar.httpserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import info.breezes.toolkit.log.Log;

public class HttpServer {

    /**
     * The default port to use unless other is specified.
     */
    private static final int DEFAULT_PORT = 3000;

    /**
     * The executor in which the requests are handled.
     */
    private ExecutorService executor = Executors.newFixedThreadPool(10);

    /**
     * Used to listen in the specified port.
     */
    private ServerSocket serverSocket;

    /**
     * The thread that is constantly accepting connections. It delegates them to a {@link HttpConnection}.
     */
    private ServerDaemon serverDaemon;

    /**
     * The port in which this server is going to listen.
     */
    private int port;

    /**
     * The implementation that will handle requests.
     */
    private Handler handler;

    /**
     * Tells if the server is running or not.
     */
    private boolean running;

    /**
     * Constructor. Initializes the server with the default port and {@link net.gescobar.httpserver.Handler} implementation.
     */
    public HttpServer() {
        this(DEFAULT_PORT);
    }

    /**
     * Constructor. Initializes the server with the specified port and default {@link net.gescobar.httpserver.Handler} implementation.
     *
     * @param port the port in which the server should listen.
     */
    public HttpServer(int port) {
        this(new Handler() {

            @Override
            public void handle(Request request, Response response) {
            }

        }, port);
    }

    /**
     * Constructor. Initializes the server with the specified {@link net.gescobar.httpserver.Handler} implementation and default port.
     *
     * @param handler the {@link net.gescobar.httpserver.Handler} implementation that will handle the requests.
     */
    public HttpServer(Handler handler) {
        this(handler, DEFAULT_PORT);
    }

    /**
     * Constructor. Initializes the server with the specified port and {@link net.gescobar.httpserver.Handler} implementation.
     *
     * @param handler the {@link net.gescobar.httpserver.Handler} implementation that will handle the requests.
     * @param port    the port in which the server should listen.
     */
    public HttpServer(Handler handler, int port) {
        this.port = port;
        this.handler = handler;
    }

    /**
     * Starts the HTTP Server.
     *
     * @return itself for method chaining
     * @throws java.io.IOException if an error occurs in the underlying connection.
     */
    public HttpServer start(boolean background) throws IOException {

        if (running) {
            Log.w(null, "HTTP Server already running ... ");
            return this;
        }

        Log.d(null, "starting the HTTP Server ... ");

        running = true;

        serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout(0);

        serverDaemon = new ServerDaemon();
        if (background) {
            serverDaemon.start();
        } else {
            serverDaemon.run();
        }
        Log.i(null, "<< HTTP Server running on port " + port + " >>");

        return this;

    }

    /**
     * Stops the server gracefully.
     */
    public void stop() {

        // signals the daemon thread to stop the loop and terminate the execution.
        Log.d(null, "stopping the HTTP Server ... ");
        running = false;

        // wait until it actually stops
        try {
            serverDaemon.join();
        } catch (InterruptedException e) {
        }
    }

    /**
     * This is the main thread listening for new connections and delegating them to {@link HttpConnection} objects.
     *
     * @author German Escobar
     */
    private class ServerDaemon extends Thread {

        @Override
        public void run() {

            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    // this is what actually process the request
                    executor.execute(new HttpConnection(socket, handler));

                } catch (SocketTimeoutException e) {
                    // no connection received this time
                } catch (IOException e) {
                    Log.e(null, "IOException while accepting connection: " + e.getMessage(), e);
                }
            }
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(null, "IOException while stopping server: " + e.getMessage(), e);
            }
            Log.e(null, "daemon thread is exiting ...");
        }
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

}
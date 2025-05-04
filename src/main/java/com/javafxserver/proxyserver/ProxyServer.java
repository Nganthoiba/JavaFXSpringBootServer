package com.javafxserver.proxyserver;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

import com.javafxserver.config.Config;
import com.javafxserver.utils.PropertyReader;

public class ProxyServer {
    private static volatile boolean isRunning = true;
    private static ServerSocket serverSocket;
    private static ExecutorService threadPool;
    private static final int THREAD_POOL_SIZE = 10;
    private static Thread acceptThread;

    public static void start() throws Exception {
        //int proxyPort = Config.PROXY_SERVER_PORT;
    	PropertyReader propertyReader = new PropertyReader();
    	int proxyPort = Integer.parseInt(propertyReader.getProperty("server.proxy.port", "8081"));
    	
        serverSocket = new ServerSocket(proxyPort);
        threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        log("Proxy server started on port " + proxyPort);

        acceptThread = new Thread(() -> {
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.execute(() -> {
                        try {
                            handleClientSocket(clientSocket);
                        } catch (IOException | URISyntaxException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (IOException e) {
                    if (isRunning) {
                        e.printStackTrace();
                    }
                }
            }
        });

        acceptThread.start();
    }

    public static void stop() throws IOException {
        isRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
            log("Server socket closed gracefully.");
        }

        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            /*
            try {
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                    if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                        log("Thread pool did not terminate.");
                    }
                }
            } catch (InterruptedException ie) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
            */
            log("Thread pool shut down.");
        }

        try {
            if (acceptThread != null) {
                acceptThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log("Proxy Server stopped.");
    }

    private static void handleClientSocket(Socket clientSocket) throws IOException, URISyntaxException {
        try (
            InputStream clientInputStream = clientSocket.getInputStream();
            OutputStream clientOutputStream = clientSocket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientInputStream));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientOutputStream))
        ) {
            String incomingLine = reader.readLine();
            if (incomingLine == null) {
                return;
            }

            String[] requestParts = incomingLine.split(" ");
            if (requestParts.length < 3) {
                return;
            }

            String method = requestParts[0];
            String path = requestParts[1];
            String httpVersion = requestParts[2];

            log("Incoming request: " + method + " "+httpVersion+":" + path);

            String origin = null;
            int contentLength = 0;

            // Read headers
            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                String[] headerParts = headerLine.split(":", 2);
                if (headerParts.length == 2) {
                    String headerName = headerParts[0].trim();
                    String headerValue = headerParts[1].trim();
                    if ("Origin".equalsIgnoreCase(headerName)) {
                        origin = headerValue;
                    } else if ("Content-Length".equalsIgnoreCase(headerName)) {
                        contentLength = Integer.parseInt(headerValue);
                    }
                }
            }

            // Handle OPTIONS preflight request
            if ("OPTIONS".equalsIgnoreCase(method)) {    
            	writer.write(httpVersion + " 204 No Content\r\n"); // Status first
            	writer.write("Access-Control-Allow-Origin: " + (origin != null ? origin : "*") + "\r\n");
            	writer.write("Access-Control-Allow-Credentials: true\r\n");
            	writer.write("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n");
            	writer.write("Access-Control-Allow-Headers: Content-Type, Authorization\r\n");
            	writer.write("Vary: Origin\r\n");
            	writer.write("\r\n");
            	writer.flush();                
                return;
            }

            // Forward the request to the MiniServer
            String miniServerUrl = "http://localhost:" + Config.getHttpPort() + path;
            URI uri = new URI(miniServerUrl);
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);

            // Forward headers
            if (origin != null) {
                connection.setRequestProperty("Origin", origin);
            }

            // Forward body to the target mini server if present
            /*
            if (contentLength > 0) {
                connection.setDoOutput(true);
                char[] buffer = new char[1024];
                int totalRead = 0;
                try (OutputStream os = connection.getOutputStream()) {
                    while (totalRead < contentLength) {
                        int read = reader.read(buffer, 0, Math.min(buffer.length, contentLength - totalRead));
                        if (read == -1) break;
                        os.write(new String(buffer, 0, read).getBytes());
                        totalRead += read;
                    }
                    os.flush();
                }
            }
			*/
            if(contentLength > 0) {
            	connection.setDoOutput(true);
	            try (OutputStream os = connection.getOutputStream()) {
	                byte[] buffer = new byte[1024];
	                int totalRead = 0;
	                int read;
	                while (totalRead < contentLength && (read = clientInputStream.read(buffer)) != -1) {
	                    os.write(buffer, 0, read);
	                    totalRead += read;
	                }
	                os.flush();
	            }
            }
            
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();

            writer.write(httpVersion + " " + responseCode + " " + responseMessage + "\r\n");

            if (origin != null) {
                writer.write("Access-Control-Allow-Origin: " + origin + "\r\n");
                writer.write("Access-Control-Allow-Credentials: true\r\n");
                writer.write("Vary: Origin\r\n");
            } else {
                writer.write("Access-Control-Allow-Origin: *\r\n");
            }

            // Forward response headers
            connection.getHeaderFields().forEach((key, values) -> {
                if (key != null) {
                    try {
                        writer.write(key + ": " + String.join(", ", values) + "\r\n");
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                        log("Error in writing headers: " + ioe.getMessage());
                    }
                }
            });
            writer.write("\r\n");
            writer.flush();

            // Forward response body
            InputStream responseStream;
            try {
                responseStream = connection.getInputStream();
            } catch (IOException e) {
                responseStream = connection.getErrorStream();
            }

            if (responseStream != null) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = responseStream.read(buffer)) != -1) {
                    clientOutputStream.write(buffer, 0, bytesRead);
                }
                responseStream.close();
            }
            clientOutputStream.flush();
        } finally {
            clientSocket.close();
        }
    }

    private static void log(String logMessage) {
        String logFilePath = Config.APP_PATH + File.separator + "ProxyServerLog.log";
        File logFile = new File(logFilePath);
        try {
            logFile.createNewFile();
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write("[" + java.time.LocalDateTime.now() + "] " + logMessage + "\n");
            }
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the log file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SimpleWebServer {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "password123";
    private static final String SESSION_COOKIE_NAME = "sessionId";
    private static final Map<String, String> activeSessions = new HashMap<>();
    private static final int PORT = 8080;
    private static final String PUBLIC_DIR = "public";
    private static final int N_THREADS = 3;
    private final ExecutorService executor;
    private volatile boolean running = true;

    public SimpleWebServer() {
        this.executor = Executors.newFixedThreadPool(N_THREADS);
    }

    public static void main(String[] args) {
        try {
            new SimpleWebServer().start();
        } catch (IOException e) {
            System.err.println("Server Error: " + e.getMessage());
        }
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Web server listening on port " + PORT + " (press CTRL-C to quit)");

        while (running) {
            Socket clientSocket = serverSocket.accept();
            executor.submit(() -> {
                try {
                    handleRequest(clientSocket);
                } catch (IOException e) {
                    System.err.println("Error handling request: " + e.getMessage());
                }
            });
        }
    }

    private void handleRequest(Socket client) throws IOException {
        try (OutputStream out = client.getOutputStream();
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))) {

            HttpRequest request = new HttpRequest(client.getInputStream());
            HttpResponse response = new HttpResponse();

            // Handle different HTTP methods
            if ("POST".equals(request.getMethod())) {
                if (request.getPath().startsWith("/admin")) {
                    handleAdminRequest(request, response);
                } else {
                    sendError(response, 405, "Method Not Allowed");
                }
            } else if ("GET".equals(request.getMethod())) {
                handleGet(request, response);
            } else {
                sendError(response, 405, "Method Not Allowed");
            }
            
            response.send(out);

        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                // Ignore close errors
            }
        }
    }

    private void handleAdminRequest(HttpRequest request, HttpResponse response) throws IOException {
        String path = request.getPath();
        Cookie sessionCookie = request.getCookie(SESSION_COOKIE_NAME);
        String sessionId = sessionCookie != null ? sessionCookie.getValue() : null;
        boolean isAuthenticated = sessionId != null && activeSessions.containsKey(sessionId);

        if (path.equals("/admin/login") && request.getMethod().equals("POST")) {
            String username = request.getFormParam("username");
            String password = request.getFormParam("password");

            if (ADMIN_USERNAME.equals(username) && ADMIN_PASSWORD.equals(password)) {
                sessionId = UUID.randomUUID().toString();
                activeSessions.put(sessionId, username);

                // Add session cookie
                Cookie newSessionCookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
                newSessionCookie.setMaxAge(3600); // 1 hour
                response.addCookie(newSessionCookie);

                // Add user info cookies
                Cookie usernameCookie = new Cookie("username", username);
                usernameCookie.setMaxAge(3600);
                response.addCookie(usernameCookie);

                Cookie lastLoginCookie = new Cookie("lastLogin", new Date().toString());
                lastLoginCookie.setMaxAge(3600);
                response.addCookie(lastLoginCookie);

                Cookie roleCookie = new Cookie("userRole", "administrator");
                roleCookie.setMaxAge(3600);
                response.addCookie(roleCookie);

                response.setStatus(302, "Found");
                response.addHeader("Location", "/admin/dashboard.html");
                return;
            } else {
                response.setStatus(401, "Unauthorized");
                response.setBody("Invalid credentials");
                return;
            }
        }

        if (path.equals("/admin/logout")) {
            if (sessionId != null) {
                activeSessions.remove(sessionId);
                Cookie expiredCookie = new Cookie(SESSION_COOKIE_NAME, "");
                expiredCookie.setMaxAge(0);
                response.addCookie(expiredCookie);
            }
            response.setStatus(302, "Found");
            response.addHeader("Location", "/admin/login.html");
            return;
        }

        // Check authentication for all other admin routes
        if (!isAuthenticated && !path.equals("/admin/login.html")) {
            response.setStatus(302, "Found");
            response.addHeader("Location", "/admin/login.html");
            return;
        }

        // Serve admin static files
        File file = new File("public" + path);
        if (file.exists() && file.isFile()) {
            serveFile(file, response);
        } else {
            response.setStatus(404, "Not Found");
            response.setBody("Page not found");
        }
    }

    private void handleGet(HttpRequest request, HttpResponse response) throws IOException {
        String path = request.getPath();

        // Handle admin routes
        if (path.startsWith("/admin")) {
            handleAdminRequest(request, response);
            return;
        }

        // Handle test running
        if (path.equals("/run-tests")) {
            HttpTest.main(new String[0]);
            response.setStatus(302, "Found");
            response.addHeader("Location", "/test-results.html");
            return;
        }

        // Handle file downloads
        if (path.startsWith("/downloads/")) {
            File file = new File("public" + path);
            if (file.exists() && file.isFile()) {
                response.setAsAttachment(file.getName());
                serveFile(file, response);
            } else {
                response.setStatus(404, "Not Found");
                response.setBody("File not found");
            }
            return;
        }

        // Handle regular file requests
        if (path.equals("/")) {
            path = "/index.html";
        }

        File file = new File("public" + path);
        if (file.exists() && file.isFile()) {
            serveFile(file, response);
        } else {
            response.setStatus(404, "Not Found");
            response.setBody("Page not found");
        }
    }

    private void sendError(HttpResponse response, int code, String message) {
        response.setStatus(code, message);
        response.setHeader("Content-Type", "text/html");
        response.setBody(String.format("<html><body><h1>%d %s</h1></body></html>", code, message));
    }

    private String getContentType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".js")) return "application/javascript";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }

    private void serveFile(File file, HttpResponse response) throws IOException {
        String contentType = getContentType(file.getName());
        byte[] content = Files.readAllBytes(file.toPath());

        response.setStatus(200, "OK");
        response.setHeader("Content-Type", contentType);
        response.setHeader("Last-Modified", new Date(file.lastModified()).toString());
        response.setBody(content);
    }
}

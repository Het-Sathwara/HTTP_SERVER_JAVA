import java.io.BufferedReader;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class HttpRequest {
    private String method;
    private String path;
    private String version;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private Map<String, String> formParams;
    private Map<String, Cookie> cookies;
    private String body;
    private static final int MAX_BODY_SIZE = 1024 * 1024; // 1MB

    public HttpRequest(InputStream in) throws IOException {
        headers = new HashMap<>();
        queryParams = new HashMap<>();
        formParams = new HashMap<>();
        cookies = new HashMap<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        // Read request line
        String requestLine = reader.readLine();
        if (requestLine == null) throw new IOException("Empty request");
        parseRequestLine(requestLine);

        // Read headers
        String line;
        int contentLength = 0;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            parseHeader(line);
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.substring(16).trim());
                if (contentLength > MAX_BODY_SIZE) {
                    throw new IOException("Request body too large");
                }
            }
        }

        // Parse cookies if present
        String cookieHeader = headers.get("Cookie");
        if (cookieHeader != null) {
            parseCookies(cookieHeader);
        }

        // Read body if present
        if (contentLength > 0) {
            char[] bodyChars = new char[contentLength];
            reader.read(bodyChars, 0, contentLength);
            body = new String(bodyChars);

            // Parse form data if content type is application/x-www-form-urlencoded
            String contentType = headers.get("Content-Type");
            if (contentType != null && contentType.toLowerCase().contains("application/x-www-form-urlencoded")) {
                parseQueryString(body, formParams);
            }
        }
    }

    private void parseRequestLine(String requestLine) throws IOException {
        String[] parts = requestLine.split(" ");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid request line: " + requestLine);
        }
        method = parts[0].toUpperCase();
        path = URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name());
        version = parts[2];

        // Parse query parameters if present
        int queryStart = path.indexOf('?');
        if (queryStart >= 0) {
            String query = path.substring(queryStart + 1);
            path = path.substring(0, queryStart);
            parseQueryString(query);
        }
    }

    private void parseHeader(String headerLine) {
        int colonPos = headerLine.indexOf(':');
        if (colonPos > 0) {
            String key = headerLine.substring(0, colonPos).trim();
            String value = headerLine.substring(colonPos + 1).trim();
            headers.put(key, value);
        }
    }

    private void parseQueryString(String query) throws IOException {
        parseQueryString(query, queryParams);
    }

    private void parseQueryString(String query, Map<String, String> params) throws IOException {
        if (query == null || query.isEmpty()) return;
        
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int equals = pair.indexOf('=');
            if (equals > 0) {
                String key = URLDecoder.decode(pair.substring(0, equals), StandardCharsets.UTF_8.name());
                String value = URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8.name());
                params.put(key, value);
            }
        }
    }

    private void parseCookies(String cookieHeader) {
        String[] cookiePairs = cookieHeader.split(";");
        for (String pair : cookiePairs) {
            String[] parts = pair.trim().split("=");
            if (parts.length == 2) {
                cookies.put(parts[0], new Cookie(parts[0], parts[1]));
            }
        }
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public Cookie getCookie(String name) {
        return cookies.get(name);
    }

    public String getQueryParam(String name) {
        return queryParams.get(name);
    }

    public String getFormParam(String name) {
        return formParams.get(name);
    }
}

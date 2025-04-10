import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpResponse {
    private int statusCode;
    private String statusMessage;
    private Map<String, String> headers;
    private List<Cookie> cookies;
    private byte[] body;
    private boolean isAttachment;
    private String filename;

    public HttpResponse() {
        headers = new HashMap<>();
        cookies = new ArrayList<>();
        isAttachment = false;
        filename = null;
        setHeader("Server", "SimpleJavaWebServer");
        setHeader("Connection", "close");
    }

    public void setStatus(int code, String message) {
        this.statusCode = code;
        this.statusMessage = message;
    }

    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public void setBody(byte[] body) {
        this.body = body;
        setHeader("Content-Length", String.valueOf(body.length));
    }

    public void setBody(String body) {
        setBody(body.getBytes());
    }

    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    public void setAsAttachment(String filename) {
        this.isAttachment = true;
        this.filename = filename;
    }

    public void send(OutputStream out) throws IOException {
        // Write status line
        String statusLine = String.format("HTTP/1.1 %d %s\r\n", statusCode, statusMessage);
        out.write(statusLine.getBytes());

        // Set content disposition for attachments
        if (isAttachment && filename != null) {
            setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
        }

        // Write headers
        for (Map.Entry<String, String> header : headers.entrySet()) {
            String headerLine = String.format("%s: %s\r\n", header.getKey(), header.getValue());
            out.write(headerLine.getBytes());
        }

        // Write cookies
        for (Cookie cookie : cookies) {
            String cookieLine = String.format("Set-Cookie: %s\r\n", cookie.toHeaderString());
            out.write(cookieLine.getBytes());
        }

        // Write blank line
        out.write("\r\n".getBytes());

        // Write body
        if (body != null) {
            out.write(body);
        }

        out.flush();
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        send(baos);
        return baos.toByteArray();
    }
}

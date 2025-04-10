import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class HttpTest {
    private static List<TestResult> results = new ArrayList<>();
    private static int totalTests = 0;
    private static int passedTests = 0;

    private static final String TEST_RESULTS_HTML = ""
        + "<!DOCTYPE html>\n"
        + "<html>\n"
        + "<head>\n"
        + "    <title>HTTP Server Test Results</title>\n"
        + "    <style>\n"
        + "        body { font-family: Arial, sans-serif; margin: 40px; }\n"
        + "        .summary { margin-bottom: 20px; }\n"
        + "        .test-result { margin: 10px 0; padding: 10px; border-radius: 4px; }\n"
        + "        .pass { background-color: #dff0d8; }\n"
        + "        .fail { background-color: #f2dede; }\n"
        + "        .button { display: inline-block; padding: 10px 20px; background-color: #4CAF50;\n"
        + "                 color: white; text-decoration: none; border-radius: 4px; margin-top: 20px; }\n"
        + "        #cookieInfo { margin-top: 15px; padding: 15px; border: 1px solid #ddd;\n"
        + "                     border-radius: 4px; background: white; display: none; }\n"
        + "        .cookie-item { padding: 8px; margin: 5px 0; background: #f9f9f9; border-radius: 4px; }\n"
        + "    </style>\n"
        + "</head>\n"
        + "<body>\n"
        + "    <h1>HTTP Server Test Results</h1>\n";

    private static final String TEST_RESULTS_SCRIPT = ""
        + "    <button class='button' onclick='showCookies()'>Show Current Cookies</button>\n"
        + "    <div id='cookieInfo'></div>\n"
        + "    <script>\n"
        + "        function showCookies() {\n"
        + "            const cookieInfo = document.getElementById('cookieInfo');\n"
        + "            cookieInfo.style.display = 'block';\n"
        + "            const cookies = document.cookie.split(';');\n"
        + "            let cookieHtml = '<h3>Current Cookies:</h3>';\n"
        + "            if (cookies.length === 0 || (cookies.length === 1 && cookies[0].trim() === '')) {\n"
        + "                cookieHtml += '<p>No cookies found</p>';\n"
        + "            } else {\n"
        + "                cookies.forEach(cookie => {\n"
        + "                    const [name, value] = cookie.split('=').map(c => c.trim());\n"
        + "                    cookieHtml += `<div class=\"cookie-item\"><strong>${name}:</strong> ${value}</div>`;\n"
        + "                });\n"
        + "            }\n"
        + "            cookieInfo.innerHTML = cookieHtml;\n"
        + "        }\n"
        + "    </script>\n"
        + "</body>\n"
        + "</html>";

    public static void main(String[] args) {
        List<TestResult> results = new ArrayList<>();

        test("Parse GET request", () -> {
            String rawRequest = 
                "GET /index.html HTTP/1.1\r\n" +
                "Host: localhost:8080\r\n" +
                "User-Agent: Mozilla/5.0\r\n" +
                "\r\n";
            
            HttpRequest request = new HttpRequest(new ByteArrayInputStream(rawRequest.getBytes()));
            
            assert "GET".equals(request.getMethod()) : "Method should be GET";
            assert "/index.html".equals(request.getPath()) : "Path should be /index.html";
            assert "HTTP/1.1".equals(request.getVersion()) : "Version should be HTTP/1.1";
            assert "Mozilla/5.0".equals(request.getHeaders().get("User-Agent")) : "User-Agent header mismatch";
        });

        test("Parse POST request with body", () -> {
            String rawRequest = 
                "POST /submit HTTP/1.1\r\n" +
                "Content-Length: 11\r\n" +
                "\r\n" +
                "Hello World";
            
            HttpRequest request = new HttpRequest(new ByteArrayInputStream(rawRequest.getBytes()));
            
            assert "POST".equals(request.getMethod()) : "Method should be POST";
            assert "/submit".equals(request.getPath()) : "Path should be /submit";
            assert "Hello World".equals(request.getBody()) : "Body mismatch";
        });

        test("Parse request with cookies", () -> {
            String rawRequest = "GET /index.html HTTP/1.1\r\n" +
                "Cookie: sessionId=abc123; user=john\r\n" +
                "\r\n";
            
            HttpRequest request = new HttpRequest(new ByteArrayInputStream(rawRequest.getBytes()));
            
            Cookie sessionCookie = request.getCookie("sessionId");
            Cookie userCookie = request.getCookie("user");
            
            assert sessionCookie != null : "Session cookie not found";
            assert "abc123".equals(sessionCookie.getValue()) : "Session cookie value mismatch";
            assert userCookie != null : "User cookie not found";
            assert "john".equals(userCookie.getValue()) : "User cookie value mismatch";
        });

        test("Parse request with query parameters", () -> {
            String rawRequest = "GET /search?q=test&page=1 HTTP/1.1\r\n\r\n";
            
            HttpRequest request = new HttpRequest(new ByteArrayInputStream(rawRequest.getBytes()));
            
            assert "/search".equals(request.getPath()) : "Path should be /search";
            assert "test".equals(request.getQueryParam("q")) : "Query parameter 'q' mismatch";
            assert "1".equals(request.getQueryParam("page")) : "Query parameter 'page' mismatch";
        });

        test("Generate 200 OK response", () -> {
            HttpResponse response = new HttpResponse();
            response.setStatus(200, "OK");
            response.setHeader("Content-Type", "text/plain");
            response.setBody("Hello World");
            
            byte[] responseBytes = response.toByteArray();
            String responseStr = new String(responseBytes);
            
            assert responseStr.startsWith("HTTP/1.1 200 OK\r\n") : "Status line mismatch";
            assert responseStr.contains("Content-Type: text/plain\r\n") : "Content-Type header missing";
            assert responseStr.contains("Hello World") : "Body content mismatch";
        });

        test("Generate 404 Not Found response", () -> {
            HttpResponse response = new HttpResponse();
            response.setStatus(404, "Not Found");
            response.setHeader("Content-Type", "text/html");
            response.setBody("<!DOCTYPE html><html><body><h1>404 Not Found</h1></body></html>");
            
            byte[] responseBytes = response.toByteArray();
            String responseStr = new String(responseBytes);
            
            assert responseStr.startsWith("HTTP/1.1 404 Not Found\r\n") : "Status line mismatch";
            assert responseStr.contains("Content-Type: text/html\r\n") : "Content-Type header missing";
            assert responseStr.contains("404 Not Found") : "Body content mismatch";
        });

        generateTestReport();
    }

    private static void test(String name, TestCase testCase) {
        totalTests++;
        try {
            testCase.run();
            passedTests++;
            results.add(new TestResult(name, true, null));
        } catch (AssertionError | Exception e) {
            results.add(new TestResult(name, false, e.getMessage()));
        }
    }

    private static void generateTestReport() {
        StringBuilder html = new StringBuilder(TEST_RESULTS_HTML);
        html.append(String.format("<div class='summary'>Total Tests: %d<br>Passed: %d<br>Failed: %d</div>\n",
                totalTests, passedTests, totalTests - passedTests));

        for (TestResult result : results) {
            html.append(String.format("<div class='test-result %s'>\n", result.passed ? "pass" : "fail"));
            html.append(String.format("<strong>%s:</strong> %s\n", result.name, result.passed ? "PASS" : "FAIL"));
            if (!result.passed && result.message != null) {
                html.append(String.format("<br><pre>%s</pre>\n", result.message));
            }
            html.append("</div>\n");
        }

        html.append("</body>\n</html>");

        // Save test results
        try {
            java.nio.file.Path testResultsPath = java.nio.file.Paths.get("public/test-results.html");
            java.nio.file.Files.createDirectories(testResultsPath.getParent());
            java.nio.file.Files.write(
                testResultsPath,
                html.toString().getBytes()
            );
        } catch (IOException e) {
            System.err.println("Error saving test results: " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface TestCase {
        void run() throws Exception;
    }

    private static class TestResult {
        String name;
        boolean passed;
        String message;

        TestResult(String name, boolean passed, String message) {
            this.name = name;
            this.passed = passed;
            this.message = message;
        }
    }
}

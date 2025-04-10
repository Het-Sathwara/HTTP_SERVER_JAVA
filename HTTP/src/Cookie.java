public class Cookie {
    private String name;
    private String value;
    private int maxAge = -1;
    private String path = "/";
    private boolean httpOnly = false;

    public Cookie(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() { return name; }
    public String getValue() { return value; }
    public int getMaxAge() { return maxAge; }
    public String getPath() { return path; }
    public boolean isHttpOnly() { return httpOnly; }

    public void setMaxAge(int maxAge) { this.maxAge = maxAge; }
    public void setPath(String path) { this.path = path; }
    public void setHttpOnly(boolean httpOnly) { this.httpOnly = httpOnly; }

    public String toHeaderString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value);
        if (maxAge >= 0) sb.append("; Max-Age=").append(maxAge);
        if (path != null) sb.append("; Path=").append(path);
        if (httpOnly) sb.append("; HttpOnly");
        return sb.toString();
    }

    public static Cookie parse(String cookieStr) {
        String[] parts = cookieStr.split(";")[0].split("=", 2);
        if (parts.length == 2) {
            return new Cookie(parts[0].trim(), parts[1].trim());
        }
        return null;
    }
}

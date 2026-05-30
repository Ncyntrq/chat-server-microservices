package network;

/**
 * Giữ JWT access token + username sau khi đăng nhập.
 * Singleton — toàn bộ Swing app dùng chung.
 */
public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private volatile String accessToken;
    private volatile String refreshToken;
    private volatile String username;

    private SessionManager() {}

    public static SessionManager get() {
        return INSTANCE;
    }

    public synchronized void setSession(String accessToken, String refreshToken, String username) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.username = username;
    }

    public void clear() {
        setSession(null, null, null);
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getUsername() { return username; }

    public boolean isAuthenticated() {
        return accessToken != null && !accessToken.isBlank();
    }
}

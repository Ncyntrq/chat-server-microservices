package network;

/**
 * Cấu hình endpoint kết nối tới Backend (đi qua API Gateway).
 * Có thể override bằng system property:
 *   -Dchatsever.gateway.http=http://host:port
 *   -Dchatsever.gateway.ws=ws://host:port
 */
public final class ApiConfig {

    public static final String GATEWAY_HTTP =
            System.getProperty("chatsever.gateway.http", "http://localhost:8080");

    public static final String GATEWAY_WS =
            System.getProperty("chatsever.gateway.ws", "ws://localhost:8080");

    public static final String WS_CHAT_PATH = "/ws/chat";

    // Default channel/server khi user chưa có UI chọn server
    public static final long DEFAULT_SERVER_ID =
            Long.parseLong(System.getProperty("chatsever.default.serverId", "1"));
    public static final long DEFAULT_CHANNEL_ID =
            Long.parseLong(System.getProperty("chatsever.default.channelId", "1"));

    private ApiConfig() {}
}

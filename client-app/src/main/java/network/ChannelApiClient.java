package network;

import com.chatsever.common.dto.MessageDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * REST client để lấy lịch sử tin nhắn của 1 channel.
 * Endpoint: GET /api/channels/{channelId}/messages?limit=50&before={id}
 *
 * Gateway dùng JWT từ Authorization header → inject X-User-Id.
 */
public class ChannelApiClient {

    private final ObjectMapper json = JsonMapper.get();

    /** Lấy `limit` tin nhắn gần nhất (theo id DESC). Trả về list đã đảo ngược (cũ → mới) để render trực tiếp. */
    public List<MessageDTO> fetchRecentMessages(long channelId, int limit) {
        String url = ApiConfig.GATEWAY_HTTP + "/api/channels/" + channelId + "/messages?limit=" + limit;
        String token = SessionManager.get().getAccessToken();
        if (token == null) {
            throw new ApiException("Chưa đăng nhập — không có token");
        }

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> resp = HttpClientHolder.get().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new ApiException(resp.statusCode(),
                        "Không lấy được lịch sử kênh " + channelId + ": HTTP " + resp.statusCode());
            }

            // BE trả ChatMessage entity; cấu trúc tương thích với MessageDTO (sender, content, channelId, serverId, timestamp, type, isEdited, id→messageId)
            List<RawChatMessage> raw = json.readValue(resp.body(), new TypeReference<List<RawChatMessage>>() {});
            // Đảo ngược: BE order DESC theo id → ta render cũ trước (ASC)
            Collections.reverse(raw);
            return raw.stream().map(RawChatMessage::toDto).toList();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Lỗi gọi " + url + ": " + e.getMessage(), e);
        }
    }
}

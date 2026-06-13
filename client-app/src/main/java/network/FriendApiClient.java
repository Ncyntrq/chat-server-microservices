package network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class FriendApiClient {

    private final ObjectMapper json = JsonMapper.get();

    // Lấy danh sách bạn bè
    public List<String> getFriends() {
        return getList("/api/friends");
    }

    // Lấy danh sách yêu cầu đang chờ
    public List<String> getPendingRequests() {
        return getList("/api/friends/pending");
    }

    // Gửi yêu cầu kết bạn
    public void sendRequest(String targetUsername) {
        postJson("/api/friends/request", Map.of("targetUsername", targetUsername));
    }

    // Chấp nhận kết bạn
    public void acceptRequest(String targetUsername) {
        postJson("/api/friends/accept", Map.of("targetUsername", targetUsername));
    }

    // Từ chối / Xóa bạn
    public void rejectOrRemoveFriend(String targetUsername) {
        postJson("/api/friends/reject", Map.of("targetUsername", targetUsername));
    }

    // Chặn
    public void blockUser(String targetUsername) {
        postJson("/api/friends/block", Map.of("targetUsername", targetUsername));
    }

    // Bỏ chặn
    public void unblockUser(String targetUsername) {
        postJson("/api/friends/unblock", Map.of("targetUsername", targetUsername));
    }

    // Lấy danh sách chặn
    public List<String> getBlockedUsers() {
        return getList("/api/friends/blocked");
    }

    @SuppressWarnings("unchecked")
    private List<String> getList(String path) {
        String url = ApiConfig.GATEWAY_HTTP + path;
        String token = SessionManager.get().getAccessToken();
        if (token == null) throw new ApiException("Chưa đăng nhập");
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> resp = HttpClientHolder.get().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new ApiException(parseErrorMessage(resp.body()));
            }
            return json.readValue(resp.body(), new TypeReference<List<String>>() {});
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Lỗi gọi " + url + ": " + e.getMessage(), e);
        }
    }

    private void postJson(String path, Object body) {
        String url = ApiConfig.GATEWAY_HTTP + path;
        String token = SessionManager.get().getAccessToken();
        if (token == null) throw new ApiException("Chưa đăng nhập");
        try {
            String payload = json.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> resp = HttpClientHolder.get().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new ApiException(parseErrorMessage(resp.body()));
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Lỗi gọi " + url + ": " + e.getMessage(), e);
        }
    }
    
    private String parseErrorMessage(String rawBody) {
        try {
            Map<String, Object> map = json.readValue(rawBody, new TypeReference<Map<String, Object>>() {});
            if (map.containsKey("message")) {
                return String.valueOf(map.get("message"));
            }
        } catch (Exception ignore) {}
        return rawBody;
    }
}

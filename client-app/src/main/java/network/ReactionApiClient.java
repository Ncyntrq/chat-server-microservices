package network;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class ReactionApiClient {

    public static boolean addReaction(Long messageId, String emoji) throws Exception {
        String token = SessionManager.get().getAccessToken();
        String encodedEmoji = URLEncoder.encode(emoji, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiConfig.GATEWAY_HTTP + "/api/messages/" + messageId + "/reactions/" + encodedEmoji))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        
        HttpResponse<String> response = HttpClientHolder.get().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.err.println("Add reaction failed: " + response.statusCode() + " " + response.body());
        }
        return response.statusCode() == 200;
    }

    public static boolean removeReaction(Long messageId, String emoji) throws Exception {
        String token = SessionManager.get().getAccessToken();
        String encodedEmoji = URLEncoder.encode(emoji, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiConfig.GATEWAY_HTTP + "/api/messages/" + messageId + "/reactions/" + encodedEmoji))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();
        
        HttpResponse<String> response = HttpClientHolder.get().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.err.println("Remove reaction failed: " + response.statusCode() + " " + response.body());
        }
        return response.statusCode() == 200;
    }
}

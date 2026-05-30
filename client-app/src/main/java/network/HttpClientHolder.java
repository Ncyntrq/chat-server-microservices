package network;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Singleton HttpClient (Java 11+). Tái sử dụng connection pool cho mọi REST call.
 */
public final class HttpClientHolder {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private HttpClientHolder() {}

    public static HttpClient get() {
        return CLIENT;
    }
}

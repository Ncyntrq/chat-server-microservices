package gui.utils;

import network.UserProfileApiClient;
import network.UserProfileCache;

import javax.swing.SwingWorker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Tải hồ sơ user theo username cho UI, với 2 tối ưu chống N+1:
 *  1) Cache ({@link UserProfileCache}) — hit thì trả ngay, không gọi HTTP.
 *  2) Gộp request đang bay (in-flight) — nhiều lời gọi cùng username (vd load 1000 tin nhắn
 *     của cùng người) chỉ tạo 1 HTTP; các callback còn lại xếp hàng chờ kết quả đó.
 *
 * <p>CHỈ gọi trên Event Dispatch Thread (Swing). {@code onDone} cũng chạy trên EDT.
 * {@code onDone} có thể nhận {@code null} nếu tải lỗi (caller tự bỏ qua).
 */
public final class ProfileLoader {

    // Truy cập hoàn toàn trên EDT (load + SwingWorker.done) ⇒ không cần đồng bộ hóa.
    private static final Map<String, List<Consumer<Map<String, Object>>>> inflight = new HashMap<>();

    private ProfileLoader() {}

    public static void load(String username, Consumer<Map<String, Object>> onDone) {
        if (username == null || username.isBlank() || onDone == null) return;

        Map<String, Object> cached = UserProfileCache.get(username);
        if (cached != null) {
            onDone.accept(cached);
            return;
        }

        List<Consumer<Map<String, Object>>> waiters = inflight.get(username);
        if (waiters != null) {
            waiters.add(onDone); // đã có request đang bay → chờ ké
            return;
        }
        waiters = new ArrayList<>();
        waiters.add(onDone);
        inflight.put(username, waiters);

        new SwingWorker<Map<String, Object>, Void>() {
            @Override protected Map<String, Object> doInBackground() {
                return new UserProfileApiClient().getProfile(username); // tự ghi cache khi thành công
            }
            @Override protected void done() {
                Map<String, Object> profile = null;
                try {
                    profile = get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception ignore) {
                    // lỗi mạng/parse → trả null cho waiters
                }
                List<Consumer<Map<String, Object>>> done = inflight.remove(username);
                if (done == null) return;
                for (Consumer<Map<String, Object>> w : done) {
                    w.accept(profile);
                }
            }
        }.execute();
    }
}

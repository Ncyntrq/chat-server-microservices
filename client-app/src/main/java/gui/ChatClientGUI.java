package gui;

import gui.components.chat.ChatMessageItem;
import gui.components.chat.UserListItem;
import gui.components.chat.SidebarCategoryHeader;
import gui.components.chat.ChatInputContainer;
import gui.theme.AppColors;
import network.ApiConfig;
import network.ChannelApiClient;
import network.ChatWebSocketClient;
import network.SessionManager;
import com.chatsever.common.dto.MessageDTO;
import com.chatsever.common.enums.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.util.List;

public class ChatClientGUI extends JFrame {
    private final JPanel chatHistoryPanel;
    private final JPanel sidebarPanel;
    private final JPanel sidebarListPanel;
    private final JScrollPane chatScrollPane;
    private final String sessionUsername;

    private final ChatWebSocketClient wsClient = new ChatWebSocketClient();
    private final ChannelApiClient channelApi = new ChannelApiClient();

    private final long activeServerId = ApiConfig.DEFAULT_SERVER_ID;
    private final long activeChannelId = ApiConfig.DEFAULT_CHANNEL_ID;

    public ChatClientGUI(String sessionUsername) {
        setTitle("Chat Server v2.0 — " + sessionUsername);
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        this.sessionUsername = sessionUsername;

        // --- 1. CHAT HISTORY (CENTER) ---
        chatHistoryPanel = new JPanel();
        chatHistoryPanel.setLayout(new BoxLayout(chatHistoryPanel, BoxLayout.Y_AXIS));
        chatHistoryPanel.setBackground(AppColors.BG_PRIMARY);
        chatHistoryPanel.add(Box.createVerticalGlue());

        chatScrollPane = new JScrollPane(chatHistoryPanel);
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // --- 2. RIGHT SIDEBAR (EAST) ---
        sidebarListPanel = new JPanel();
        sidebarListPanel.setLayout(new BoxLayout(sidebarListPanel, BoxLayout.Y_AXIS));
        sidebarListPanel.setBackground(AppColors.BG_SECONDARY);

        sidebarPanel = new JPanel(new BorderLayout());
        sidebarPanel.setBackground(AppColors.BG_SECONDARY);
        sidebarPanel.add(sidebarListPanel, BorderLayout.NORTH);

        JScrollPane sidebarScroll = new JScrollPane(sidebarPanel);
        sidebarScroll.setBorder(BorderFactory.createEmptyBorder());
        sidebarScroll.setPreferredSize(new Dimension(250, 0));
        sidebarScroll.getVerticalScrollBar().setUnitIncrement(16);

        // --- 3. INPUT (SOUTH) ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(AppColors.BG_PRIMARY);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        ChatInputContainer chatInput = new ChatInputContainer();

        chatInput.getSendButton().addActionListener(e -> sendChatFromInput(chatInput));
        chatInput.getInputField().addActionListener(e -> sendChatFromInput(chatInput));

        bottomPanel.add(chatInput, BorderLayout.CENTER);

        add(chatScrollPane, BorderLayout.CENTER);
        add(sidebarScroll, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Đóng WebSocket khi đóng cửa sổ
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                wsClient.close();
            }
        });
    }

    /**
     * Bắt đầu phiên: load history qua REST + mở WebSocket. Gọi sau khi setVisible(true).
     */
    public void startSession() {
        appendSystem("Đang tải lịch sử kênh #" + activeChannelId + "...");
        new SwingWorker<List<MessageDTO>, Void>() {
            @Override
            protected List<MessageDTO> doInBackground() {
                return channelApi.fetchRecentMessages(activeChannelId, 50);
            }

            @Override
            protected void done() {
                try {
                    List<MessageDTO> history = get();
                    for (MessageDTO m : history) appendMessage(m);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    appendSystem("Không tải được lịch sử: " + cause.getMessage());
                }
                connectWebSocket();
            }
        }.execute();
    }

    private void connectWebSocket() {
        String token = SessionManager.get().getAccessToken();
        if (token == null) {
            appendSystem("Thiếu JWT — vui lòng đăng nhập lại");
            return;
        }

        wsClient.setOnMessage(msg -> SwingUtilities.invokeLater(() -> handleIncoming(msg)));
        wsClient.setOnError(err -> SwingUtilities.invokeLater(() -> appendSystem(err)));
        wsClient.setOnClose(() -> SwingUtilities.invokeLater(() -> appendSystem("Mất kết nối tới server")));

        appendSystem("Đang kết nối WebSocket...");
        wsClient.connect(token).whenComplete((v, err) -> SwingUtilities.invokeLater(() -> {
            if (err != null) {
                appendSystem("Lỗi WebSocket: " + err.getMessage());
            } else {
                appendSystem("Đã kết nối. Channel #" + activeChannelId + " / Server #" + activeServerId);
            }
        }));
    }

    private void sendChatFromInput(ChatInputContainer chatInput) {
        String text = chatInput.getMessageText();
        if (text == null || text.trim().isEmpty()) return;

        if (!wsClient.isOpen()) {
            appendSystem("WebSocket chưa sẵn sàng, tin nhắn chưa được gửi");
            return;
        }

        MessageDTO out = new MessageDTO(MessageType.CHAT, sessionUsername, null, text, LocalDateTime.now());
        out.setServerId(activeServerId);
        out.setChannelId(activeChannelId);

        wsClient.send(out).whenComplete((ws, err) -> {
            if (err != null) {
                SwingUtilities.invokeLater(() -> appendSystem("Gửi thất bại: " + err.getMessage()));
            }
        });
        chatInput.clearInput();
    }

    /**
     * Phân loại message đến từ server và cập nhật UI tương ứng.
     */
    private void handleIncoming(MessageDTO msg) {
        if (msg.getType() == null) {
            appendMessage(msg);
            return;
        }
        switch (msg.getType()) {
            case CHAT, PRIVATE, EDIT, DELETE -> appendMessage(msg);
            case JOIN, LEAVE, SYSTEM -> appendSystem(msg.getContent());
            case ERROR -> appendSystem("⚠ " + msg.getContent());
            case TYPING, PING, PONG -> { /* ignore */ }
            case LIST -> {
                if (msg.getContent() != null) {
                    setOnlineUsers(List.of(msg.getContent().split(",")));
                }
            }
            default -> appendMessage(msg);
        }
    }

    private void appendSystem(String text) {
        MessageDTO sys = new MessageDTO(MessageType.SYSTEM, "SYSTEM", null, text, LocalDateTime.now());
        appendMessage(sys);
    }

    public void appendMessage(MessageDTO message) {
        boolean isHighlighted = message.getContent() != null &&
                message.getContent().contains("@" + sessionUsername);

        int insertIndex = chatHistoryPanel.getComponentCount() - 1;
        chatHistoryPanel.add(new ChatMessageItem(message, isHighlighted), insertIndex);
        chatHistoryPanel.add(Box.createVerticalStrut(10), insertIndex + 1);

        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    public void setOnlineUsers(List<String> usernames) {
        sidebarListPanel.removeAll();
        sidebarListPanel.add(Box.createVerticalStrut(15));
        sidebarListPanel.add(new SidebarCategoryHeader("TRỰC TUYẾN — " + usernames.size()));
        sidebarListPanel.add(Box.createVerticalStrut(5));

        for (String username : usernames) {
            if (username == null || username.isBlank()) continue;
            sidebarListPanel.add(new UserListItem(username.trim(), null, AppColors.STATUS_ONLINE));
        }

        sidebarListPanel.revalidate();
        sidebarListPanel.repaint();
    }
}

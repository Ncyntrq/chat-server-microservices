package com.chatsever.messaging.handler;

import com.chatsever.common.dto.MessageDTO;
import com.chatsever.messaging.config.WebSocketConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final String PRESENCE_URL = "http://localhost:8083/api/presence";
    public ChatWebSocketHandler(ObjectMapper objectMapper, RestTemplate restTemplate)
    {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Tạm thời chưa biết username là gì nên chưa lưu vào Map ở đây.
        System.out.println("Co nguoi moi ket noi! ID: " + session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        MessageDTO msg = objectMapper.readValue(message.getPayload(), MessageDTO.class);
        String username = msg.getSender();

        // Nếu là lần đầu User này xuất hiện trong Map, báo cho Presence Service
        if (!sessions.containsKey(username)) {
            sessions.put(username, session);
            // Ghi chú: Lưu username vào thuộc tính của session để lúc thoát còn biết ai mà báo offline
            session.getAttributes().put("username", username);

            // GỌI ĐIỆN: POST /connect?username=...
            restTemplate.postForObject(PRESENCE_URL + "/connect?username=" + username, null, Void.class);
            System.out.println(">>> Đã báo Presence: " + username + " ONLINE");
        }
        String payload = message.getPayload();
        MessageDTO msgd = objectMapper.readValue(payload, MessageDTO.class);
        sessions.put(msgd.getSender(), session);
        String jsonResponse = objectMapper.writeValueAsString(msgd);
        for(WebSocketSession s : sessions.values())
        {
            if(s.isOpen())
                s.sendMessage(new TextMessage(jsonResponse));
        }
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            sessions.remove(username);
        }
        restTemplate.postForObject(PRESENCE_URL + "/disconnect?username=" + username, null, Void.class);
        System.out.println(">>> Đã báo Presence: " + username + " OFFLINE");
    }
}

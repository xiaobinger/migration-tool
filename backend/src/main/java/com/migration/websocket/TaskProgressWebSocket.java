package com.migration.websocket;

import com.migration.model.dto.TaskProgressMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务进度WebSocket处理器
 */
@Slf4j
@Component
public class TaskProgressWebSocket extends TextWebSocketHandler {

    private static final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("WebSocket连接建立: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("WebSocket连接关闭: {}, status={}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 可以接收客户端消息，如订阅特定任务
        log.debug("收到WebSocket消息: {}", message.getPayload());
    }

    /**
     * 广播进度消息给所有连接的客户端
     */
    public void broadcast(TaskProgressMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);

            sessions.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                    }
                } catch (IOException e) {
                    log.warn("发送WebSocket消息失败: sessionId={}", session.getId(), e);
                }
            });
        } catch (Exception e) {
            log.error("广播进度消息失败", e);
        }
    }

    /**
     * 发送消息给订阅特定任务的客户端
     */
    public void sendToTask(Long taskId, TaskProgressMessage message) {
        broadcast(message); // 简化实现，广播给所有客户端
    }
}

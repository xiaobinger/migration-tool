package com.migration.config;

import com.migration.websocket.TaskProgressWebSocket;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TaskProgressWebSocket taskProgressWebSocket;

    public WebSocketConfig(TaskProgressWebSocket taskProgressWebSocket) {
        this.taskProgressWebSocket = taskProgressWebSocket;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(taskProgressWebSocket, "/ws/task-progress")
                .setAllowedOrigins("*");
    }
}

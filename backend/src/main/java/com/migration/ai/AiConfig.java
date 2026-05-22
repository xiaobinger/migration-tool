package com.migration.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "migration.ai")
public class AiConfig {

    private boolean enabled = false;

    private String apiUrl = "http://localhost:11434/v1/chat/completions";

    private String apiKey = "";

    private String model = "deepseek-chat";

    private int maxTokens = 4096;

    private double temperature = 0.7;

    private int timeout = 60000;

    private String systemPrompt = "";
}

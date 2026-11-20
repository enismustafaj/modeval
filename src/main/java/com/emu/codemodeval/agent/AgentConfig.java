package com.emu.codemodeval.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The LLM the agents run on: an OpenAI-compatible model served by OpenRouter (see spring.ai.openai in
 * application.yaml). Agents get their own system prompt and tools per call.
 */
@Configuration
public class AgentConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}

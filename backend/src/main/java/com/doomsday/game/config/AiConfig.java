package com.doomsday.game.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring AI ChatClient Bean 配置。
 *
 * 两个 ChatClient 实例：
 *   1. mainChatClient  (@Primary) — 叙事/选项生成，默认 application.yml 中的 qwen-plus
 *   2. routerChatClient            — 意图分类/轻量润色，覆盖为 qwen-turbo（快、省 token）
 *
 * 切换模型只需修改此处或 application.yml，Agent 代码无感知。
 */
@Configuration
public class AiConfig {

    /**
     * 主力 ChatClient：使用 application.yml 配置的默认模型（qwen-plus）。
     * 用于 PlotGenerationAgent、OptionGenerationAgent 等重度生成任务。
     */
    @Primary
    @Bean
    public ChatClient mainChatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * 轻量 ChatClient：覆盖为 qwen-turbo，低延迟、低 Token 消耗。
     * 用于 RouterAgent（意图分类）、NarrationAgent（文风润色）。
     *
     * ⚠️ 待确认：可替换为 qwen3.6-flash（最新轻量模型）
     */
    @Bean("routerChatClient")
    public ChatClient routerChatClient(ChatClient.Builder builder, DashScopeChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model("qwen-turbo")
                .temperature(0.1d)   // 分类任务需低温度，减少随机性
                    .maxToken(256)
                        .build())
                .build();
    }
}

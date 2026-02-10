package com.byeolnight.dto.external.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiChatRequest {
    private String model;
    private List<OpenAiMessage> messages;

    @JsonProperty("max_tokens")
    private int maxTokens;

    private double temperature;
}

package com.byeolnight.dto.external.openai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiMessage {
    private String role;
    private String content;

    public static OpenAiMessage user(String content) {
        return new OpenAiMessage("user", content);
    }
}

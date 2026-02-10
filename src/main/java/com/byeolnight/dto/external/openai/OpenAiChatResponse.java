package com.byeolnight.dto.external.openai;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class OpenAiChatResponse {
    private List<OpenAiChoice> choices;

    public String getFirstContent() {
        if (choices != null && !choices.isEmpty()) {
            OpenAiMessage message = choices.get(0).getMessage();
            if (message != null) {
                return message.getContent();
            }
        }
        return null;
    }
}

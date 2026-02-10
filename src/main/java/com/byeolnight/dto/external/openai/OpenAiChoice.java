package com.byeolnight.dto.external.openai;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OpenAiChoice {
    private int index;
    private OpenAiMessage message;
}

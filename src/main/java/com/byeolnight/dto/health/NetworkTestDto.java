package com.byeolnight.dto.health;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkTestDto {
    private String connection;
    private LocalDateTime timestamp;
    private Integer receivedData;
    private Map<String, Object> echo;
}

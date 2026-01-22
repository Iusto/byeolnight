package com.byeolnight.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 닉네임 디버깅 응답 DTO
 * - 관리자가 닉네임 중복 여부를 확인할 때 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NicknameDebugDto {

    private String inputNickname;
    private String trimmedNickname;
    private boolean exists;
    private List<String> similarNicknames;
}
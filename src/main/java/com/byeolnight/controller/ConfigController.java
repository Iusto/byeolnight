package com.byeolnight.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Config Server 설정 새로고침을 위한 컨트롤러
 */
@RestController
@RequestMapping("/api/admin/config")
public class ConfigController {

    @Autowired
    private ContextRefresher contextRefresher;

    /**
     * Config Server에서 설정을 다시 로드
     */
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Set<String>> refreshConfig() {
        Set<String> refreshedKeys = contextRefresher.refresh();
        return ResponseEntity.ok(refreshedKeys);
    }
}
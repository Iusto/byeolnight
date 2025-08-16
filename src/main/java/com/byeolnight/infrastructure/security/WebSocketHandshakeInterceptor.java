package com.byeolnight.infrastructure.security;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        System.out.println("WebSocket 핸드셰이크 시작: " + request.getURI());
        
        // URL 파라미터에서 토큰 추출
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    String token = param.substring(6); // "token=" 제거
                    attributes.put("token", java.net.URLDecoder.decode(token, "UTF-8"));
                    System.out.println("핸드셰이크에서 토큰 추출 성공");
                    break;
                }
            }
        }
        
        // 쿠키에서 토큰 추출
        String cookieHeader = request.getHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                String[] parts = cookie.trim().split("=", 2);
                if (parts.length == 2 && "accessToken".equals(parts[0])) {
                    attributes.put("token", parts[1]);
                    System.out.println("핸드셰이크에서 쿠키 토큰 추출 성공");
                    break;
                }
            }
        }
        
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 핸드셰이크 완료 후 처리
    }
}
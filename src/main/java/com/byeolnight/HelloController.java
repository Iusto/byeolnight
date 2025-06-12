package com.byeolnight;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
@Tag(name = "Hello", description = "헬로 테스트 API") // 이거 추가
public class HelloController {

    @GetMapping
    @Operation(summary = "헬로 메세지", description = "Swagger 확인용 테스트 API입니다.") // 이거도 추가
    public String hello() {
        return "Hello, Swagger!";
    }
}

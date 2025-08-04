package com.byeolnight.infrastructure.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 분산락 애노테이션
 * - 메서드에 적용하여 자동으로 분산락 처리
 * - AOP를 통해 구현
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    
    /**
     * 락 키 (SpEL 표현식 지원)
     */
    String key();
    
    /**
     * 락 획득 대기 시간 (초)
     */
    long waitTime() default 5;
    
    /**
     * 락 보유 시간 (초)
     */
    long leaseTime() default 10;
    
    /**
     * 락 획득 실패 시 예외 메시지
     */
    String timeoutMessage() default "요청을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.";
}
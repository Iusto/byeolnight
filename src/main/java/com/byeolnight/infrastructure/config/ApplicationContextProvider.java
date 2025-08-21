package com.byeolnight.infrastructure.config;

/**
 * Spring ApplicationContext 정적 접근 제공자
 * 
 * 역할:
 * - 빈 주입이 불가능한 정적 컨텍스트에서 Spring 빈 접근
 * - 유틸리티 클래스나 정적 메서드에서 빈 사용 시 활용
 * - ApplicationContextAware 인터페이스 구현으로 컨텍스트 주입 받음
 */

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextProvider implements ApplicationContextAware {
    
    private static ApplicationContext context;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }
    
    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }
    
    public static Object getBean(String beanName) {
        return context.getBean(beanName);
    }
}
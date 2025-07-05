package com.byeolnight.infrastructure.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextProvider implements ApplicationContextAware {
    
    private static ApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }
    
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
package com.procuresense.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final IdentityHeaderInterceptor identityHeaderInterceptor;

    @Autowired
    public WebConfig(IdentityHeaderInterceptor identityHeaderInterceptor) {
        this.identityHeaderInterceptor = identityHeaderInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(identityHeaderInterceptor).addPathPatterns("/api/**");
    }
}

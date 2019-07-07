package com.config.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class H2Condition implements Condition {
    @Override
    public boolean matches(ConditionContext context, 
                           AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        return null != env 
               && "H2".equals(env.getProperty("domain.datasource.type"));
    }
}
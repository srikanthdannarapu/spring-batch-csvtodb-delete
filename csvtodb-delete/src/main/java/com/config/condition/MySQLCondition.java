package com.config.condition;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class MySQLCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, 
                           AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        return null != env 
               && "MYSQL".equals(env.getProperty("domain.datasource.type"));
    }
}
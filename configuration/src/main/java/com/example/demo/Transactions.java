package com.example.demo;

import org.aopalliance.intercept.MethodInterceptor;
import org.jspecify.annotations.NonNull;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.transaction.support.TransactionTemplate;

class Transactions {

    static <T> T transactional(@NonNull TransactionTemplate template, @NonNull T target) {

        var pfb = new ProxyFactoryBean();
        pfb.setTarget(target);
        pfb.setProxyTargetClass(true);
        for (var i : target.getClass().getInterfaces()) {
            pfb.addInterface(i);
        }
        pfb.addAdvice((MethodInterceptor) invocation -> {
            var method = invocation.getMethod();
            var args = invocation.getArguments();
            return template.execute(tx -> {
                try {
                    return method.invoke(target, args);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        });
        return (T) pfb.getObject();
    }
}

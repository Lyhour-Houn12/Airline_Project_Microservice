package com.jing.common.feign;

import com.jing.common.security.AuthHeaders;
import feign.Request;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class FeignConfig {

    private static final List<String>  FORWARDED_HEADERS =
            List.of(AuthHeaders.USER_ID, AuthHeaders.USER_ROLE, "Authorization");

    @Bean
    public RequestInterceptor headerForwardingInterceptor(){
        return template -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs){
                HttpServletRequest request = attrs.getRequest();
                for (String header : FORWARDED_HEADERS){
                    String value = request.getHeader(header);
                    if (value != null) {
                        template.header(header, value);
                    }
                }
            }
        };
    }

    @Bean
    public ErrorDecoder feignErrorDecoder(){
        return new FeignErrorDecoder();
    }

    @Bean
    public Request.Options feignOptions(){
        return new Request.Options(2, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true);
    }
}

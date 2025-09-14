package org.certis.studyplatform.config;

import lombok.RequiredArgsConstructor;
import org.certis.studyplatform.shared.security.CurrentUser;
import org.certis.studyplatform.shared.security.MockCurrentUserProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.bind.support.WebDataBinderFactory;

import java.util.List;

/**
 * 테스트 환경에서만 CurrentUser를 컨트롤러 파라미터로 주입하기 위한 Resolver 설정
 */
@TestConfiguration
@RequiredArgsConstructor
@Profile("test")
public class TestWebMvcConfig implements WebMvcConfigurer {

    private final MockCurrentUserProvider provider;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(CurrentUser.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return provider.getMockCurrentUser();
            }
        });
    }

}

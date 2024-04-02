package sit.zlx.enotebackend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import sit.zlx.enotebackend.interceptor.AuthorizeInterceptor;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    private final AuthorizeInterceptor authorizeInterceptor;

    @Autowired
    WebConfiguration(AuthorizeInterceptor authorizeInterceptor) {
        this.authorizeInterceptor = authorizeInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizeInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/auth/**");
    }

}

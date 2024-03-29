package sit.zlx.enotebackend.config;

import com.alibaba.fastjson.JSONObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import sit.zlx.enotebackend.dto.ResponseDTO;
import sit.zlx.enotebackend.service.AuthService;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Objects;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    private final AuthService authService;
    private final DataSource dataSource;

    @Autowired
    SecurityConfiguration(AuthService authService, DataSource dataSource) {
        this.authService = authService;
        this.dataSource = dataSource;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, PersistentTokenRepository tokenRepository) throws Exception {
        return http
                .authorizeHttpRequests()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/super_admin/**").hasRole("SUPER_ADMIN")
                .anyRequest().authenticated()
                .and()
                .formLogin()
                .loginProcessingUrl("/api/auth/login")
                .successHandler(this::onAuthenticationSuccess)
                .failureHandler(this::onAuthenticationFailure)
                .and()
                .logout()
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(this::onAuthenticationSuccess)
                .and()
                .rememberMe()
                .rememberMeParameter("remember")
                .tokenRepository(tokenRepository)
                // 7 days
                .tokenValiditySeconds(3600 * 24 * 7)
                .and()
//                .csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).and()
                .csrf().disable()
//                .cors()
//                .configurationSource(this.corsConfigurationSource())
//                .and()
                .exceptionHandling()
                .authenticationEntryPoint(this::onAuthenticationFailure)
                .and()
                .build();
    }

    @Bean
    public PersistentTokenRepository tokenRepository() {
        JdbcTokenRepositoryImpl jdbcTokenRepository = new JdbcTokenRepositoryImpl();
        jdbcTokenRepository.setDataSource(dataSource);
        // 在创建完表之后，将其设置为 false
        jdbcTokenRepository.setCreateTableOnStartup(false);
        return jdbcTokenRepository;
    }


//    private CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration cors = new CorsConfiguration();
//        cors.addAllowedOriginPattern("*");
//        cors.setAllowCredentials(true);
//        cors.addAllowedHeader("*");
//        cors.addAllowedMethod("*");
//        cors.addExposedHeader("*");
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", cors);
//
//        return source;
//    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity security) throws Exception {
        return security
                .getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(authService)
                .and()
                .build();

    }

    private void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json;");

        if (request.getRequestURI().endsWith("/login")) {
            response.getWriter().write(JSONObject.toJSONString(new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("登录成功", null))));

        } else if (request.getRequestURI().endsWith("/logout")) {
            response.getWriter().write(JSONObject.toJSONString(new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("已退出登录", null))));
        }
    }

    private void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException {
        String message = Objects.equals(e.getMessage(), "用户名或密码错误") ? "邮箱或密码错误" : e.getMessage();

        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json;");
        response.getWriter().write(JSONObject.toJSONString(new ResponseDTO<>(ResponseDTO.STATUS_CODE.UNAUTHORIZED.getCode(), new ResponseDTO.ResponseData<>(message, null))));
    }
}


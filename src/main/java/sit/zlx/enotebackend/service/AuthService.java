package sit.zlx.enotebackend.service;

import org.springframework.security.core.userdetails.UserDetailsService;

public interface AuthService extends UserDetailsService {
    String sendCode(String email, String sessionId, String actionKey);

    String register(String password, String email, String code, String sessionId);

    String validateCode(String email, String code, String sessionId, String actionKey);

    Boolean resetPassword(String email, String password);
}
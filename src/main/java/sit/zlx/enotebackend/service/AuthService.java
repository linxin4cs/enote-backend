package sit.zlx.enotebackend.service;

import org.springframework.security.core.userdetails.UserDetailsService;

public interface AuthService extends UserDetailsService {
    String sendCode(String email, String sessionId, boolean hasAccount);

    String register(String name, String password, String email, String code, String sessionId);

    String validateCode(String email, String code, String sessionId);

    Boolean resetPassword(String email, String password);
}
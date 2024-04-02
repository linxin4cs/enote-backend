package sit.zlx.enotebackend.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import sit.zlx.enotebackend.domain.User;
import sit.zlx.enotebackend.dto.UserDTO;
import sit.zlx.enotebackend.service.UserService;

@Component
public class AuthorizeInterceptor implements HandlerInterceptor {
    private final UserService userService;

    @Autowired
    AuthorizeInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request,
                             @NotNull HttpServletResponse response,
                             @NotNull Object handler) throws Exception {

        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();

        org.springframework.security.core.userdetails.User userDetails
                = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
        String email = userDetails.getUsername();

        User user = userService.getOne(new QueryWrapper<User>().eq("email", email));

        UserDTO userDTO = UserDTO.toDTO(user);
        request.getSession().setAttribute("user", userDTO);

        return true;
    }

}
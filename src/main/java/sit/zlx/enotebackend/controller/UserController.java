package sit.zlx.enotebackend.controller;


import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sit.zlx.enotebackend.service.UserService;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Resource
    public UserService userService;
}

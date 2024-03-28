package sit.zlx.enotebackend.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sit.zlx.enotebackend.domain.User;
import sit.zlx.enotebackend.dto.ResponseDTO;
import sit.zlx.enotebackend.dto.UserDTO;
import sit.zlx.enotebackend.service.UserService;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseDTO<UserDTO> me(@AuthenticationPrincipal UserDetails currentUser) {
        QueryWrapper<User> queryWrapper = new  QueryWrapper<User>().eq("email", currentUser.getUsername());
        User user = userService.getOne(queryWrapper);

        return new ResponseDTO<>(
                ResponseDTO.STATUS_CODE.SUCCESS.getCode(),
                new ResponseDTO.ResponseData<>("", UserDTO.toDTO(user))
        );
    }
}

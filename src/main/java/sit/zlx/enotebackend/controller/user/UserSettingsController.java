package sit.zlx.enotebackend.controller.user;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import jakarta.servlet.http.HttpSession;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sit.zlx.enotebackend.domain.User;
import sit.zlx.enotebackend.dto.RequestDTO;
import sit.zlx.enotebackend.dto.ResponseDTO;
import sit.zlx.enotebackend.service.AdminService;
import sit.zlx.enotebackend.service.AuthService;
import sit.zlx.enotebackend.service.UserService;

import java.util.List;

import static sit.zlx.enotebackend.controller.AuthController.returnSendCodeResult;

@RestController
@RequestMapping("/api/settings")
public class UserSettingsController {
    private final AuthService authService;
    private final UserService userService;
    private final AdminService adminService;

    @Autowired
    public UserSettingsController(AuthService authService, UserService userService, AdminService adminService) {
        this.authService = authService;
        this.userService = userService;
        this.adminService = adminService;
    }

    @PostMapping("/delete-account/send-code")
    public ResponseDTO<?> sendDeleteAccountCode(@AuthenticationPrincipal UserDetails currentUser, HttpSession httpSession) {
        try {
            String result = authService.sendCode(currentUser.getUsername(), httpSession.getId(), "deleteAccount");
            return returnSendCodeResult(result);
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("验证码发送失败，请联系管理员！", null));
        }
    }

@PostMapping("/delete-account/validate-code")
public ResponseDTO<?> validateDeleteAccountCode(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<ValidateDeleteAccountCodeBody> requestDTO, HttpSession httpSession) {
    try {
        User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));

        if (user.getRole() != 0   ) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("无法删除超级管理员！", null));
        }

        String code = requestDTO.getData().getCode();
    String result = authService.validateCode(currentUser.getUsername(), code, httpSession.getId(), "deleteAccount");

        if (result == null) {
            List<String> ids = List.of(user.getId());

            userService.update(new UpdateWrapper<User>().set("isDeleting", 1).in("id", ids));
            adminService.deleteUserData(ids);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("", "确认删除！"));
        }

        return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>(result, null));

    } catch (Exception e) {
        return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("验证失败，请联系管理员！", null));
    }
}

    @Data
    @NoArgsConstructor
    public static class ValidateDeleteAccountCodeBody {
        String code;
    }

}

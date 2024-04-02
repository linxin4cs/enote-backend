package sit.zlx.enotebackend.controller;

import jakarta.servlet.http.HttpSession;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sit.zlx.enotebackend.dto.RequestDTO;
import sit.zlx.enotebackend.dto.ResponseDTO;
import sit.zlx.enotebackend.service.AuthService;
import sit.zlx.enotebackend.service.MyUtils;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;

    @Autowired
    AuthController(AuthService service) {
        this.service = service;
    }

    @NotNull
    public static ResponseDTO<String> returnSendCodeResult(String result) {
        if (result == null) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("邮件已发送，请注意查收！", null));
        } else {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>(result, null));
        }
    }

    @PostMapping("/send-register-code")
    public ResponseDTO<?> sendRegisterCode(@RequestBody RequestDTO<SendCodeBody> requestDTO,
                                           HttpSession httpSession) {

        String emailValidation = MyUtils.Validator.validateEmail(requestDTO.getData().getEmail());
        if (!emailValidation.isEmpty()) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>(emailValidation, null));
        }

        String result = service.sendCode(requestDTO.getData().getEmail(), httpSession.getId(), "register");
        return returnSendCodeResult(result);
    }

    @PostMapping("/send-reset-code")
    public ResponseDTO<?> sendResetCode(@RequestBody RequestDTO<SendCodeBody> requestDTO,
                                        HttpSession httpSession) {

        String emailValidation = MyUtils.Validator.validateEmail(requestDTO.getData().getEmail());
        if (!emailValidation.isEmpty()) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>(emailValidation, null));
        }

        String result = service.sendCode(requestDTO.getData().getEmail(), httpSession.getId(), "reset");
        return returnSendCodeResult(result);
    }

    @PostMapping("/register")
    public ResponseDTO<?> register(@RequestBody RequestDTO<RegisterBody> requestDTO,
                                   HttpSession httpSession) {

        String emailValidation = MyUtils.Validator.validateEmail(requestDTO.getData().getEmail());
        String passwordValidation = MyUtils.Validator.validatePassword(requestDTO.getData().getPassword());
        if (!(emailValidation + passwordValidation).isEmpty()) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>(emailValidation + passwordValidation, null));
        }

        String result = service.register(requestDTO.getData().getPassword(), requestDTO.getData().getEmail(), requestDTO.getData().getCode(), httpSession.getId());

        if (result == null) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("注册成功！", null));
        } else {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>(result, null));
        }
    }

    @PostMapping("/validate-reset-code")
    public ResponseDTO<?> validateResetCode(@RequestBody RequestDTO<ValidateResetCodeBody> requestDTO,
                                            HttpSession httpSession) {

        String emailValidation = MyUtils.Validator.validateEmail(requestDTO.getData().getEmail());
        if (!emailValidation.isEmpty()) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>(emailValidation, null));
        }

        String email = requestDTO.getData().getEmail();
        String result = service.validateCode(email, requestDTO.getData().getCode(), httpSession.getId(), "reset");

        if (result == null) {
            httpSession.setAttribute("reset-token", email);
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("开始重置密码！", null));
        } else {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>(result, null));
        }
    }

    @PostMapping("/reset-password")
    public ResponseDTO<?> resetPassword(@RequestBody RequestDTO<ResetPasswordBody> requestDTO,
                                        HttpSession httpSession) {

        String passwordValidation = MyUtils.Validator.validatePassword(requestDTO.getData().getPassword());
        if (!passwordValidation.isEmpty()) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>(passwordValidation, null));
        }

        String email = (String) httpSession.getAttribute("reset-token");

        if (email == null) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("请先完成邮箱验证！", null));
        } else if (service.resetPassword(email, requestDTO.getData().getPassword())) {
            httpSession.removeAttribute("reset-token");

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("密码重置成功！", null));
        } else {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("内部错误，请联系管理员！", null));
        }
    }

    @Data
    @NoArgsConstructor
    public static class SendCodeBody {
        String email;
    }

    @Data
    @NoArgsConstructor
    public static class RegisterBody {
        String email;
        String password;
        String code;
    }

    @Data
    @NoArgsConstructor
    public static class ValidateResetCodeBody {
        String email;
        String code;
    }

    @Data
    @NoArgsConstructor
    public static class ResetPasswordBody {
        String password;
    }
}

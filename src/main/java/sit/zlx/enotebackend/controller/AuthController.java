package sit.zlx.enotebackend.controller;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import sit.zlx.enotebackend.dto.RequestDTO;
import sit.zlx.enotebackend.dto.ResponseDTO;
import sit.zlx.enotebackend.service.AuthService;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;

    @Autowired
    AuthController(AuthService service) {
        this.service = service;
    }

    @Data
    @AllArgsConstructor
    public static class RegisterBody {
        String password;
        String email;
        String code;
    }

    @Data
    @AllArgsConstructor
    public static class ValidateResetCodeBody {
        String email;
        String code;
    }

    @PostMapping("/send-register-code")
    public ResponseDTO<?> sendRegisterCode(@RequestBody RequestDTO<String> requestDTO,
                                           HttpSession httpSession) {

        String result = service.sendCode(requestDTO.getData(), httpSession.getId(), false);
        return returnSendCodeResult(result);
    }

    @NotNull
    private ResponseDTO<?> returnSendCodeResult(String result) {
        if (result == null) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("邮件已发送，请注意查收", null));
        } else {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>(result, null));
        }
    }


    @PostMapping("/send-reset-code")
    public ResponseDTO<?> sendResetCode(@RequestBody RequestDTO<String> requestDTO,
                                        HttpSession httpSession) {

        String result = service.sendCode(requestDTO.getData(), httpSession.getId(), true);
        return returnSendCodeResult(result);
    }

    @PostMapping("/register")
    public ResponseDTO<?> register(@RequestBody  RequestDTO<RegisterBody> requestDTO,
                                   HttpSession httpSession) {

        String result = service.register(requestDTO.getData().getPassword(), requestDTO.getData().getEmail(), requestDTO.getData().getCode(), httpSession.getId());

        if (result == null) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("注册成功", null));
        } else {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>(result, null));
        }
    }

    @PostMapping("/validate-reset-code")
    public ResponseDTO<?> validateResetCode(@RequestBody RequestDTO<ValidateResetCodeBody> requestDTO,
                                            HttpSession httpSession) {

        String email = requestDTO.getData().getEmail();
        String result = service.validateCode(email, requestDTO.getData().getCode(), httpSession.getId());

        if (result == null) {
            httpSession.setAttribute("reset-token", email);
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("开始重置密码", null));
        } else {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>(result, null));
        }
    }

    @PostMapping("/reset-password")
    public ResponseDTO<?> resetPassword(@RequestBody RequestDTO<String> requestDTO,
                                        HttpSession httpSession) {

        String email = (String) httpSession.getAttribute("reset-token");

        if (email == null) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("请先完成邮箱验证", null));
        } else if (service.resetPassword(email, requestDTO.getData())) {
            httpSession.removeAttribute("reset-token");

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("密码重置成功", null));
        } else {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("内部错误，请联系管理员", null));
        }
    }
}

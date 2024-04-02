package sit.zlx.enotebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sit.zlx.enotebackend.domain.File;
import sit.zlx.enotebackend.domain.User;
import sit.zlx.enotebackend.dto.RequestDTO;
import sit.zlx.enotebackend.dto.ResponseDTO;
import sit.zlx.enotebackend.dto.UserDTO;
import sit.zlx.enotebackend.service.*;
import sit.zlx.enotebackend.service.impl.AuthServiceImpl;

import java.nio.file.Files;
import java.nio.file.Paths;

import static sit.zlx.enotebackend.controller.AuthController.returnSendCodeResult;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final UploadService uploadService;
    private final FileService fileService;


    @Autowired
    public UserController(UserService userService, AuthService authService, UploadService uploadService, FileService fileService) {
        this.userService = userService;
        this.authService = authService;
        this.uploadService = uploadService;
        this.fileService = fileService;
    }

    @GetMapping("/me")
    public ResponseDTO<UserDTO> me(@AuthenticationPrincipal UserDetails currentUser) {
        try {
            QueryWrapper<User> queryWrapper = new QueryWrapper<User>().eq("email", currentUser.getUsername());
            User user = userService.getOne(queryWrapper);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取成功！", UserDTO.toDTO(user)));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取失败！", null));
        }
    }

    @PostMapping("/edit/info")
    public ResponseDTO<?> editInfo(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<EditInfoBody> requestDTO) {
        String nameValidation = MyUtils.Validator.validateName(requestDTO.getData().getName());
        if (!nameValidation.isEmpty()) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>(nameValidation, null));
        }

        try {
            if (currentUser.getUsername().equals(requestDTO.getData().getName())) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("请勿提供原用户名！", null));
            }

            User comparedUser = userService.getOne(new QueryWrapper<User>().eq("name", requestDTO.getData().getName()));

            if (comparedUser != null) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("用户名已存在！", null));
            }

            User targetUser = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));
            targetUser.setName(requestDTO.getData().getName());
            userService.updateById(targetUser);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("", "修改成功！"));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("修改失败！", null));
        }
    }

    @PostMapping("/edit/email/send-code/old")
    public ResponseDTO<?> sendCodeOldEmail(@AuthenticationPrincipal UserDetails currentUser, HttpSession httpSession) {
        try {
            String result = authService.sendCode(currentUser.getUsername(), httpSession.getId(), "changeOldEmail");
            return returnSendCodeResult(result);
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("验证码发送失败，请联系管理员！", null));
        }
    }

    @PostMapping("/edit/email/send-code/new")
    public ResponseDTO<?> sendCodeNewEmail(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<SendCodeNewEmailBody> requestDTO, HttpSession httpSession) {
        if (requestDTO.getData().getEmail().equals(currentUser.getUsername())) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("请勿提供原邮箱！", null));
        }

        String emailValidation = MyUtils.Validator.validateEmail(requestDTO.getData().getEmail());
        if (!emailValidation.isEmpty()) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>(emailValidation, null));
        }

        User user = userService.getOne(new QueryWrapper<User>().eq("email", requestDTO.getData().getEmail()));
        if (user != null) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("该邮箱已被注册！", null));
        }

        try {
            String result = authService.sendCode(requestDTO.getData().getEmail(), httpSession.getId(), "changeNewEmail");
            return returnSendCodeResult(result);
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("验证码发送失败，请联系管理员！", null));
        }
    }

    @PostMapping("/edit/email/validate-code")
    public ResponseDTO<?> validateEmailCode(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<ValidateCodeBody> requestDTO, HttpSession httpSession) {
        String newEmail = requestDTO.getData().getNewEmail();

        String newEmailValidation = MyUtils.Validator.validateEmail(newEmail);
        if (!newEmailValidation.isEmpty()) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>(newEmailValidation, null));
        }

        try {
            User originalUser = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));

            SecurityContext context = SecurityContextHolder.getContext();
            Authentication currentAuthentication = context.getAuthentication();
            UserDetails originalUserDetails = (UserDetails) currentAuthentication.getPrincipal();
            String newUsername = requestDTO.getData().getNewEmail();
            UserDetails newUserDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(newUsername)
                    .password(originalUser.getPassword())
                    .authorities(originalUserDetails.getAuthorities())
                    .build();

            Authentication newAuthentication = new UsernamePasswordAuthenticationToken(
                    newUserDetails,
                    currentAuthentication.getCredentials(),
                    newUserDetails.getAuthorities());

            String oldEmail = currentUser.getUsername();
            String oldEmailCode = requestDTO.getData().getOldEmailCode();
            String newEmailCode = requestDTO.getData().getNewEmailCode();
            String oldEmailCodeResult = authService.validateCode(oldEmail, oldEmailCode, httpSession.getId(), "changeOldEmail");
            String newEmailCodeResult = authService.validateCode(newEmail, newEmailCode, httpSession.getId(), "changeNewEmail");

            if (oldEmailCodeResult == null && newEmailCodeResult == null) {
                originalUser.setEmail(requestDTO.getData().getNewEmail());
                userService.updateById(originalUser);

                context.setAuthentication(newAuthentication);

                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("", "修改成功！"));
            }


            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>(oldEmailCodeResult + "," + newEmailCodeResult, null));

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("验证失败，请联系管理员！", null));
        }
    }

    @PostMapping("/edit/password/send-code")
    public ResponseDTO<?> sendCode(@AuthenticationPrincipal UserDetails currentUser, HttpSession httpSession) {
        try {
            String result = authService.sendCode(currentUser.getUsername(), httpSession.getId(), "changePassword");
            return returnSendCodeResult(result);
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("验证码发送失败，请联系管理员！", null));
        }
    }

    @PostMapping("/edit/password/validate-code")
    public ResponseDTO<?> validatePasswordCode(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<validatePasswordCodeBody> requestDTO, HttpSession httpSession) {
        String password = requestDTO.getData().getPassword();

        String passwordValidation = MyUtils.Validator.validatePassword(password);
        if (!passwordValidation.isEmpty()) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>(passwordValidation, null));
        }

        try {
            SecurityContext context = SecurityContextHolder.getContext();
            Authentication currentAuthentication = context.getAuthentication();
            UserDetails originalUserDetails = (UserDetails) currentAuthentication.getPrincipal();

            String result = authService.validateCode(currentUser.getUsername(), requestDTO.getData().getCode(), httpSession.getId(), "changePassword");

            if (result == null) {
                User originalUser = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));
                String newPassword = AuthServiceImpl.passwordEncoder.encode(password);
                originalUser.setPassword(newPassword);
                userService.updateById(originalUser);


                UserDetails newUserDetails = org.springframework.security.core.userdetails.User.builder()
                        .username(originalUser.getEmail())
                        .password(newPassword)
                        .authorities(originalUserDetails.getAuthorities())
                        .build();

                Authentication newAuthentication = new UsernamePasswordAuthenticationToken(
                        newUserDetails,
                        currentAuthentication.getCredentials(),
                        newUserDetails.getAuthorities());

                context.setAuthentication(newAuthentication);

                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("", "修改成功！"));
            }

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>(result, null));

        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("验证失败，请联系管理员！", null));
        }
    }

    @PostMapping("/edit/avatar")
    public ResponseDTO<editAvatarResponse> editAvatar(@AuthenticationPrincipal UserDetails currentUser, @RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("请上传文件！", null));
        }

        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));
            if (user.getAvatar() != null) {
                File oldFile = fileService.getOne(new QueryWrapper<File>().eq("id", user.getAvatar().split("/")[4]));
                Files.delete(Paths.get(oldFile.getPath()));
                fileService.removeById(oldFile.getId());
            }

            File file = uploadService.storeFile(files[0], UploadService.FILE_TYPE.IMAGE, user.getId());

            String avatar = "/api/file/image/" + file.getId();
            user.setAvatar(avatar);
            userService.updateById(user);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("修改成功！", new editAvatarResponse(avatar)));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("修改失败！", null));
        }
    }


    @Data
    @NoArgsConstructor
    public static class EditInfoBody {
        String name;
    }

    @Data
    @NoArgsConstructor
    public static class SendCodeNewEmailBody {
        String email;
    }

    @Data
    @NoArgsConstructor
    public static class ValidateCodeBody {
        String oldEmailCode;
        String newEmail;
        String newEmailCode;
    }

    @Data
    @NoArgsConstructor
    public static class validatePasswordCodeBody {
        String password;
        String code;
    }

    @Data
    @AllArgsConstructor
    public static class editAvatarResponse {
        String avatar;
    }
}

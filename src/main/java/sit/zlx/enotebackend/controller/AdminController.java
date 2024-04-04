package sit.zlx.enotebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sit.zlx.enotebackend.domain.User;
import sit.zlx.enotebackend.dto.PaginatedDTO;
import sit.zlx.enotebackend.dto.RequestDTO;
import sit.zlx.enotebackend.dto.ResponseDTO;
import sit.zlx.enotebackend.dto.UserDTO;
import sit.zlx.enotebackend.service.MyUtils;
import sit.zlx.enotebackend.service.UserService;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static sit.zlx.enotebackend.service.MyUtils.generateRandomString;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    public static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserService userService;

    @Autowired
    AdminController(UserService userService) {
        this.userService = userService;
    }

    @NotNull
    private static QueryWrapper<User> getUserQueryWrapper(ListUsersBody.Query searchParams) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("createdAt");

        // 根据搜索参数构建查询条件
        if (searchParams != null) {
            if (searchParams.getKeyword() != null) {
                queryWrapper.and(wrapper ->
                        wrapper.like("id", searchParams.getKeyword())
                                .or().like("name", searchParams.getKeyword())
                                .or().like("email", searchParams.getKeyword())
                );
            }

            // 添加基于日期范围的搜索条件
            if (searchParams.getCreatedAtStart() != null) {
                queryWrapper.ge("createdAt", searchParams.getCreatedAtStart()); // ge 是 "greater than or equal to" 的缩写
            }
            if (searchParams.getCreatedAtEnd() != null) {
                queryWrapper.le("createdAt", searchParams.getCreatedAtEnd()); // le 是 "less than or equal to" 的缩写
            }
        }
        return queryWrapper;
    }

    @PostMapping("/manage/user/list")
    public ResponseDTO<PaginatedDTO<UserDTO>> listUsers(@RequestBody RequestDTO<ListUsersBody> requestDTO) {
        try {
            ListUsersBody pageParams = requestDTO.getData();
            ListUsersBody.Query searchParams = pageParams.getQuery(); // 获取搜索参数
            int page = pageParams.getPage();
            int size = pageParams.getSize();

            Page<User> pageObj = new Page<>(page, size);

            QueryWrapper<User> queryWrapper = getUserQueryWrapper(searchParams);

            IPage<User> userPage = userService.page(pageObj, queryWrapper);
            List<User> users = userPage.getRecords();
            List<UserDTO> usersDTO = users.stream().map(UserDTO::toDTO).toList();

            ResponseDTO.ResponseData<PaginatedDTO<UserDTO>> responseData = new ResponseDTO.ResponseData<>("获取用户列表成功！", new PaginatedDTO<>(usersDTO, (int) userPage.getCurrent(), (int) userPage.getTotal(), (int) userPage.getPages()));
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), responseData);
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取用户列表失败！", null));
        }

    }

    @PostMapping("/manage/user/edit")
    public ResponseDTO<?> editUser(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<EditUserBody> requestDTO) {
        User targetUser = userService.getById(requestDTO.getData().getId());
        User currentUserEntity = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));
        if (targetUser == null) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("用户不存在！", null));
        }

        if (Objects.equals(targetUser.getId(), currentUserEntity.getId())) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.FORBIDDEN.getCode(), new ResponseDTO.ResponseData<>("请前往个人中心进行修改！", null));
        }

        if (targetUser.getRole() >= currentUserEntity.getRole()) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.FORBIDDEN.getCode(), new ResponseDTO.ResponseData<>("权限不足！", null));
        }

        String emailValidation = MyUtils.Validator.validateEmail(requestDTO.getData().getNewEmail());
        String nameValidation = MyUtils.Validator.validateName(requestDTO.getData().getNewName());
        String roleValidation = MyUtils.Validator.validateRole(requestDTO.getData().getNewRole());
        String statusValidation = MyUtils.Validator.validateStatus(requestDTO.getData().getNewStatus());
        String validation = emailValidation + nameValidation + roleValidation + statusValidation;
        if (!validation.isEmpty()) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>(validation, null));
        }

        if (targetUser.getEmail().equals(requestDTO.getData().getNewEmail()) && targetUser.getName().equals(requestDTO.getData().getNewName()) && targetUser.getRole() == requestDTO.getData().getNewRole() && targetUser.getStatus() == requestDTO.getData().getNewStatus()) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("请勿提供原信息！", null));
        }

        try {
            targetUser.setEmail(requestDTO.getData().getNewEmail());
            targetUser.setName(requestDTO.getData().getNewName());
            targetUser.setRole(requestDTO.getData().getNewRole());
            targetUser.setStatus(requestDTO.getData().getNewStatus());
            userService.updateById(targetUser);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("修改用户信息成功！", null));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("修改用户信息失败！", null));
        }
    }

    @PostMapping("/manage/user/new")
    public ResponseDTO<?> newUser(@RequestBody RequestDTO<NewUserBody> requestDTO) {
        String emailValidation = MyUtils.Validator.validateEmail(requestDTO.getData().getEmail());
        if (!(emailValidation).isEmpty()) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>(emailValidation, null));
        }

        String email = requestDTO.getData().getEmail();
        User user = userService.getOne(new QueryWrapper<User>().eq("email", email));
        if (user != null) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("邮箱已被注册！", null));
        }

        try {
            User newUser = new User();
            String name;

            do {
                name = "user" + generateRandomString(8);

            } while (userService.getOne(new QueryWrapper<User>().eq("name", name)) != null);

            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setPassword(passwordEncoder.encode("enotepwd"));
            newUser.setStatus(0);
            newUser.setRole(0);

            userService.save(newUser);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("新建用户成功！", null));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("新建用户失败！", null));
        }
    }

    @PostMapping("/manage/user/delete")
    public ResponseDTO<?> deleteUser(@AuthenticationPrincipal UserDetails currentUser, @RequestBody RequestDTO<DeleteUserBody> requestDTO) {
        User currentUserEntity = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));
        List<Integer> ids = requestDTO.getData().getIds();

        for (Integer id : ids) {
            User targetUser = userService.getById(id);
            if (targetUser == null) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("用户不存在！", null));
            }

            if (Objects.equals(targetUser.getId(), currentUserEntity.getId())) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.FORBIDDEN.getCode(), new ResponseDTO.ResponseData<>("不能删除自己！", null));
            }

            if (targetUser.getRole() >= currentUserEntity.getRole()) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.FORBIDDEN.getCode(), new ResponseDTO.ResponseData<>("权限不足！", null));
            }
        }


        try {
            userService.removeByIds(ids);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("删除用户成功！", null));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("删除用户失败！", null));
        }
    }

    @Data
    @NoArgsConstructor
    public static class ListUsersBody {
        private int page;
        private int size;
        private Query query;

        @Data
        @NoArgsConstructor
        public static class Query {
            private String keyword;
            private Date createdAtStart;
            private Date createdAtEnd;
        }
    }

    @Data
    @NoArgsConstructor
    public static class EditUserBody {
        private int id;
        private String newEmail;
        private String newName;
        private int newRole;
        private int newStatus;
    }

    @Data
    @NoArgsConstructor
    public static class NewUserBody {
        private String email;
    }

    @Data
    @NoArgsConstructor
    public static class DeleteUserBody {
        private List<Integer> ids;
    }
}

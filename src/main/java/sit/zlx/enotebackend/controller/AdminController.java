package sit.zlx.enotebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import sit.zlx.enotebackend.domain.ActiveNote;
import sit.zlx.enotebackend.domain.ActiveUser;
import sit.zlx.enotebackend.domain.File;
import sit.zlx.enotebackend.domain.User;
import sit.zlx.enotebackend.dto.PaginatedDTO;
import sit.zlx.enotebackend.dto.RequestDTO;
import sit.zlx.enotebackend.dto.ResponseDTO;
import sit.zlx.enotebackend.dto.UserDTO;
import sit.zlx.enotebackend.service.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static sit.zlx.enotebackend.service.MyUtils.generateRandomString;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserService userService;
    private final NoteService noteService;
    private final ActiveUserService activeUserService;
    private final ActiveNoteService activeNoteService;
    private final FileService fileService;
    private final AdminService adminService;


    @Autowired
    AdminController(UserService userService, NoteService noteService, ActiveUserService activeUserService, ActiveNoteService activeNoteService, FileService fileService, AdminService adminService) {
        this.userService = userService;
        this.noteService = noteService;
        this.activeUserService = activeUserService;
        this.activeNoteService = activeNoteService;
        this.fileService = fileService;
        this.adminService = adminService;
    }

    @NotNull
    private static QueryWrapper<User> getUserQueryWrapper(ListUsersBody.Query searchParams) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("createdAt");

        // 先过滤掉正在删除的用户
        queryWrapper.eq("isDeleting", 0);

        // 根据搜索参数构建查询条件
        if (searchParams != null) {
            if (searchParams.getKeyword() != null) {
                queryWrapper.and(wrapper -> wrapper.like("id", searchParams.getKeyword()).or().like("name", searchParams.getKeyword()).or().like("email", searchParams.getKeyword()));
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

        if (targetUser.getId().equals(currentUserEntity.getId())) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.FORBIDDEN.getCode(), new ResponseDTO.ResponseData<>("请前往个人中心进行修改！", null));
        }

        if (targetUser.getRole() >= currentUserEntity.getRole()) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.FORBIDDEN.getCode(), new ResponseDTO.ResponseData<>("权限不足！", null));
        }

        if (targetUser.getIsDeleting() == 1) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.BAD_REQUEST.getCode(), new ResponseDTO.ResponseData<>("用户正在删除中！", null));
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

            newUser.setId(UUID.randomUUID().toString());
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
        List<String> ids = requestDTO.getData().getIds();

        for (String id : ids) {
            User targetUser = userService.getById(id);

            if (targetUser == null || targetUser.getIsDeleting() == 1) {
                continue;
            }

            if (Objects.equals(targetUser.getId(), currentUserEntity.getId())) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.FORBIDDEN.getCode(), new ResponseDTO.ResponseData<>("不能删除自己！", null));
            }

            if (targetUser.getRole() >= currentUserEntity.getRole()) {
                return new ResponseDTO<>(ResponseDTO.STATUS_CODE.FORBIDDEN.getCode(), new ResponseDTO.ResponseData<>("只能删除权限小于自己的用户！", null));
            }
        }

        try {
            // 先将用户的isDeleting字段设置为true，然后异步删除用户的文件和相关记录
            userService.update(new UpdateWrapper<User>().set("isDeleting", 1).in("id", ids));
            adminService.deleteUserFiles(ids);
            adminService.deleteUserRelatedRecords(ids);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("删除用户成功！", null));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("删除用户失败！", null));
        }
    }

    @GetMapping("/dashboard/user/total")
    public ResponseDTO<DashBoardCardBody> totalUser() {
        try {
            DashBoardCardBody dashBoardCardBody = new DashBoardCardBody();
            // 今天0点
            Date todayZero = MyUtils.Time.getTodayZero();
            Long newNum = userService.count();
            // 截止至今天0点的计数
            Long oldNum = userService.count(new QueryWrapper<User>().le("createdAt", todayZero));
            dashBoardCardBody.setNum(newNum);
            dashBoardCardBody.setPercentValue(oldNum, newNum);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取用户总数成功！", dashBoardCardBody));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取用户总数失败！", null));
        }
    }

    @GetMapping("/dashboard/user/daily/new")
    public ResponseDTO<DashBoardCardBody> dailyNewUser() {
        try {
            DashBoardCardBody dashBoardCardBody = new DashBoardCardBody();
            Date todayZero = MyUtils.Time.getTodayZero();
            Date yesterdayZero = MyUtils.Time.getYesterdayZero();
            // 今天创建的用户数
            Long newNum = userService.count(new QueryWrapper<User>().ge("createdAt", todayZero));
            // 昨天创建的用户数
            Long oldNum = userService.count(new QueryWrapper<User>().between("createdAt", yesterdayZero, todayZero));
            dashBoardCardBody.setNum(newNum);
            dashBoardCardBody.setPercentValue(oldNum, newNum);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取用户日新增数成功！", dashBoardCardBody));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取用户日新增数失败！", null));
        }
    }

    @GetMapping("/dashboard/user/weekly/new")
    public ResponseDTO<BarChartDataBody> weeklyNewUser() {
        try {
            BarChartDataBody barChartDataBody = new BarChartDataBody();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date lastWeekZero = MyUtils.Time.getLastWeekZero();
            // 获取过去7天的数据，如果那一天没有数据，也要加到返回结果中，如果有数据，但是为0，也要加到返回结果中
            List<BarChartDataBody.BarChartDataItem> data = new java.util.ArrayList<>(userService.list(new QueryWrapper<User>().select("createdAt")).stream().map(User::getCreatedAt).filter(Objects::nonNull).filter(date -> date.after(lastWeekZero)).map(sdf::format).distinct().map(date -> {
                String nextDayZero = date.substring(0, 8) + (Integer.parseInt(date.substring(8)) + 1);
                BarChartDataBody.BarChartDataItem item = new BarChartDataBody.BarChartDataItem();
                item.setDate(date.substring(5).replace("-", "/"));
                item.setNum(userService.count(new QueryWrapper<User>().between("createdAt", date, nextDayZero)));
                return item;
            }).toList());

            // 补全过去7天（包括今天）的数据
            adminService.formatAndSortBarChartData(barChartDataBody, sdf, data);


            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取用户周新增数成功！", barChartDataBody));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取用户周新增数失败！", null));
        }
    }


    @GetMapping("/dashboard/user/daily/active")
    public ResponseDTO<DashBoardCardBody> dailyActiveUser() {
        try {
            DashBoardCardBody dashBoardCardBody = new DashBoardCardBody();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            // 今天日期，格式为 yyyy-MM-dd
            String today = sdf.format(new Date());
            String yesterday = sdf.format(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
            // 今天活跃的用户数
            Long newNum = activeUserService.count(new QueryWrapper<ActiveUser>().eq("loginDate", today));
            // 昨天活跃的用户数
            Long oldNum = activeUserService.count(new QueryWrapper<ActiveUser>().eq("loginDate", yesterday));
            dashBoardCardBody.setNum(newNum);
            dashBoardCardBody.setPercentValue(oldNum, newNum);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取用户日活跃数成功！", dashBoardCardBody));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取用户日活跃数失败！", null));
        }
    }

    @GetMapping("/dashboard/user/weekly/active")
    public ResponseDTO<BarChartDataBody> weeklyActiveUser() {
        try {
            BarChartDataBody barChartDataBody = new BarChartDataBody();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date lastWeek = new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000);
            // 获取过去7天的数据，如果那一天没有数据，也要加到返回结果中，如果有数据，但是为0，也要加到返回结果中
            List<BarChartDataBody.BarChartDataItem> data = new java.util.ArrayList<>(activeUserService.list(new QueryWrapper<ActiveUser>().select("loginDate")).stream().map(ActiveUser::getLoginDate).filter(Objects::nonNull).filter(date -> date.after(lastWeek)).map(sdf::format).distinct().map(date -> {
                BarChartDataBody.BarChartDataItem item = new BarChartDataBody.BarChartDataItem();
                item.setDate(date.substring(5).replace("-", "/"));
                item.setNum(activeUserService.count(new QueryWrapper<ActiveUser>().eq("loginDate", date).eq("isDeleting", 0)));
                return item;
            }).toList());

            // 补全过去7天（包括今天）的数据，不能出现两个相同的日期
            adminService.formatAndSortBarChartData(barChartDataBody, sdf, data);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取用户周活跃数成功！", barChartDataBody));

        } catch (Exception e) {

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取用户周活跃数失败！", null));
        }

    }

    @GetMapping("/dashboard/note/total")
    public ResponseDTO<DashBoardCardBody> totalNote() {
        try {
            DashBoardCardBody dashBoardCardBody = new DashBoardCardBody();
            Date todayZero = MyUtils.Time.getTodayZero();
            Long newNum = noteService.count();
            Long oldNum = noteService.count(new QueryWrapper<sit.zlx.enotebackend.domain.Note>().le("createdAt", todayZero));
            dashBoardCardBody.setNum(newNum);
            dashBoardCardBody.setPercentValue(oldNum, newNum);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取笔记总数成功！", dashBoardCardBody));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取笔记总数失败！", null));
        }
    }

    @GetMapping("/dashboard/note/daily/new")
    public ResponseDTO<DashBoardCardBody> dailyNewNote() {
        try {
            DashBoardCardBody dashBoardCardBody = new DashBoardCardBody();
            Date todayZero = MyUtils.Time.getTodayZero();
            Date yesterdayZero = MyUtils.Time.getYesterdayZero();
            Long newNum = noteService.count(new QueryWrapper<sit.zlx.enotebackend.domain.Note>().ge("createdAt", todayZero));
            Long oldNum = noteService.count(new QueryWrapper<sit.zlx.enotebackend.domain.Note>().between("createdAt", yesterdayZero, todayZero));
            dashBoardCardBody.setNum(newNum);
            dashBoardCardBody.setPercentValue(oldNum, newNum);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取笔记日新增数成功！", dashBoardCardBody));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取笔记日新增数失败！", null));
        }
    }

    @GetMapping("/dashboard/note/weekly/new")
    public ResponseDTO<BarChartDataBody> weeklyNewNote() {
        try {
            BarChartDataBody barChartDataBody = new BarChartDataBody();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date lastWeekZero = MyUtils.Time.getLastWeekZero();
            List<BarChartDataBody.BarChartDataItem> data = new java.util.ArrayList<>(noteService.list(new QueryWrapper<sit.zlx.enotebackend.domain.Note>().select("createdAt")).stream().map(sit.zlx.enotebackend.domain.Note::getCreatedAt).filter(Objects::nonNull).filter(date -> date.after(lastWeekZero)).map(sdf::format).distinct().map(date -> {
                String nextDayZero = date.substring(0, 8) + (Integer.parseInt(date.substring(8)) + 1);
                BarChartDataBody.BarChartDataItem item = new BarChartDataBody.BarChartDataItem();
                item.setDate(date.substring(5).replace("-", "/"));
                item.setNum(noteService.count(new QueryWrapper<sit.zlx.enotebackend.domain.Note>().between("createdAt", date, nextDayZero)));
                return item;
            }).toList());

            adminService.formatAndSortBarChartData(barChartDataBody, sdf, data);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取笔记周新增数成功！", barChartDataBody));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取笔记周新增数失败！", null));
        }
    }


    @GetMapping("/dashboard/note/daily/active")
    public ResponseDTO<DashBoardCardBody> dailyActiveNote() {
        try {
            DashBoardCardBody dashBoardCardBody = new DashBoardCardBody();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String today = sdf.format(new Date());
            String yesterday = sdf.format(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
            Long newNum = activeNoteService.count(new QueryWrapper<ActiveNote>().eq("lastModifiedTime", today));
            Long oldNum = activeNoteService.count(new QueryWrapper<ActiveNote>().eq("lastModifiedTime", yesterday));
            dashBoardCardBody.setNum(newNum);
            dashBoardCardBody.setPercentValue(oldNum, newNum);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取笔记日活跃数成功！", dashBoardCardBody));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取笔记日活跃数失败！", null));
        }
    }


    @GetMapping("/dashboard/note/weekly/active")
    public ResponseDTO<BarChartDataBody> weeklyActiveNote() {
        try {
            BarChartDataBody barChartDataBody = new BarChartDataBody();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date lastWeek = new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000);
            // 获取过去7天的数据，如果那一天没有数据，也要加到返回结果中，如果有数据，但是为0，也要加到返回结果中
            List<BarChartDataBody.BarChartDataItem> data = new java.util.ArrayList<>(activeNoteService.list(new QueryWrapper<ActiveNote>().select("lastModifiedTime")).stream().map(ActiveNote::getLastModifiedTime).filter(Objects::nonNull).filter(date -> date.after(lastWeek)).map(sdf::format).distinct().map(date -> {
                BarChartDataBody.BarChartDataItem item = new BarChartDataBody.BarChartDataItem();
                item.setDate(date.substring(5).replace("-", "/"));
                item.setNum(activeUserService.count(new QueryWrapper<ActiveUser>().eq("loginDate", date)));
                return item;
            }).toList());

            // 补全过去7天（包括今天）的数据，不能出现两个相同的日期
            adminService.formatAndSortBarChartData(barChartDataBody, sdf, data);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取用户周活跃数成功！", barChartDataBody));

        } catch (Exception e) {

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取用户周活跃数失败！", null));
        }
    }

    @GetMapping("/dashboard/file/total/image")
    public ResponseDTO<DashBoardCardBody> totalImage() {
        try {
            DashBoardCardBody dashBoardCardBody = new DashBoardCardBody();
            Date todayZero = MyUtils.Time.getTodayZero();
            Long newNum = fileService.count(new QueryWrapper<File>().eq("type", "image"));
            Long oldNum = fileService.count(new QueryWrapper<File>().eq("type", "image").le("uploadAt", todayZero));
            dashBoardCardBody.setNum(newNum);
            dashBoardCardBody.setPercentValue(oldNum, newNum);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取图片总数成功！", dashBoardCardBody));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取图片总数失败！", null));
        }
    }

    @GetMapping("/dashboard/file/total/video")
    public ResponseDTO<DashBoardCardBody> totalVideo() {
        try {
            DashBoardCardBody dashBoardCardBody = new DashBoardCardBody();
            Date todayZero = MyUtils.Time.getTodayZero();
            Long newNum = fileService.count(new QueryWrapper<File>().eq("type", "video"));
            Long oldNum = fileService.count(new QueryWrapper<File>().eq("type", "video").le("uploadAt", todayZero));
            dashBoardCardBody.setNum(newNum);
            dashBoardCardBody.setPercentValue(oldNum, newNum);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取视频总数成功！", dashBoardCardBody));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取视频总数失败！", null));
        }
    }

    @GetMapping("/dashboard/file/total/audio")
    public ResponseDTO<DashBoardCardBody> totalAudio() {
        try {
            DashBoardCardBody dashBoardCardBody = new DashBoardCardBody();
            Date todayZero = MyUtils.Time.getTodayZero();
            Long newNum = fileService.count(new QueryWrapper<File>().eq("type", "audio"));
            Long oldNum = fileService.count(new QueryWrapper<File>().eq("type", "audio").le("uploadAt", todayZero));
            dashBoardCardBody.setNum(newNum);
            dashBoardCardBody.setPercentValue(oldNum, newNum);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取音频总数成功！", dashBoardCardBody));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取音频总数失败！", null));
        }
    }

    @GetMapping("/dashboard/usage")
    public ResponseDTO<UsageBody> usage() {
        try {
            UsageBody.Size totalSize = new UsageBody.Size();
            UsageBody.Size noteSize = new UsageBody.Size();

            noteSize.setParsedSize("100.00 MB");
            noteSize.setRawSize((long) (100 * 1024 * 1024));

            List<Long> imageSizes = fileService.list(new QueryWrapper<File>().eq("type", "image").select("size")).stream().map(File::getSize).toList();
            List<Long> videoSizes = fileService.list(new QueryWrapper<File>().eq("type", "video").select("size")).stream().map(File::getSize).toList();
            List<Long> audioSizes = fileService.list(new QueryWrapper<File>().eq("type", "audio").select("size")).stream().map(File::getSize).toList();
            String imageParsedTotalSize = MyUtils.File.sumItemSize(imageSizes);
            String videoParsedTotalSize = MyUtils.File.sumItemSize(videoSizes);
            String audioParsedTotalSize = MyUtils.File.sumItemSize(audioSizes);

            UsageBody.Size imageSize = adminService.getUsageSize(totalSize, imageSizes, imageParsedTotalSize);
            UsageBody.Size videoSize = adminService.getUsageSize(totalSize, videoSizes, videoParsedTotalSize);
            UsageBody.Size audioSize = adminService.getUsageSize(totalSize, audioSizes, audioParsedTotalSize);

            totalSize.setParsedSize(MyUtils.File.convertRawSize(totalSize.getRawSize()));
            UsageBody usageBody = new UsageBody(totalSize, noteSize, imageSize, videoSize, audioSize);

            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取使用情况成功！", usageBody));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取使用情况失败！", null));
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
        private String id;
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
        private List<String> ids;
    }

    @Data
    @NoArgsConstructor
    public static class DashBoardCardBody {
        private Long num;
        private String percent;

        public void setPercentValue(Long oldNum, Long newNum) {
            if (oldNum > newNum) {
                this.percent = "-" + String.format("%.2f", (double) (oldNum - newNum) / oldNum * 100) + "%";
            } else if (oldNum < newNum) {
                if (oldNum == 0) {
                    this.percent = "100.00%";
                    return;
                }

                this.percent = String.format("%.2f", (double) (newNum - oldNum) / oldNum * 100) + "%";
            } else {
                this.percent = "0.00%";
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageBody {
        private Size total;
        private Size note;
        private Size image;
        private Size video;
        private Size audio;

        @Data
        @NoArgsConstructor
        public static class Size {
            private String parsedSize;
            private Long rawSize;
        }
    }

    @Data
    @NoArgsConstructor
    public static class BarChartDataBody {
        private List<BarChartDataItem> data;

        @Data
        @NoArgsConstructor
        public static class BarChartDataItem {
            // 示例数据：
            // {
            //     "date": "09/01",
            //     "num": 10
            // }
            private String date;
            private Long num;
        }
    }
}

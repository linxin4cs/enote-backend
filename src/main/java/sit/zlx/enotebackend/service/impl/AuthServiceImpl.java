package sit.zlx.enotebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import sit.zlx.enotebackend.domain.User;
import sit.zlx.enotebackend.service.AuthService;
import sit.zlx.enotebackend.service.UserService;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static sit.zlx.enotebackend.service.MyUtils.generateRandomString;
import static sit.zlx.enotebackend.service.impl.UserServiceImpl.ROLE_LIST;


@Service
public class AuthServiceImpl implements AuthService {

    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserService userService;
    private final MailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private final HashMap<String, String> emailSessions = new HashMap<String, String>();
    @Value("${spring.mail.properties.from}")
    private String from;

    @Autowired
    AuthServiceImpl(UserService userService, MailSender mailSender, StringRedisTemplate redisTemplate) {
        this.userService = userService;
        this.mailSender = mailSender;
        this.redisTemplate = redisTemplate;
    }

    // 使用邮箱登录
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null) {
            throw new UsernameNotFoundException("邮箱不能为空！");
        }

        User user = userService.getOne(new QueryWrapper<User>().eq("email", email));

        if (user == null) {
            throw new UsernameNotFoundException("邮箱或密码错误！");
        }

        if (user.getIsDeleting() == 1) {
            throw new AccessDeniedException("未授权访问！");
        }

        String password = user.getPassword();
        String role = ROLE_LIST.get(user.getRole());
        int status = user.getStatus();

        if (status == 0) {
            throw new DisabledException("账号已被禁用！");
        }

        return org.springframework.security.core.userdetails.User.withUsername(email)
                .password(password)
                .roles(role)
                .build();
    }

    /**
     * 1. 生成验证码
     * 2. 发送邮箱，如果发送成功则插入到 Redis 中，键为邮箱，值为验证码
     * 3. 验证码有效期为三分钟，如果在此时重新请求发送验证码，只有在剩余时间低于两分钟时才会重新发送，重复此流程。
     * 4. 在用户注册时，从 Redis 中取出相应的键值对进行验证。
     */
    @Override
    public String sendCode(String email, String sessionId, String actionKey) {
        String key = "email:" + sessionId + ":" + email + ":" + actionKey;
        Map<String, String> ACTION_MAP = new HashMap<>() {{
            put("register", "注册");
            put("reset", "重置密码");
            put("changeOldEmail", "更换邮箱");
            put("changeNewEmail", "更换邮箱");
            put("changePassword", "修改密码");
        }};

        synchronized (emailSessions) {
            if (emailSessions.containsKey(key)) {
                return "请求频繁，请稍后再试！";
            } else {
                emailSessions.put(key, "");
            }
        }

        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            Long expire = Optional.ofNullable(redisTemplate.getExpire(key, TimeUnit.SECONDS)).orElse(0L);

            if (expire > 120) {
                return "请求频繁，请稍后再试！";
            }
        }

        User user = userService.getOne(new QueryWrapper<User>().eq("email", email));

        if (actionKey.equals("reset") || actionKey.equals("changePassword") || actionKey.equals("changeOldEmail")) {
            if (user == null) {
                return "该邮箱未注册";
            }
        }

        if (actionKey.equals("register") || actionKey.equals("changeNewEmail")) {
            if (user != null) {
                if (user.getIsDeleting() == 1)
                    return "请等待用户数据删除完成！";

                return "该邮箱已被注册";
            }
        }

        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        String text = ("您正在" + ACTION_MAP.get(actionKey) + "，验证码为：" + code + "，有效期为三分钟。");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("ENote-验证码");
        message.setText(text);

        try {
            mailSender.send(message);
            redisTemplate.opsForValue().set(key, String.valueOf(code), 3, TimeUnit.MINUTES);

            return null;
        } catch (MailException mailException) {
            return "验证码发送失败，请联系管理员！";
        } finally {
            emailSessions.remove(key);
        }
    }

    @Override
    public String register(String password, String email, String code, String sessionId) {
        String key = "email:" + sessionId + ":" + email + ":register";

        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            String val = redisTemplate.opsForValue().get(key);

            if (val == null) {
                redisTemplate.delete(key);
                return "验证码已过期，请重新请求！";
            } else {
                if (val.equals(code)) {
                    redisTemplate.delete(key);
                    password = passwordEncoder.encode(password);

                    User newUser = new User();
                    newUser.setId(UUID.randomUUID().toString());
                    newUser.setEmail(email);

                    String name;

                    do {
                        name = "user" + generateRandomString(8);

                    } while (userService.getOne(new QueryWrapper<User>().eq("name", name)) != null);

                    newUser.setName(name);
                    newUser.setPassword(password);
                    newUser.setStatus(1);

                    if (userService.count() == 0) {
                        newUser.setRole(2);
                    }

                    if (userService.save(newUser)) {
                        return null;
                    } else {
                        return "服务器内部错误，请联系管理员！";
                    }
                } else {
                    return "验证码错误！";
                }
            }
        } else {
            return "请先请求一封验证码邮件！";
        }
    }

    @Override
    public String validateCode(String email, String code, String sessionId, String actionKey) {
        String key = "email:" + sessionId + ":" + email + ":" + actionKey;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            String val = redisTemplate.opsForValue().get(key);

            if (val == null) {
                redisTemplate.delete(key);
                return "验证码已过期，请重新请求！";
            } else {
                if (val.equals(code)) {
                    redisTemplate.delete(key);
                    return null;
                } else {
                    return "验证码错误！";
                }
            }
        } else {
            return "请先发送一封验证码邮件！";
        }
    }

    @Override
    public Boolean resetPassword(String email, String password) {
        User user = userService.getOne(new QueryWrapper<User>().eq("email", email));
        user.setPassword(passwordEncoder.encode(password));

        return userService.updateById(user);
    }
}

package sit.zlx.enotebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import sit.zlx.enotebackend.domain.User;
import sit.zlx.enotebackend.service.UserService;
import sit.zlx.enotebackend.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
* @author linxin4cs
* @description 针对表【user】的数据库操作Service实现
* @createDate 2024-03-26 10:01:15
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{
    public static final ArrayList<String> ROLE_LIST = new ArrayList<>() {{
        add("USER");
        add("ADMIN");
        add("SUPER_ADMIN");
    }};
}





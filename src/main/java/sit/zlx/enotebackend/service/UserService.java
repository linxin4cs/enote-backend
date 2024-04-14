package sit.zlx.enotebackend.service;

import sit.zlx.enotebackend.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.ArrayList;

/**
* @author linxin4cs
* @description 针对表【user】的数据库操作Service
* @createDate 2024-04-15 01:47:34
*/
public interface UserService extends IService<User> {
    ArrayList<String> ROLE_LIST = new ArrayList<>() {{
        add("USER");
        add("ADMIN");
        add("SUPER_ADMIN");
    }};
}

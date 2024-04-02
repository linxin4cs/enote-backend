package sit.zlx.enotebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import sit.zlx.enotebackend.domain.User;
import sit.zlx.enotebackend.mapper.UserMapper;
import sit.zlx.enotebackend.service.UserService;

import java.util.ArrayList;

/**
 * @author linxin4cs
 * @description 针对表【user】的数据库操作Service实现
 * @createDate 2024-03-29 14:34:32
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

}





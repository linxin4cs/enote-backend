package sit.zlx.enotebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import sit.zlx.enotebackend.domain.User;
import sit.zlx.enotebackend.service.UserService;
import sit.zlx.enotebackend.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author linxin4cs
* @description 针对表【user】的数据库操作Service实现
* @createDate 2024-04-15 01:47:34
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}





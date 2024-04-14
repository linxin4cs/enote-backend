package sit.zlx.enotebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import sit.zlx.enotebackend.domain.Tag;
import sit.zlx.enotebackend.service.TagService;
import sit.zlx.enotebackend.mapper.TagMapper;
import org.springframework.stereotype.Service;

/**
* @author linxin4cs
* @description 针对表【tag】的数据库操作Service实现
* @createDate 2024-04-11 03:06:36
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

}





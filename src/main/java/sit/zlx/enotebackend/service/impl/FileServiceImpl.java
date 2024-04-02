package sit.zlx.enotebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import sit.zlx.enotebackend.domain.File;
import sit.zlx.enotebackend.service.FileService;
import sit.zlx.enotebackend.mapper.FileMapper;
import org.springframework.stereotype.Service;

/**
* @author linxin4cs
* @description 针对表【file】的数据库操作Service实现
* @createDate 2024-04-02 16:12:24
*/
@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File>
    implements FileService{

}





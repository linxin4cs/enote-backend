package sit.zlx.enotebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import sit.zlx.enotebackend.domain.ActiveNote;
import sit.zlx.enotebackend.service.ActiveNoteService;
import sit.zlx.enotebackend.mapper.ActiveNoteMapper;
import org.springframework.stereotype.Service;

/**
* @author linxin4cs
* @description 针对表【activeNote】的数据库操作Service实现
* @createDate 2024-04-11 03:41:03
*/
@Service
public class ActiveNoteServiceImpl extends ServiceImpl<ActiveNoteMapper, ActiveNote>
    implements ActiveNoteService{

}





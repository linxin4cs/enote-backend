package sit.zlx.enotebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import sit.zlx.enotebackend.domain.Note;
import sit.zlx.enotebackend.service.NoteService;
import sit.zlx.enotebackend.mapper.NoteMapper;
import org.springframework.stereotype.Service;

/**
* @author linxin4cs
* @description 针对表【note】的数据库操作Service实现
* @createDate 2024-04-11 03:07:04
*/
@Service
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note>
    implements NoteService{

}





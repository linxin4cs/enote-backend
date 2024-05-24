package sit.zlx.enotebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import sit.zlx.enotebackend.domain.Folder;
import sit.zlx.enotebackend.mapper.FolderMapper;
import sit.zlx.enotebackend.service.FolderService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author linxin4cs
 * @description 针对表【folder】的数据库操作Service实现
 * @createDate 2024-05-03 02:20:53
 */
@Service
public class FolderServiceImpl extends ServiceImpl<FolderMapper, Folder>
        implements FolderService {


    @Override
    public void updateUpdatedAt(String folderId) {
        // 向上寻找该文件夹的父文件夹，直至根文件夹，将它们放到一个列表中
        List<String> folderIds = new ArrayList<>();
        this.getAllParentfolderIdsHelper(folderId, folderIds);
        // 更新这些文件夹的更新时间
        for (String id : folderIds) {
            Folder folder = this.getById(id);
            folder.setUpdatedAt(new Date());
            this.updateById(folder);
        }


    }

    @Override
    public void getAllParentfolderIdsHelper(String parentId, List<String> folderIds) {
        if(parentId == null){
            return;
        }

        folderIds.add(parentId);
        Folder folder = this.getById(parentId);
        getAllParentfolderIdsHelper(folder.getParentId(), folderIds);
    }


}





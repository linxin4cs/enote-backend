package sit.zlx.enotebackend.service;

import sit.zlx.enotebackend.domain.Folder;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author linxin4cs
* @description 针对表【folder】的数据库操作Service
* @createDate 2024-05-03 02:20:53
*/
public interface FolderService extends IService<Folder> {
    public void updateUpdatedAt(String folderId);

    public void getAllParentfolderIdsHelper(String parentId, List<String> folderIds);
}

package sit.zlx.enotebackend.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sit.zlx.enotebackend.domain.SearchHistoryDoc;
import sit.zlx.enotebackend.domain.User;
import sit.zlx.enotebackend.dto.ResponseDTO;
import sit.zlx.enotebackend.repository.SearchHistoryDocRepository;
import sit.zlx.enotebackend.service.UserService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/search")
public class UserSearchController {
    private final UserService userService;
    private final SearchHistoryDocRepository searchHistoryDocRepository;

    @Autowired
    public UserSearchController(SearchHistoryDocRepository searchHistoryDocRepository, UserService userService) {
        this.searchHistoryDocRepository = searchHistoryDocRepository;
        this.userService = userService;
    }

    @GetMapping("/history")
    public ResponseDTO<SearchHistoryDoc> getSearchHistory(@AuthenticationPrincipal UserDetails currentUser) {
        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));

            Optional<SearchHistoryDoc> searchHistoryDocOptional = searchHistoryDocRepository.findById(user.getId());
            SearchHistoryDoc searchHistoryDoc = searchHistoryDocOptional.orElseGet(() -> {
                SearchHistoryDoc newSearchHistoryDoc = new SearchHistoryDoc();
                newSearchHistoryDoc.setId(user.getId());
                newSearchHistoryDoc.setKeywords(List.of());
                searchHistoryDocRepository.save(newSearchHistoryDoc);
                return newSearchHistoryDoc;
            });
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("获取搜索历史成功！", searchHistoryDoc));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("获取搜索历史失败！", null));
        }
    }

    @DeleteMapping("/history")
    public ResponseDTO<?> deleteSearchHistory(@AuthenticationPrincipal UserDetails currentUser) {
        try {
            User user = userService.getOne(new QueryWrapper<User>().eq("email", currentUser.getUsername()));

            searchHistoryDocRepository.deleteById(user.getId());
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.SUCCESS.getCode(), new ResponseDTO.ResponseData<>("删除搜索历史成功！", null));
        } catch (Exception e) {
            return new ResponseDTO<>(ResponseDTO.STATUS_CODE.INTERNAL_SERVER_ERROR.getCode(), new ResponseDTO.ResponseData<>("删除搜索历史失败！", null));
        }
    }
}

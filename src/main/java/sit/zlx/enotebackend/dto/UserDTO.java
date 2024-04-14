package sit.zlx.enotebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sit.zlx.enotebackend.domain.User;

import java.util.Date;

@Getter
@NoArgsConstructor  // 添加这个注解来生成无参构造器
@AllArgsConstructor
@Builder
public class UserDTO {
    private String id;
    private String email;
    private String name;
    private Object status;
    private Object role;
    private Date createdAt;
    private String usage;
    private String avatar;


    public static UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .status(user.getStatus())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .avatar(user.getAvatar())
                .build();
    }
}

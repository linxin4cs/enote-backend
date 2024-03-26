package sit.zlx.enotebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor  // 添加这个注解来生成无参构造器
@AllArgsConstructor
public class RequestDTO<T> {
    private T data;
}

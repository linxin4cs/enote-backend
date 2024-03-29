package sit.zlx.enotebackend.service;

import java.security.SecureRandom;
import java.util.Base64;

public class MyUtils {
    public static String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static class Validator {
        public static String validateEmail(String email) {
            if (email == null || email.isEmpty()) {
                return "邮箱不能为空";
            } else if (!email.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
                return "邮箱格式错误";
            }

            return "";
        }

        public static String validatePassword(String password) {
            if (password == null || password.isEmpty()) {
                return "密码不能为空";
            } else if (password.length() < 8 || password.length() > 20) {
                return "密码长度应在8-20位之间";
            } else if (!password.matches("^[a-zA-Z0-9]+$")) {
                return "密码只能包含英文和数字";
            }

            return "";
        }

        public static String validateName(String name) {
            if (name == null || name.isEmpty()) {
                return "用户名不能为空";
            } else if (name.length() < 2 || name.length() > 16) {
                return "用户名长度应在2-16位之间";
            } else if (!name.matches("^[a-zA-Z0-9\\u4e00-\\u9fa5]+$")) {
                return "用户名只能包含中文、英文和数字";
            }

            return "";
        }
    }
}

package sit.zlx.enotebackend.service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class MyUtils {
    public static String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length()); // 生成一个随机索引
            char randomChar = characters.charAt(index); // 通过索引获取字符
            result.append(randomChar); // 将字符追加到最终字符串
        }

        return result.toString();
    }

    public static class Validator {
        public static String validateEmail(String email) {
            if (email == null || email.isEmpty()) {
                return "邮箱不能为空!";
            } else if (!email.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
                return "邮箱格式错误!";
            }

            return "";
        }

        public static String validatePassword(String password) {
            if (password == null || password.isEmpty()) {
                return "密码不能为空!";
            } else if (password.length() < 8 || password.length() > 20) {
                return "密码长度应在8-20位之间!";
            } else if (!password.matches("^[a-zA-Z0-9]+$")) {
                return "密码只能包含英文和数字!";
            }

            return "";
        }

        public static String validateName(String name) {
            if (name == null || name.isEmpty()) {
                return "用户名不能为空!";
            } else if (name.length() < 2 || name.length() > 16) {
                return "用户名长度应在2-16位之间!";
            } else if (!name.matches("^[a-zA-Z0-9\\u4e00-\\u9fa5]+$")) {
                return "用户名只能包含中文、英文和数字!";
            }

            return "";
        }

        public static String validateRole(int role) {
            if (role != 0 && role != 1) {
                return "角色只能是 普通用户/管理员!";
            }

            return "";
        }

        public static String validateStatus(int status) {
            if (status != 0 && status != 1) {
                return "状态只能是 启用/禁用!";
            }

            return "";
        }
    }

    public static class Time {
        public static Date getTodayZero() {
            Calendar calendar = Calendar.getInstance();
            calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
            return calendar.getTime();
        }

        public static Date getYesterdayZero() {
            Calendar calendar = Calendar.getInstance();
            calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH) - 1, 0, 0, 0);
            return calendar.getTime();
        }

        public static Date getLastWeekZero() {
            Calendar calendar = Calendar.getInstance();
            calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH) - 7, 0, 0, 0);
            return calendar.getTime();
        }
    }

    public static class File {
        public static String convertRawSize(Long rawSize) {
            String[] units = {"B", "KB", "MB", "GB", "TB"};
            int index = 0;
            double size = rawSize.doubleValue();

            while (size >= 1024 && index < units.length - 1) {
                size /= 1024;
                index++;
            }

            return String.format("%.2f %s", size, units[index]);
        }

        public static String sumItemSize(List<Long> itemRawSize) {
            Long sum = 0L;
            for (Long rawSize : itemRawSize) {
                sum += rawSize;
            }
            return convertRawSize(sum);
        }
    }
}

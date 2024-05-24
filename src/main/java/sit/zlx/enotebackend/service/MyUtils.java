package sit.zlx.enotebackend.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;

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

    public static UsageBody.Size getUsageSize(UsageBody.Size totalSize, List<Long> sizes, String parsedTotalSize) {
        UsageBody.Size size = new UsageBody.Size();
        size.setParsedSize(parsedTotalSize);

        if (totalSize.getRawSize() == null) {
            totalSize.setRawSize(0L);  // 如果 rawSize 是 null，则初始化为 0
        }

        // 对 size 的 rawSize 做同样处理
        if (size.getRawSize() == null) {
            size.setRawSize(0L);  // 如果 rawSize 是 null，则初始化为 0
        }

        for (Long sizeLong : sizes) {
            if (sizeLong != null) {  // 这里也检查 sizeLong 是否为 null
                totalSize.setRawSize(totalSize.getRawSize() + sizeLong);
                size.setRawSize(size.getRawSize() + sizeLong);
            }
        }

        return size;
    }

    public static Long calculateWordCount(String text) {
        String[] parts = text.split("[一-龥]");
        long wordCount = 0;
        for (String part : parts) {
            if (!part.isEmpty()) {
                wordCount += part.split("\\s+").length;
            }
        }

        long chineseCount = text.replaceAll("[^\\u4e00-\\u9fa5]", "").length();

        return (wordCount + chineseCount);
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

        public static String validateNoteTitle(String title) {
            if (title == null || title.isEmpty()) {
                return "标题不能为空!";
            } else if (title.length() < 2 || title.length() > 16) {
                return "标题长度应在2-16位之间!";
            } else if (!title.matches("^[a-zA-Z0-9\\u4e00-\\u9fa5]+$")) {
                return "标题不能包含特殊字符!";
            }

            return "";
        }

        public static String validateFolderName(String name) {
            if (name == null || name.isEmpty()) {
                return "文件夹名不能为空!";
            } else if (name.length() < 2 || name.length() > 16) {
                return "文件夹名长度应在2-16位之间!";
            } else if (!name.matches("^[a-zA-Z0-9\\u4e00-\\u9fa5]+$")) {
                return "文件夹名不能包含特殊字符!";
            }

            return "";
        }

        public static String validateTagName(String name) {
            if (name == null || name.isEmpty()) {
                return "标签名不能为空!";
            } else if (name.length() < 2 || name.length() > 16) {
                return "标签名长度应在2-16位之间!";
            } else if (!name.matches("^[a-zA-Z0-9\\u4e00-\\u9fa5]+$")) {
                return "标签名不能包含特殊字符!";
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

    public static class NoteContentDiffer {
        public static Set<String> extractServerFileIds(String htmlContent) {
            Set<String> fileIds = new HashSet<>();
            Document doc = Jsoup.parse(htmlContent);
            Elements sources = doc.select("video source[src], audio source[src], img[src]");

            for (Element source : sources) {
                String src = source.attr("src");
                if (src.contains("/api/file/")) {
                    // 提取ID（假设ID为URL路径中的最后一个部分）
                    String fileId = src.substring(src.lastIndexOf("/") + 1);
                    fileIds.add(fileId);
                }
            }

            return fileIds;
        }

        public static Set<String> compareServerFileIds(String oldHtml, String newHtml) {
            Set<String> oldFileIds = extractServerFileIds(oldHtml);
            Set<String> newFileIds = extractServerFileIds(newHtml);

            // 计算旧HTML中存在但新HTML中不存在的文件ID
            oldFileIds.removeAll(newFileIds);
            return oldFileIds;
        }
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageBody {
        private UsageBody.Size total;
        private UsageBody.Size note;
        private UsageBody.Size image;
        private UsageBody.Size video;
        private UsageBody.Size audio;

        @Data
        @NoArgsConstructor
        public static class Size {
            private String parsedSize;
            private Long rawSize;
        }
    }
}

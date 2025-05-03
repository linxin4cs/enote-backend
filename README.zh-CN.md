# ENote-Backend
[English](./README.md) | 中文

本代码仓库包含云笔记平台项目的后端源代码，作为本科毕业设计的一部分开发。本后端服务负责云笔记应用的核心业务逻辑、数据持久化以及 API 接口的提供。


## 项目简介

本后端服务使用 Java 和 Spring Boot 3 构建，采用前后端分离架构，负责处理所有业务逻辑，包括用户认证、笔记管理、数据存储与检索，并为前端应用提供 RESTful API 接口。系统同时使用多种数据库（MySQL、MongoDB、Redis），以优化不同类型数据的存储和访问效率。

## 主要功能

* **用户认证与授权：**
  * 提供用户注册、登录、登出接口；
  * 密码加密存储与验证；
  * 实现“记住我”功能（基于 Spring Security `persistent_logins`）；
  * 实现密码重置流程（包括邮箱验证码发送与校验）；
  * 基于角色的访问控制（普通用户、管理员、超级管理员）；
  * 用户账户启用/禁用状态管理；
  * 基于 Spring Security 的安全防护。

* **笔记管理：**
  * 提供笔记的创建、读取、更新、删除（CRUD）操作接口；
  * 支持富文本内容的存储（纯文本和 HTML 分别存储在 MongoDB 中）；
  * 实现笔记的自动/手动保存逻辑；
  * 提供分页查询笔记列表接口，并支持按文件夹、标签、收藏状态、关键字过滤；
  * 实现模糊关键字搜索（不区分大小写）；
  * 提供收藏/取消收藏、移动、添加/删除标签等操作接口。

* **文件夹与标签管理：**
  * 提供文件夹和标签的 CRUD 接口；
  * 提供移动文件夹的接口。

* **文件管理：**
  * 提供用户头像及笔记内嵌媒体文件（图片/视频/音频）的上传与访问接口；
  * 控制文件访问权限；
  * 实现与笔记内容关联文件的自动清理逻辑。

* **用户管理（管理员功能）：**
  * 提供管理员接口用于查询、编辑用户信息（权限、状态）及删除用户；
  * 权限校验机制，防止低权限管理员修改高权限管理员。

* **数据统计与分析（管理员功能）：**
  * 提供用户、笔记、文件等数据的统计接口（包括总量、增量、活跃度、存储使用量等）。

* **平台数据维护（超级管理员功能）：**
  * 提供数据备份与恢复的基础接口（实现较为简化）。

* **通用功能：**
  * 全局异常处理；
  * 统一的 API 响应格式；
  * 使用 MyBatis-Plus 简化数据库操作；
  * 邮件发送服务（用于验证码与通知）；
  * 利用 Redis 存储验证码及缓存会话/token 信息。

## 技术栈

* **核心框架：** Spring Boot 3.x  
* **开发语言：** Java 17+  
* **安全机制：** Spring Boot Security  
* **数据库持久层：** MyBatis-Plus  
* **使用数据库：**  
  * MySQL：存储结构化数据（如用户、文件夹、标签、笔记元数据、文件元数据）  
  * MongoDB：存储文档类数据（如笔记内容、搜索记录）  
  * Redis：存储键值类缓存（如验证码、会话信息）  
* **邮件服务：** Spring Boot Mail  
* **构建工具：** Maven
* **API 风格：** RESTful  
* **常用依赖：** Lombok（用于简化样板代码）等

## 架构概览

本项目采用经典的三层架构设计（Controller、Service、Mapper/DAO）：

* **Controller 层：** 接收 HTTP 请求，调用 Service 处理逻辑，返回 JSON 响应；
* **Service 层：** 实现核心业务逻辑，进行数据校验、事务控制，并调用 Mapper 访问数据库；
* **Mapper/DAO 层：** 通过 MyBatis-Plus 操作 MySQL，同时提供 MongoDB 和 Redis 的操作接口（使用 Spring Data）。

数据库职责划分：

* **MySQL：** 存储结构化数据，支持事务一致性；
* **MongoDB：** 存储笔记富文本和搜索历史，发挥文档数据库灵活性；
* **Redis：** 存储临时数据（如验证码），提高访问效率并缓解主数据库压力。

部署时，建议使用 Nginx 作为反向代理。数据库和 Redis 可通过 Docker 管理。

## 项目结构

```
enotebackend/
├── src/
│   └── main/
│      ├── java/
│      │   └── com/yourpackage/enote/ # 根包
│      │       ├── EnoteBackendApplication.java # 启动类
│      │       ├── config/ # 配置类（安全、数据库、Web等）
│      │       ├── controller/ # 控制器
│      │       ├── domain/ # 实体类（或 entity）
│      │       ├── dto/ # 请求/响应 DTO
│      │       ├── enums/ # 枚举类
│      │       ├── interceptor/ # 拦截器/过滤器
│      │       ├── mapper/ # MyBatis Mapper 接口
│      │       ├── repository/ # MongoDB Repository
│      │       ├── service/ # 服务接口
│      │       │   └── impl/ # 服务实现
│      │       └── utils/ # 工具类
│      └── resources/
│          ├── application.yml # 配置文件
│          └── mapper/ # MyBatis XML 映射文件
├── pom.xml # Maven 配置文件
└── ... # 其他文件（如 .gitignore）
```

## 本地运行

**前提条件：**

* JDK 17+
* Maven 3.6+ 或 Gradle 7.x+
* MySQL 服务（如 8.0）
* MongoDB 服务（如 5.x 或 6.x）
* Redis 服务（如 6.x）
* 可选：Docker 和 Docker Compose（用于快速部署数据库）

**安装与启动步骤：**

1. **克隆项目仓库：**
    ```bash
    git clone git@github.com:linxin4cs/enote-backend.git
    cd enote-backend
    ```

2. **数据库配置：**
    * 确保 MySQL、MongoDB 和 Redis 均已启动；
    * **MySQL：** 创建数据库 `enote`，执行初始化 SQL 脚本（如有）创建表，或启用 MyBatis-Plus 的自动建表（不推荐用于生产）；
    * **MongoDB：** 无需手动创建集合，首次写入时自动生成；
    * **Redis：** 确保服务可连接。

3. **配置应用程序：**
    * 修改 `application.yml` 文件：
      ```yaml
      server:
        port: 8080

      spring:
        datasource:
          url: jdbc:mysql://localhost:3306/enote?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
          username: your_mysql_user
          password: your_mysql_password
          driver-class-name: com.mysql.cj.jdbc.Driver

        data:
          mongodb:
            uri: mongodb://localhost:27017/enote_notes
          redis:
            host: localhost
            port: 6379
            # password: your_redis_password

        mail:
          host: smtp.example.com
          port: 587
          username: your_email@example.com
          password: your_email_password_or_app_code
          properties:
            mail:
              smtp:
                auth: true
                starttls:
                  enable: true
      ```

4. **构建项目（可选）：**
    ```bash
    mvn clean package -DskipTests
    # 或 Gradle:
    # ./gradlew build -x test
    ```

5. **运行项目：**
    ```bash
    mvn spring-boot:run
    # 或使用 jar 包：
    # java -jar target/enotebackend-0.0.1-SNAPSHOT.jar
    ```
    默认服务启动地址为：`http://localhost:8080`

## API 文档

API 遵循 RESTful 设计风格，所有可用接口定义在 `src/main/java/.../controller/` 目录下。您可以使用 Postman、Apifox 等工具进行测试。

前端项目地址：[ENote-Frontend](https://github.com/linxin4cs/enote-frontend)
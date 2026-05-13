# external-api-foundation

`external-api-foundation` 是一个面向外部 API 接入场景的 Spring Boot 基础工程。它不是业务模板生成器，而是把外部接口服务常见的横切能力沉淀成可复用基础设施：统一响应、错误码、鉴权、签名防重放、加解密扩展、幂等、分布式锁、防重复提交、出入站日志、HTTP 客户端封装、分页模型和 MyBatis-Plus 基础配置。

## 技术栈

| 能力 | 选型 |
| --- | --- |
| JDK | Java 21 |
| 框架 | Spring Boot 3.3.5 |
| Web | Spring MVC |
| 参数校验 | Jakarta Validation |
| 数据库 | MySQL |
| ORM | MyBatis-Plus 3.5.16 |
| Redis | Spring Data Redis |
| 分布式锁 | Redisson 3.52.0 |
| JWT | JJWT 0.13.0 |
| 接口文档 | Knife4j 4.5.0 |
| 日志 | SLF4J + Logback + MDC |
| 健康检查 | Spring Boot Actuator |

## 已实现能力

- 统一响应：`ApiResponse`
- 统一错误码：`ErrorCode`
- 全局异常处理：`GlobalExceptionHandler`
- 统一分页：`PageQuery`、`PageResult`、`PageUtils`
- JWT 鉴权：`JwtAuthFilter`、`JwtTokenProvider`
- 当前用户上下文：`CurrentUserContext`
- 请求/响应日志：TraceId、访问日志、请求体/响应体脱敏和截断
- 出站 HTTP 客户端：GET、POST JSON、POST FORM、自定义请求头、回调、拦截器
- 防重复提交：`@NoRepeatSubmit`
- Redis 幂等服务骨架：`IdempotencyService`
- 分布式锁：`DistributedLockService`
- 请求签名与防重放：HMAC-SHA256 默认实现，默认关闭
- 请求解密与响应加密框架：Provider 扩展机制，默认关闭
- 定时任务基础配置：线程池、单机串行示例、分布式互斥示例
- MyBatis-Plus 基础配置：分页插件、乐观锁插件、逻辑删除、审计字段自动填充
- Knife4j/OpenAPI 配置
- 单元测试覆盖核心工具、HTTP 客户端、JWT、签名、加解密、分页、日志等模块

## 目录结构

```text
src/main/java/com/example/externalapi
├─ common                  # 通用响应、错误码、异常、分页
├─ config                  # Jackson、Knife4j、MyBatis-Plus、Redis 配置
├─ controller              # 示例接口
├─ dto                     # 接口入参/出参 DTO
├─ entity                  # MyBatis-Plus 实体
├─ mapper                  # Mapper Java 和 XML
├─ service                 # 示例业务服务
├─ task                    # 示例定时任务
└─ infrastructure          # 通用基础设施
   ├─ crypto               # 请求解密/响应加密框架
   │  ├─ config            # 加解密配置属性
   │  ├─ context           # 单次请求加解密上下文
   │  ├─ key               # 密钥解析接口与默认实现
   │  ├─ model             # 加密载荷、元数据、密钥模型
   │  ├─ provider          # 算法 Provider 扩展点与注册表
   │  └─ web               # 加解密 Filter 与请求体包装
   ├─ http                 # 出站 HTTP 客户端
   │  ├─ api               # 调用入口与回调接口
   │  ├─ config            # RestClient 和属性配置
   │  ├─ core              # 默认实现
   │  ├─ exception         # HTTP 客户端异常与错误类型
   │  ├─ interceptor       # 拦截器与上下文
   │  └─ support           # Header、URI 辅助工具
   ├─ idempotency          # 幂等服务
   ├─ lock                 # 分布式锁
   ├─ logging              # TraceId、访问日志、脱敏
   ├─ repeat               # 防重复提交
   ├─ scheduler            # 定时任务配置
   ├─ security             # JWT、签名防重放、当前用户
   └─ web                  # Web 辅助工具
```

`infrastructure/crypto` 已按职责拆分：

```text
config       # CryptoProperties
context      # CryptoContext
key          # CryptoKeyResolver、DefaultCryptoKeyResolver
model        # CryptoEnvelope、CryptoMetadata、CryptoKey
provider     # PayloadCryptoProvider、PayloadCryptoProviderRegistry、NoopPayloadCryptoProvider
web          # CryptoFilter、DecryptedBodyHttpServletRequest
```

`infrastructure/http` 已按职责拆分：

```text
api          # 调用入口与回调接口
config       # RestClient 和属性配置
core         # 默认实现
exception    # HTTP 客户端异常与错误类型
interceptor  # 拦截器与上下文
support      # Header、URI 辅助工具
```

## 本地启动

前置依赖：

- JDK 21
- Maven 3.9+
- MySQL
- Redis

创建示例表：

```sql
source src/main/resources/sql/demo_order.sql;
```

检查本地配置：

```yaml
# src/main/resources/application-dev.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/demo_service
    username: root
    password: 123456
  data:
    redis:
      host: localhost
      port: 6379
```

启动：

```bash
mvn spring-boot:run
```

打包：

```bash
mvn clean package -DskipTests
```

运行 Jar：

```bash
java -jar target/external-api-foundation-0.0.1-SNAPSHOT.jar
```

## 常用入口

```text
Knife4j:  http://localhost:8080/doc.html
OpenAPI:  http://localhost:8080/v3/api-docs
Health:   http://localhost:8080/actuator/health
```

示例接口：

```text
POST /auth/login
GET  /api/ping
POST /api/repeat-submit-demo
POST /api/example/orders
POST /api/example/orders/page
```

默认情况下 `/api/**` 需要 JWT。`/auth/login`、Knife4j、Actuator health/info 等路径已排除鉴权。

## 生成测试 Token

```bash
mvn -Dtest=JwtTokenProviderTest test
```

测试会打印 `Bearer ...`。在 Knife4j 的授权窗口中按页面提示填写；如果页面使用 HTTP Bearer 模式，通常只填写 JWT 本体，不需要 `Bearer ` 前缀。

## 统一响应与分页

统一响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "traceId": "abc123"
}
```

分页请求 DTO 继承 `PageQuery`：

```json
{
  "pageNo": 1,
  "pageSize": 10,
  "userId": 10001,
  "requestNo": "REQ202605070001"
}
```

分页响应统一返回 `PageResult<T>`，避免把 MyBatis-Plus 的 `Page/IPage` 暴露到接口协议中。

## 出站 HTTP 客户端

统一入口：

```java
HttpClientService httpClientService;
```

GET 路径变量：

```java
UserDTO user = httpClientService.get(
        "https://example.com/users/{id}",
        UserDTO.class,
        10001L);
```

GET query：

```java
Map<String, Object> query = new LinkedHashMap<>();
query.put("userId", 10001L);
query.put("status", "PAID");

PageDTO page = httpClientService.get(
        "https://example.com/orders",
        query,
        PageDTO.class);
```

自定义请求头：

```java
Map<String, String> headers = HttpHeadersBuilder.create()
        .bearerToken("jwt-token")
        .appId("partner-a")
        .header("X-Custom", "custom-value")
        .build();
```

POST JSON 回调：

```java
httpClientService.postJson(
        url,
        headers,
        request,
        ThirdPartyResponse.class,
        HttpClientCallbackAdapter.of(
                response -> updateSuccess(response),
                exception -> updateFailed(exception)
        ));
```

扩展拦截器：

```java
@Component
public class PartnerHttpClientInterceptor implements HttpClientInterceptor {

    @Override
    public void beforeRequest(HttpClientRequestContext context) {
        if (context.getUrl().contains("partner.example.com")) {
            context.getHeaders().put("X-Sign", sign(context));
        }
    }
}
```

普通调用失败会抛出 `HttpClientException`；回调调用失败会进入 `onFailure`，不再向外抛出该异常。

## 幂等与防重复提交

防重复提交适合短时间内防止重复点击或重复请求：

```java
@NoRepeatSubmit(seconds = 5)
```

业务幂等适合创建订单、支付回调、发放权益、外部系统重试等场景。示例请求：

```http
POST /api/example/orders
Authorization: Bearer <JWT>
Idempotency-Key: REQ202605070001
Content-Type: application/json

{
  "userId": 10001,
  "amount": 99.99
}
```

同一个 `Idempotency-Key` 搭配相同请求体会复用首次成功结果；相同 key 搭配不同请求体应视为冲突。

## 请求签名与防重放

默认关闭：

```yaml
app:
  security:
    replay:
      enabled: false
```

启用后请求需要提供：

```http
X-App-Id: partner-a
X-Timestamp: 1710000000000
X-Nonce: random-string
X-Sign: signature
```

签名算法由服务端配置决定，不信任调用方传入算法：

```yaml
app:
  security:
    replay:
      enabled: true
      signature-algorithm: HMAC_SHA256
      clients:
        - app-id: partner-a
          signature-key: secret-a
          enabled: true
```

新增签名算法时实现 `RequestSignatureVerifier` 即可。

## 请求解密与响应加密

默认关闭：

```yaml
app:
  crypto:
    enabled: false
```

协议元数据统一放 Header：

```http
X-App-Id: partner-a
X-Crypto-Algorithm: NOOP
X-Crypto-Encrypted-Key: optional
X-Crypto-IV: optional
```

请求密文载荷：

```json
{
  "data": "encrypted request payload"
}
```

响应加密只替换 `ApiResponse.data`，`code/message/traceId` 保持明文：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "data": "encrypted response payload"
  },
  "traceId": "abc"
}
```

真实算法通过 `PayloadCryptoProvider` 扩展。当前内置 `NOOP` 仅用于验证框架流程。

开启加解密后，`CryptoFilter` 会在请求解密成功后保存明文请求体副本，并在响应加密前保存明文响应体副本。是否在访问日志中使用这些明文副本由 `app.log.plain-crypto-body-enabled` 控制，默认关闭。对外协议不变：Controller 面对明文 DTO，客户端收到的响应仍按配置加密 `ApiResponse.data`。

## 日志

默认日志目录：

```text
logs/external-api-foundation
```

日志文件：

```text
app.log      应用日志
error.log    ERROR 日志
access.log   入站访问日志和出站 HTTP 日志
```

覆盖日志根目录：

```bash
java -DLOG_PATH=/data/logs -jar target/external-api-foundation-0.0.1-SNAPSHOT.jar
```

日志支持 `X-Trace-Id` 透传、MDC、请求体/响应体脱敏和最大长度截断。

加密接口明文 body 日志默认关闭：

```yaml
app:
  log:
    plain-crypto-body-enabled: false
```

入站访问日志分为元信息和 body 两类：

```text
HTTP BEGIN POST /api/example/orders traceId=... ip=... userId=-
HTTP END   POST /api/example/orders -> 200 35ms traceId=... ip=... userId=- code=0
REQ  {"userId":10001,"amount":99.99}
RESP {"code":0,"message":"success","data":{"orderId":1},"traceId":"..."}
```

`HTTP BEGIN` 和 `HTTP END` 只打印方法、路径、TraceId、IP、状态码、耗时和业务 `code` 等元信息，不打印请求体或响应体。`REQ` 和 `RESP` 打印 body；如果启用了请求解密/响应加密且 `plain-crypto-body-enabled=true`，日志优先打印 `CryptoFilter` 保存的明文副本，再执行脱敏和截断。开关关闭或解密失败时，请求日志回退到原始请求体，响应日志回退到最终响应体。

## 测试

编译测试代码：

```bash
mvn test-compile
```

运行核心轻量单元测试：

```bash
mvn "-Dtest=ApiResponseTest,PageSupportTest,MaskUtilsTest,TraceIdUtilsTest,PathMatchUtilsTest,CurrentUserContextTest,IdempotencyCheckResultTest,SignatureSupportTest,CryptoSupportTest" test
```

运行 HTTP 客户端测试：

```bash
mvn -Dtest=DefaultHttpClientServiceTest,HttpClientUriBuilderTest test
```

运行 JWT 测试：

```bash
mvn -Dtest=JwtTokenProviderTest test
```

## 设计文档

更完整的模块设计、扩展点和约束见：

```text
DESIGN.md
```

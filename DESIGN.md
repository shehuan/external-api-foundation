# external-api-foundation 设计文档

## 1. 定位

`external-api-foundation` 是一个外部 API 接入服务基础工程。它的目标不是替代业务开发，而是把外部接口项目里反复出现的基础能力统一下来，让业务代码只关注业务流程。

核心目标：

- 对外协议稳定：统一响应、错误码、分页、异常语义。
- 调用链可观测：所有请求携带 TraceId，入站和出站 HTTP 都有结构化日志。
- 安全能力内聚：JWT、签名防重放、加解密协议框架集中在 infrastructure。
- 写操作可靠：防重复提交、幂等、分布式锁配合数据库唯一约束兜底。
- 扩展点明确：签名算法、加解密算法、HTTP 出站拦截器都通过接口扩展。
- 示例适度：保留订单示例和任务示例，用于说明框架能力，不把业务模板做厚。

## 2. 技术栈与版本

| 模块 | 当前选择 |
| --- | --- |
| JDK | Java 21 |
| Spring Boot | 3.3.5 |
| Web | Spring MVC |
| 参数校验 | Jakarta Validation |
| JSON | Jackson |
| JWT | JJWT 0.13.0 |
| 接口文档 | Knife4j 4.5.0 |
| 数据库 | MySQL |
| ORM | MyBatis-Plus 3.5.16 |
| 连接池 | HikariCP |
| Redis | Spring Data Redis |
| 分布式锁 | Redisson 3.52.0 |
| 健康检查 | Actuator |
| 日志 | SLF4J + Logback + MDC |

Spring Boot 版本固定在 3.3.5，主要是为了兼容 Knife4j 4.5.0 当前依赖的 springdoc 版本，降低 Spring Framework 6.2.x 兼容风险。

## 3. 工程结构

```text
src/main/java/com/example/externalapi
├─ common
│  ├─ error
│  ├─ exception
│  ├─ page
│  └─ response
├─ config
├─ controller
├─ dto
├─ entity
├─ mapper
├─ service
├─ task
└─ infrastructure
   ├─ crypto
   │  ├─ config
   │  ├─ context
   │  ├─ key
   │  ├─ model
   │  ├─ provider
   │  └─ web
   ├─ http
   │  ├─ api
   │  ├─ config
   │  ├─ core
   │  ├─ exception
   │  ├─ interceptor
   │  └─ support
   ├─ idempotency
   ├─ lock
   ├─ logging
   ├─ repeat
   ├─ scheduler
   ├─ security
   │  ├─ jwt
   │  ├─ replay
   │  └─ user
   └─ web
```

分层约定：

- `controller`：HTTP 入站适配，只处理协议、校验和响应。
- `dto`：接口契约对象，按业务域继续分包。
- `service`：业务流程编排。
- `entity`：数据库实体，不直接作为对外响应。
- `mapper`：MyBatis-Plus Mapper，Java 和 XML 当前都放在 `src/main/java`。
- `common`：跨模块通用模型与异常。
- `infrastructure`：技术基础设施，避免散落到业务包。

## 4. 配置分层

当前只保留基础配置和本地开发配置：

```text
application.yml      通用框架配置、开关、路径规则、日志、MyBatis-Plus、Actuator
application-dev.yml  本地 MySQL、Redis、JWT secret、示例 appId/key
```

默认激活：

```yaml
spring:
  profiles:
    active: dev
```

后续增加 `test/prod` 时按同样原则拆分：稳定框架配置放 `application.yml`，环境相关连接信息和密钥放对应 profile。

## 5. 统一响应与错误码

统一响应模型：

```java
public record ApiResponse<T>(
        int code,
        String message,
        T data,
        String traceId
) {
}
```

响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "traceId": "abc123"
}
```

错误码分段：

```text
0       成功
400xxx  请求参数错误
401xxx  认证、签名、Token 错误
403xxx  权限错误
404xxx  资源不存在
409xxx  冲突、重复、幂等冲突
429xxx  频率限制、重复提交
500xxx  系统或基础设施错误
100xxx+ 示例业务错误
```

HTTP Status 和 `body.code` 同时保留：

- HTTP Status 给网关、浏览器、监控和通用 HTTP 客户端使用。
- `body.code` 给调用方识别具体业务或基础设施错误。

## 6. 全局异常处理

入口类：

```text
com.example.externalapi.common.exception.GlobalExceptionHandler
```

覆盖范围：

- `BizException`
- 参数绑定与校验异常
- 请求体缺失或 JSON 解析异常
- 请求方法不支持
- 资源不存在
- 数据库唯一约束冲突
- 出站 HTTP 客户端异常
- 未知异常兜底

设计原则：

- 对外返回稳定错误码和消息。
- 日志保留排查细节。
- 未知异常不暴露堆栈、SQL、内部类名。

## 7. 分页模型

对外协议不暴露 MyBatis-Plus 的 `Page/IPage`。

请求基类：

```text
PageQuery
pageNo    默认 1，最小 1
pageSize  默认 10，最小 1，最大 500
```

响应模型：

```text
PageResult<T>
records
pageNo
pageSize
total
pages
```

业务分页 DTO 继承 `PageQuery`：

```java
public class OrderPageQuery extends PageQuery {
    private Long userId;
    private String requestNo;
}
```

Service/Mapper 内部可使用 MyBatis-Plus `Page`，Controller 对外只返回 `PageResult<T>`。

## 8. TraceId 与日志

TraceId 规则：

- 请求头 `X-Trace-Id` 合法时沿用。
- 未传或非法时服务端生成 32 位随机 TraceId。
- TraceId 写入 MDC。
- 响应 Header 和响应体都返回 TraceId。
- 出站 HTTP 调用自动透传 TraceId。

入站日志：

```text
RequestResponseLogFilter
HTTP_ACCESS logger
```

出站日志：

```text
DefaultHttpClientService
HTTP_OUT logger
```

日志能力：

- 打印请求开始和结束。
- 打印请求体/响应体，支持开关。
- `HTTP BEGIN` 和 `HTTP END` 只打印元信息，不打印 body。
- 请求体/响应体日志使用 `REQ` 和 `RESP` 单独打印。
- 加解密启用且 `app.log.plain-crypto-body-enabled=true` 时，`REQ` 和 `RESP` 优先打印 `CryptoFilter` 写入的明文副本。
- 敏感字段脱敏：password、token、authorization、phone、email、bankcard 等。
- Body 超长截断。
- 可配置排除路径。

入站日志示例：

```text
HTTP BEGIN POST /api/example/orders traceId=abc ip=127.0.0.1 userId=-
HTTP END   POST /api/example/orders -> 200 35ms traceId=abc ip=127.0.0.1 userId=- code=0
REQ  {"userId":10001,"amount":99.99}
RESP {"code":0,"message":"success","data":{"orderId":1},"traceId":"abc"}
```

明文日志实现约定：

- `RequestResponseLogFilter` 是最外层 Filter，负责缓存原始请求体、包装响应体，并在 `finally` 中统一打印访问日志。
- `CryptoFilter` 负责加解密，也负责在解密成功后写入明文请求体 attribute，在响应加密前写入明文响应体 attribute。
- `RequestResponseLogFilter` 只有在 `app.log.plain-crypto-body-enabled=true` 时才会读取这些明文 attribute；默认回退到原始请求体或最终响应体。
- 解密失败时无法得到明文，请求日志只能保留原始请求体。

默认文件：

```text
logs/external-api-foundation/app.log
logs/external-api-foundation/error.log
logs/external-api-foundation/access.log
```

滚动策略：

- 单文件最大 100MB。
- 保留 30 天。
- 总大小上限 10GB。

## 9. JWT 鉴权

核心类：

```text
JwtTokenProvider
JwtAuthFilter
JwtProperties
CurrentUserContext
LoginUser
```

请求头：

```http
Authorization: Bearer <token>
```

鉴权通过后写入：

```java
public record LoginUser(
        Long userId,
        String username,
        List<String> roles
) {
}
```

`CurrentUserContext` 使用 `ThreadLocal` 保存当前用户。Filter 必须在 `finally` 中调用 `clear()`，避免 Tomcat 线程复用造成用户串线。

## 10. Knife4j 与 OpenAPI

访问入口：

```text
/doc.html
/v3/api-docs
/swagger-ui.html
```

JWT 使用 OpenAPI HTTP Bearer 方案。Knife4j 授权窗口中按页面提示填写；大多数情况下填写 JWT 本体即可，页面会自动拼接 `Authorization: Bearer <token>`。

## 11. MyBatis-Plus 与数据库

Mapper 约定：

```text
src/main/java/com/example/externalapi/mapper/*.java
src/main/java/com/example/externalapi/mapper/*.xml
```

由于 XML 放在 `src/main/java`，Maven 需要显式打包 XML 资源：

```xml
<resource>
    <directory>src/main/java</directory>
    <includes>
        <include>**/*.xml</include>
    </includes>
</resource>
```

MyBatis-Plus 插件：

- `PaginationInnerInterceptor(DbType.MYSQL)`
- `OptimisticLockerInnerInterceptor`

`BaseEntity` 统一字段：

```text
createTime
updateTime
createBy
updateBy
deleted
```

自动填充：

- insert：创建时间、更新时间、创建人、更新人、deleted=0
- update：更新时间、更新人

创建人和更新人来自 `CurrentUserContext.getUserId()`。

## 12. Redis 与 Redisson

Redis 用途：

- 防重复提交短期占位。
- 请求防重放 nonce 存储。
- 幂等记录存储。

Redisson 用途：

- 通用分布式锁。
- 分布式定时任务互斥。
- 关键业务串行化辅助。

注意：Redis/Redisson 是并发控制辅助，不应替代数据库唯一约束。关键写操作最终仍应由数据库约束兜底。

## 13. 防重复提交

注解：

```java
@NoRepeatSubmit(seconds = 5)
```

Key 组成：

```text
userId 或 IP + HTTP method + URI + requestBodyHash
```

实现方式：

```text
Redis SET NX EX seconds
```

适用场景：

- 前端按钮重复点击。
- 调用方短时间重复提交同一请求。

不适用场景：

- 需要长期语义保证的业务幂等。
- 支付、下单、发券等关键副作用接口。此类场景应使用幂等机制和数据库唯一约束。

## 14. 幂等设计

核心类：

```text
IdempotencyService
RedisIdempotencyService
IdempotencyRecord
IdempotencyCheckResult
IdempotencyStatus
```

状态：

```text
PROCESSING 处理中
SUCCESS    已成功，可复用响应
FAILED     处理失败
```

推荐组合：

```text
Idempotency-Key + requestHash + Redis 记录 + 分布式锁 + 数据库唯一约束
```

请求示例：

```http
POST /api/example/orders
Idempotency-Key: REQ202605070001
Content-Type: application/json

{
  "userId": 10001,
  "amount": 99.99
}
```

规则：

- 相同 key + 相同请求体：重复请求返回首次成功响应。
- 相同 key + 不同请求体：返回幂等冲突。
- 第一次请求仍在处理：根据业务策略返回处理中或冲突。
- GET 不应依赖幂等机制修补副作用设计。

## 15. 请求签名与防重放

核心类：

```text
ReplayProtectionFilter
ReplayProtectionProperties
RequestSignatureVerifier
RequestSignatureVerifierRegistry
HmacSha256RequestSignatureVerifier
SignatureUtils
RedisNonceStore
```

默认关闭：

```yaml
app:
  security:
    replay:
      enabled: false
```

请求头：

```http
X-App-Id: partner-a
X-Timestamp: 1710000000000
X-Nonce: random-string
X-Sign: signature
```

签名原文由服务端构造，签名算法由服务端配置：

```yaml
app:
  security:
    replay:
      signature-algorithm: HMAC_SHA256
```

校验顺序：

1. 路径命中检查。
2. 校验 appId、timestamp、nonce、sign。
3. 校验 timestamp 是否在允许窗口内。
4. 使用 Redis nonce 防止重复请求。
5. 按服务端配置选择 `RequestSignatureVerifier`。
6. 验签失败返回签名错误。

扩展 RSA/SM2 等算法时，新增 `RequestSignatureVerifier` Bean 即可。

## 16. 请求解密与响应加密

核心类：

```text
config/CryptoProperties
context/CryptoContext
key/CryptoKeyResolver
key/DefaultCryptoKeyResolver
model/CryptoEnvelope
model/CryptoMetadata
model/CryptoKey
provider/PayloadCryptoProvider
provider/PayloadCryptoProviderRegistry
provider/NoopPayloadCryptoProvider
web/CryptoFilter
web/DecryptedBodyHttpServletRequest
logging/RequestLogAttributes
```

默认关闭：

```yaml
app:
  crypto:
    enabled: false
```

协议 Header：

```http
X-App-Id: partner-a
X-Crypto-Algorithm: RSA_AES
X-Crypto-Encrypted-Key: optional
X-Crypto-IV: optional
```

Body 只承载业务数据：

- 明文请求：普通业务 JSON。
- 密文请求：`CryptoEnvelope`。

密文请求示例：

```json
{
  "data": "encrypted request payload"
}
```

响应加密只替换 `ApiResponse.data`：

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

支持组合：

- 请求明文 + 响应明文
- 请求密文 + 响应密文
- 请求明文 + 响应密文
- 请求密文 + 响应明文

处理流程：

1. 判断请求路径是否需要解密。
2. 判断响应路径是否需要加密。
3. 任一需要时解析 Header，构建 `CryptoMetadata`。
4. 根据 `X-App-Id` 构建 `CryptoContext` 并解析 `CryptoKey`。
5. 根据算法选择 `PayloadCryptoProvider`。
6. 调用 `prepare` 做密钥协商或上下文准备。
7. 如果请求需要解密，读取 `CryptoEnvelope`，解密成功后将明文请求体写入日志 attribute，并替换请求体。
8. Controller 按普通 DTO 处理。
9. 如果响应需要加密，先将加密前的明文 `ApiResponse` 写入日志 attribute，再只加密 `ApiResponse.data`。

内置 `NOOP` Provider 只用于验证框架流程，不应作为生产加密算法。

## 17. 出站 HTTP 客户端

模块结构：

```text
com.example.externalapi.infrastructure.http.api
com.example.externalapi.infrastructure.http.config
com.example.externalapi.infrastructure.http.core
com.example.externalapi.infrastructure.http.exception
com.example.externalapi.infrastructure.http.interceptor
com.example.externalapi.infrastructure.http.support
```

统一入口：

```text
HttpClientService
```

支持能力：

- GET 路径变量。
- GET query 参数。
- POST JSON。
- POST FORM。
- 自定义请求头。
- 同步返回。
- 回调返回。
- beforeRequest/afterResponse/onError 拦截器。

请求头合并规则：

- 自动设置 `X-Trace-Id`。
- 自动设置 `User-Agent`。
- 自动设置 `Accept: application/json`。
- POST JSON/FORM 自动设置 `Content-Type`。
- 调用方传入 header 可覆盖同名默认 header。

异常分类：

```text
NETWORK_ERROR
TIMEOUT
HTTP_STATUS_ERROR
RESPONSE_PARSE_ERROR
```

回调约定：

- 普通方法失败：抛出 `HttpClientException`。
- 回调方法失败：进入 `callback.onFailure`。
- 回调内部业务异常：继续向外抛出。

## 18. 定时任务

基础配置：

```text
SchedulerConfig
SchedulerProperties
```

配置项：

```yaml
app:
  scheduler:
    enabled: true
    pool-size: 4
    thread-name-prefix: demo-scheduler-
    await-termination-seconds: 30
```

示例任务：

```text
LocalSerialDemoTask
DistributedSerialDemoTask
```

`LocalSerialDemoTask` 使用 `fixedDelay`，保证同一 JVM 内上一次执行完成后再等待下一轮。

`DistributedSerialDemoTask` 使用 Redisson 分布式锁，保证多实例部署时同一时间只有一个实例执行。拿不到锁时跳过本轮，不排队等待。

## 19. HTTP Filter 顺序

当前设计顺序：

```text
RequestResponseLogFilter
-> ReplayProtectionFilter
-> CryptoFilter
-> JwtAuthFilter
-> Controller
```

含义：

- 日志最外层负责 TraceId、访问日志和最终响应记录。
- 防重放在解密前执行，因为签名需要覆盖原始请求体和关键安全 Header。
- 加解密在鉴权前执行，使 Controller 始终面对普通 DTO。
- 请求返回阶段先回到 `CryptoFilter`，它在响应加密前保存明文响应副本；最后回到 `RequestResponseLogFilter`，由日志 Filter 打印 `HTTP END`、`REQ` 和 `RESP`。
- JWT 靠近 Controller，负责写入当前用户上下文。

## 20. 代码生成器

位置：

```text
src/test/java/com/example/externalapi/generator/MybatisPlusCodeGenerator.java
```

生成范围：

```text
Entity
Mapper Java
Mapper XML
```

不生成：

```text
Service
ServiceImpl
Controller
```

原因：Service 和 Controller 需要按业务语义设计，不适合由表结构直接生成。

生成后必须检查：

- Entity 是否重复声明 `BaseEntity` 中已有字段。
- 主键字段是否符合实际表结构。
- XML 是否被 Maven resources 配置打包。

## 21. 单元测试策略

当前测试覆盖：

- `ApiResponse`
- `PageQuery/PageResult/PageUtils`
- `MaskUtils`
- `TraceIdUtils`
- `PathMatchUtils`
- `CurrentUserContext`
- `IdempotencyCheckResult`
- `SignatureUtils`、HMAC 验签、签名验证器注册表
- 加解密 Provider、Provider 注册表、默认 Key 解析器
- HTTP 客户端默认实现和 URI 构造器
- JWT Token Provider

测试原则：

- 纯逻辑优先单元测试。
- Redis、数据库、Filter 链路适合后续补集成测试。
- HTTP 出站调用使用 `MockRestServiceServer`，不依赖真实网络。
- 安全相关工具至少覆盖成功路径、失败路径和异常路径。

常用命令：

```bash
mvn test-compile
mvn "-Dtest=ApiResponseTest,PageSupportTest,MaskUtilsTest,TraceIdUtilsTest,PathMatchUtilsTest,CurrentUserContextTest,IdempotencyCheckResultTest,SignatureSupportTest,CryptoSupportTest" test
mvn -Dtest=DefaultHttpClientServiceTest,HttpClientUriBuilderTest test
mvn -Dtest=JwtTokenProviderTest test
```

## 22. 运维注意事项

- 本地启动前需要 MySQL 和 Redis 可用。
- Redis 没有密码时不要配置空 password，避免 Redisson 发送 AUTH。
- 生产环境不要使用示例 JWT secret、示例 appId key、示例 crypto secret。
- 加解密模块默认只有 `NOOP` Provider，生产必须实现真实算法。
- 幂等和分布式锁不能替代数据库唯一约束。
- Mapper XML 放在 `src/main/java`，必须保留 `pom.xml` 中的 resources 配置。
- Knife4j 静态资源路径需要在 JWT 排除列表中保留。
- `LOG_PATH` 可通过 JVM 参数覆盖。

## 23. 后续演进建议

- 增加 Testcontainers 或本地 profile 的 Redis/MySQL 集成测试。
- 为 `ReplayProtectionFilter`、`CryptoFilter`、`NoRepeatSubmitAspect` 补 Web 层集成测试。
- 引入真实加密 Provider，例如 AES-GCM、RSA-AES、SM4/SM2。
- 为业务幂等增加注解或模板方法，减少业务层重复样板代码。
- 将示例业务包和基础设施包进一步解耦，方便作为 starter 或 archetype 复用。

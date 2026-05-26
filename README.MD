# maarlakes-common

Java 8 工具库，提供 Token 管理、异步 HTTP 客户端框架、事件总线、责任链、分布式锁、定时任务、消息队列、工厂、SPI 扩展系统等通用能力。

## 依赖

```xml
<dependency>
    <groupId>cn.maarlakes</groupId>
    <artifactId>common</artifactId>
    <version>2.0.0</version>
</dependency>
```

**Java 版本要求**：Java 8+

---

## 功能模块

### 1. Token 管理

通用的 Token 管理框架，支持内存和 Redis 两种存储后端，内置微信 Access Token 获取能力。提供完整的缓存、过期检查、自动刷新和分布式锁保护机制。

#### 架构概览

```
                              ┌──────────────────────────────────┐
                              │         Token 模型层              │
                              │  AppToken<A, T>                  │
                              │      └── ExpirationAppToken<A,T> │
                              │              └── AccessToken     │
                              └──────────────────────────────────┘
                                            ▲
                                            │ 获取/缓存
                              ┌─────────────┴──────────────────────────┐
                              │           Repository 层                 │
                              │  TokenRepository                        │
                              │      └── CacheableTokenRepository      │
                              │              └── RefreshableToken-     │
                              │                  Repository            │
                              │                  └── ExpirationToken-  │
                              │                      Repository        │
                              │              ┌─────────┴──────────┐    │
                              │     MemoryAccessToken-   Redisson-   │    │
                              │     Repository           AccessToken-│    │
                              │                           Repository │    │
                              └─────────────────────────────────────┘
                                            ▲
                                            │ 创建 Token
                              ┌─────────────┴──────────────────────────┐
                              │            Factory / Provider 层        │
                              │  TokenFactory                            │
                              │      └── AccessTokenFactory             │
                              │          └── AccessTokenProvider        │
                              │              └── AbstractAccess-        │
                              │                  TokenProvider          │
                              │              ┌──────────┴───────────┐  │
                              │     DefaultWeixin-    StableWeixin-  │  │
                              │     TokenProvider     TokenProvider  │  │
                              └────────────────────────────────────────┘
                                            ▲
                              ┌─────────────┴──────────────────────────┐
                              │         DefaultAccessTokenFactory       │
                              │  遍历 Provider 列表 → supported(appId)  │
                              │  → 第一个支持的 Provider 创建 Token      │
                              └────────────────────────────────────────┘
```

#### 核心接口

**Token 模型**

| 接口 | 职责 |
|------|------|
| `AppToken<A, T>` | Token 顶层接口，关联 appId 与 token 值 |
| `ExpirationAppToken<A, T>` | 扩展 `AppToken`，增加过期时间（`expiresAt`）和过期判断（`isExpired`） |
| `AccessToken` | 泛型特化的访问令牌（`AppId` + `String`），通过 `AccessToken.of()` 创建 |

**Repository 层**

| 接口 | 职责 |
|------|------|
| `TokenRepository` | 基础仓库契约：按 appId 获取 Token |
| `CacheableTokenRepository` | 增加批量查询（`getTokens`）、清空（`clear`）、移除（`remove`） |
| `RefreshableTokenRepository` | 增加主动刷新（`refresh`），强制重新创建 Token |
| `ExpirationTokenRepository` | 增加过期清理（`removeExpiredToken`）和过期查询（`getExpiredTokens`） |
| `AccessTokenRepository` | 泛型特化为 `AccessToken` 的完整仓库接口 |

**Factory / Provider 层**

| 接口/类 | 职责 |
|---------|------|
| `TokenFactory` | 异步创建 Token 的工厂接口 |
| `AccessTokenFactory` | 泛型特化的 AccessToken 工厂 |
| `AccessTokenProvider` | 扩展 `TokenFactory`，增加 `supported(AppId)` 能力声明 |
| `DefaultAccessTokenFactory` | 多 Provider 路由实现，遍历列表匹配第一个支持的 Provider |
| `AbstractAccessTokenProvider` | 基于 HTTP 的模板方法基类（构建请求 + 解析响应 + 重试） |

#### 接口继承关系

**Token 模型**

```
AppToken<A, T>
└── ExpirationAppToken<A, T>
    └── AccessToken（特化为 AppId + String）
```

**Repository**

```
TokenRepository<T, A, V>
└── CacheableTokenRepository<T, A, V>
    └── RefreshableTokenRepository<T, A, V>
        └── ExpirationTokenRepository<T, A, V>
            └── AccessTokenRepository（特化）
                ├── MemoryAccessTokenRepository
                └── RedissonAccessTokenRepository
```

**Provider**

```
TokenFactory<T, A, V>
└── AccessTokenFactory（特化）
    └── AccessTokenProvider
        └── AbstractAccessTokenProvider（模板方法：buildRequest + parseToken + 重试）
            └── AbstractWeixinTokenProvider（微信通用响应解析 + supported 匹配）
                ├── DefaultWeixinTokenProvider（GET /cgi-bin/token）
                └── StableWeixinTokenProvider（POST /cgi-bin/stable_token）
```

#### 快速开始

**1. 使用内存仓库管理 Token**

```java
// 创建 Token Provider（以微信为例）
WeixinSecretMapper secretMapper = new MemoryWeixinSecretMapper(
    ImmutableMap.of("wx1234", "secret1")
);
AccessTokenProvider provider = new DefaultWeixinTokenProvider(httpClient, secretMapper);

// 创建 Factory（支持多个 Provider）
AccessTokenFactory factory = new DefaultAccessTokenFactory(
    Collections.singletonList(provider)
);

// 创建内存仓库
AccessTokenRepository repository = new MemoryAccessTokenRepository(factory);

// 获取 Token（自动缓存，过期自动刷新）
AppId appId = WeixinAppId.of("wx1234");
AccessToken token = repository.getToken(appId);
System.out.println(token.getToken());
System.out.println(token.getExpiresAt());

// 同步阻塞获取
AccessToken sameToken = repository.getToken(appId);  // 缓存命中
```

**2. 使用 Redis 仓库管理 Token**

```java
// 创建 Redis 仓库（分布式锁保护，跨实例共享）
AccessTokenRepository repository = new RedissonAccessTokenRepository(
    redissonClient,
    "myapp:access-token",    // Redis Map 命名空间
    factory,
    Duration.ofMinutes(3)     // 分布式锁获取超时
);

// 多实例部署时，同一 appId 只会创建一次 Token
AccessToken token = repository.getToken(WeixinAppId.of("wx1234"));
```

**3. 微信标准 Token 获取**

```java
// GET 请求方式（默认 API 地址）
AccessTokenProvider provider = new DefaultWeixinTokenProvider(
    httpClient,
    secretMapper
);

// 自定义 API 地址（代理或私有部署）
AccessTokenProvider provider = new DefaultWeixinTokenProvider(
    httpClient,
    secretMapper,
    "https://my-proxy.example.com/cgi-bin/token"
);

// 带重试
AccessTokenProvider provider = new DefaultWeixinTokenProvider(
    httpClient,
    3,               // 最大重试次数
    secretMapper
);
```

**4. 微信 Stable Token 获取**

```java
// POST 请求方式，支持强制刷新
AccessTokenProvider provider = new StableWeixinTokenProvider(
    true,            // forceRefresh：强制刷新微信服务端缓存
    httpClient,
    secretMapper
);

// 不强制刷新（使用微信服务端缓存的 Token）
AccessTokenProvider provider = new StableWeixinTokenProvider(
    false,
    httpClient,
    secretMapper
);
```

#### 仓库实现对比

| 特性 | MemoryAccessTokenRepository | RedissonAccessTokenRepository |
|------|----------------------------|-------------------------------|
| 存储 | JVM 内存（`ConcurrentHashMap`） | Redis（`RMap`） |
| 跨实例共享 | 不支持 | 支持 |
| 并发保护 | `computeIfAbsent` 原子操作 | Redisson 信号量分布式锁 |
| 序列化 | 不需要 | Kryo5Codec |
| 过期清理 | 扫描缓存 CAS 移除 | `fastRemoveAsync` 批量删除 |
| 适用场景 | 单实例部署 | 多实例部署 |
| 进程重启 | Token 丢失 | Token 持久化 |
| 额外依赖 | 无 | `org.redisson:redisson`（可选） |

#### Token Provider 路由

`DefaultAccessTokenFactory` 按顺序遍历注册的 Provider 列表，选择第一个支持当前 AppId 的 Provider：

```java
// 注册多个 Provider（顺序决定优先级）
List<AccessTokenProvider> providers = Arrays.asList(
    weixinProvider,      // 支持 WeixinAppId
    dingtalkProvider,    // 支持 DingtalkAppId
    feishuProvider       // 支持 FeishuAppId
);

AccessTokenFactory factory = new DefaultAccessTokenFactory(providers);

// 路由到对应的 Provider
AccessToken wxToken = factory.createToken(WeixinAppId.of("wx1234")).toCompletableFuture().get();
AccessToken dtToken = factory.createToken(DingtalkAppId.of("dt5678")).toCompletableFuture().get();

// 没有匹配的 Provider → 抛出 UnsupportedAppException
AccessToken unknown = factory.createToken(unknownAppId).toCompletableFuture().get();
```

#### 微信 Access Token

**两种 Provider 的区别**

| 特性 | DefaultWeixinTokenProvider | StableWeixinTokenProvider |
|------|---------------------------|--------------------------|
| HTTP 方法 | GET | POST |
| API 路径 | `/cgi-bin/token` | `/cgi-bin/stable_token` |
| 参数传递 | Query String | JSON Body |
| 强制刷新 | 不支持 | 支持（`forceRefresh` 参数） |
| 适用场景 | 常规获取 | 高稳定性要求、Token 被意外刷新后需要立即获取 |

**WeixinSecretMapper**

通过 `WeixinSecretMapper` 接口将 AppID 映射到 AppSecret，解耦密钥管理与 Token 获取：

```java
// 基于内存映射
WeixinSecretMapper mapper = new MemoryWeixinSecretMapper(
    ImmutableMap.of("wx1234", "abc123", "wx5678", "def456")
);

// 自定义实现（如从数据库、配置中心获取）
WeixinSecretMapper dbMapper = appId -> {
    return secretRepository.findSecretByAppId(appId);
};
```

#### 自定义扩展

**扩展新的 Token 类型**

```java
// 1. 定义新的 AppId 类型
public interface DingtalkAppId extends AppId {
    static DingtalkAppId of(String appId) {
        return new DefaultDingtalkAppId(appId);
    }
}

// 2. 实现 AccessTokenProvider
public class DingtalkTokenProvider extends AbstractAccessTokenProvider {

    @Override
    protected Request buildRequest(AppId appId) {
        return Request.builder()
                .get("https://oapi.dingtalk.com/gettoken")
                .addQueryParam("appkey", appId.getAppId())
                .addQueryParam("appsecret", getSecret(appId))
                .build();
    }

    @Override
    protected AccessToken parseToken(AppId appId, Response response, Instant now) {
        // 解析钉钉 API 响应为 AccessToken
        JSONObject json = JSON.parseObject(response.getBody().asText(StandardCharsets.UTF_8));
        return AccessToken.of(appId, json.getString("access_token"),
                now.plusSeconds(json.getLongValue("expires_in")));
    }

    @Override
    public boolean supported(AppId appId) {
        return appId instanceof DingtalkAppId;
    }
}

// 3. 注册到 DefaultAccessTokenFactory
AccessTokenFactory factory = new DefaultAccessTokenFactory(Arrays.asList(
    new DefaultWeixinTokenProvider(httpClient, weixinMapper),
    new DingtalkTokenProvider(httpClient, dingtalkMapper)
));
```

#### 异常体系

```
TokenException (RuntimeException)
├── UnsupportedAppException      — 没有可用的 Provider 能处理给定的 AppId
└── WeixinTokenException         — 微信 API 返回错误（包含 errcode + errmsg）
```

#### 日志

框架内置 SLF4J 日志：

| 组件 | 级别 | 内容 |
|------|------|------|
| `MemoryCacheableAppTokenRepository` | INFO | Token 创建成功（含过期时间） |
| `MemoryCacheableAppTokenRepository` | DEBUG | 缓存命中/未命中、移除 Token |
| `MemoryCacheableAppTokenRepository` | WARN | Token 创建失败 |
| `MemoryExpirationTokenRepository` | INFO | 过期 Token 清理数量 |
| `MemoryExpirationTokenRepository` | DEBUG | Token 过期触发刷新 |
| `RedissonCacheableAppTokenRepository` | INFO | Token 创建并写入 Redis、缓存清空 |
| `RedissonCacheableAppTokenRepository` | DEBUG | 缓存命中/未命中、分布式锁获取、双重检查 |
| `RedissonCacheableAppTokenRepository` | WARN | 获取分布式锁超时 |
| `RedissonCacheableAppTokenRepository` | ERROR | 释放锁失败、删除信号量失败 |
| `RedissonExpirationTokenRepository` | INFO | 过期 Token 清理数量 |
| `RedissonExpirationTokenRepository` | DEBUG | Token 过期触发刷新 |
| `AbstractAccessTokenProvider` | INFO | Access Token 获取成功 |
| `AbstractAccessTokenProvider` | DEBUG | 开始获取 Token |
| `AbstractAccessTokenProvider` | WARN | 解析失败、重试中 |
| `AbstractAccessTokenProvider` | ERROR | 重试耗尽最终失败 |
| `DefaultAccessTokenFactory` | DEBUG | 匹配到 Provider |
| `DefaultAccessTokenFactory` | WARN | 无可用 Provider |
| `WeixinTokenUtils` | DEBUG | 微信 Token 解析成功 |
| `WeixinTokenUtils` | WARN | 微信返回错误码 |
| `AbstractWeixinTokenProvider` | WARN | 微信请求失败（非 200 响应） |

---

### 2. HTTP 客户端框架

统一的异步 HTTP 客户端抽象，所有操作返回 `CompletableFuture`，通过 SPI 自动发现可用后端。

#### 支持的后端

| 后端 | 优先级 | 异步模型 | 依赖 |
|------|--------|----------|------|
| OkHttp | 100 | Call.enqueue 原生异步 | `com.squareup.okhttp3`（可选） |
| Apache HC 5 | 200 | NIO 异步 | `org.apache.httpcomponents.client5`（可选） |
| Apache HC 4 | 300 | Executor 调度同步 I/O | `org.apache.httpcomponents`（可选） |
| AsyncHttpClient | 400 | Netty 事件循环回调 | `org.asynchttpclient`（可选） |
| JDK HttpURLConnection | 兜底 | Executor 调度同步 I/O | 无（内置） |

所有可选后端均为 `<optional>true</optional>` 依赖，classpath 上有的自动启用，没有则回退到下一个，最终使用 JDK 内置实现。

#### 快速开始

```java
// GET 请求
Request request = Request.builder()
        .get("https://httpbin.org/get")
        .build();

Response response = HttpClients.execute(request).join();
System.out.println(response.getStatusCode());
System.out.println(response.getBody().getContent()); // InputStream

// POST JSON
Request post = Request.builder()
        .post("https://httpbin.org/post")
        .json("{\"name\":\"test\"}")
        .setHeader("Authorization", "Bearer token")
        .build();

Response resp = HttpClients.execute(post).join();
```

#### 构建请求

```java
// 方法快捷方式
Request.builder().get(url);
Request.builder().post(url);
Request.builder().put(url);
Request.builder().delete(url);
Request.builder().patch(url);
Request.builder().head(url);
Request.builder().options(url);

// Header 操作
Request.builder()
        .setHeader("Content-Type", "application/json")    // 替换同名头
        .appendHeader("Accept", "application/json")       // 追加到同名头
        .appendHeader("Accept", "text/html");

// Query 参数
Request.builder()
        .get("https://httpbin.org/get")
        .addQueryParam("key", "value");

// 表单提交
Request.builder()
        .post("https://httpbin.org/post")
        .addFormParam("username", "admin")
        .addFormParam("password", "123456");

// Cookie
Request.builder()
        .get("https://httpbin.org/cookies")
        .addCookie("session", "abc123")
        .addCookie(Cookie.builder("token")
                .value("xyz")
                .domain("httpbin.org")
                .path("/")
                .maxAge(3600)
                .isSecure(true)
                .isHttpOnly(true)
                .build());
```

#### 从已有请求创建副本

```java
Request original = Request.builder()
        .get("https://example.com/api")
        .setHeader("Authorization", "Bearer token")
        .build();

// 复制并修改部分字段
Request modified = Request.Builder.from(original)
        .uri("https://other.example.com/api")
        .setHeader("Authorization", "Bearer new-token")
        .build();
```

#### 请求体类型

```java
// 纯文本
.text("hello world")
.text("hello", "UTF-8")

// JSON 字符串
.json("{\"key\":\"value\"}")

// JSON 对象（通过 JsonFactory SPI 序列化）
.json(myObject)
.json(myObject, Charset.forName("GBK"))

// Multipart
.multipartBody(MultipartBody.builder()
        .addPart(new ByteArrayPart("file", "data.bin", bytes))
        .addPart(new DefaultTextPart("field", "value"))
        .addPart(new DefaultFilePart("upload", new File("/path/to/file")))
        .addPart(new JsonPart("meta", metadataObject))
        .build())

// 自定义 RequestBody
.body(myCustomRequestBody)
```

#### 响应处理

```java
// 完整响应（响应体缓冲到内存）
Response response = HttpClients.execute(request).join();
int status = response.getStatusCode();
String body = IOUtils.toString(response.getBody().getContent(), StandardCharsets.UTF_8);
List<? extends Cookie> cookies = response.getCookies();
Header contentType = response.getHeaders().getHeader("Content-Type");

// 流式响应处理（不缓冲，适合大文件）
CompletableFuture<byte[]> future = HttpClients.execute(request, (responseInfo, bodySink) -> {
    return bodySink.consume(new BodyConsumer<byte[]>() {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        @Override
        public void onChunk(byte[] data, int offset, int length) {
            buffer.write(data, offset, length);
        }

        @Override
        public byte[] onComplete() {
            return buffer.toByteArray();
        }

        @Override
        public void onError(Throwable error) {
            throw new RuntimeException(error);
        }
    });
});
```

#### 请求配置

```java
RequestConfig config = RequestConfig.builder()
        .connectTimeout(Duration.ofSeconds(5))
        .requestTimeout(Duration.ofSeconds(10))
        .responseTimeout(Duration.ofSeconds(30))
        .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.example.com", 8080)))
        .proxyAuthentication(new BasicAuthentication("user", "pass"))
        // 或 Digest 认证
        // .proxyAuthentication(new DigestAuthentication("user", "pass"))
        .build();

Response response = HttpClients.execute(request, config).join();
```

#### 过滤器

过滤器通过责任链模式拦截请求和响应：

```java
HttpFilter loggingFilter = (request, config, handler, chain) -> {
    System.out.println(">>> " + request.getMethod() + " " + request.getUri());
    return chain.doFilter(request, config, handler).whenComplete((response, error) -> {
        if (error != null) {
            System.out.println("<<< ERROR: " + error.getMessage());
        } else {
            System.out.println("<<< " + response.getStatusCode());
        }
    });
};

HttpFilter authFilter = (request, config, handler, chain) -> {
    // 修改请求
    Request newRequest = Request.Builder.from(request)
            .setHeader("Authorization", "Bearer " + getToken())
            .build();
    return chain.doFilter(newRequest, config, handler);
};

Response response = HttpClients.execute(request, loggingFilter, authFilter).join();
```

#### 构建自定义 HttpClient

```java
HttpClient client = HttpClient.builder()
        .okHttp()                           // 或 .apache4() / .apache5() / .asyncHttp() / .jdk()
        .config(HttpClientConfig.builder()
                .executor(Executors.newFixedThreadPool(4))
                .build())
        .filter(loggingFilter)
        .filter(retryFilter)
        .build();

try {
    Response response = client.execute(request).join();
} finally {
    client.close();
}
```

#### 响应体编码

框架通过 SPI 自动解码 `Content-Encoding`，无需手动处理：

| 编码 | 实现类 | 依赖 |
|------|--------|------|
| gzip | `GzipResponseBodyEncoder` | JDK 内置 |
| deflate | `DeflateResponseBodyEncoder` | JDK 内置 |
| brotli | `BrResponseBodyEncoder` | `org.brotli:dec`（可选） |

---

### 3. 事件总线

注解驱动的发布-订阅事件模型，支持同步/异步分发、多参数依赖注入和 SPI 扩展。

#### 架构概览

```
                        ┌─────────────────────────┐
                        │  EventListenerRegistrar  │  注册中心
                        │  register / unregister   │
                        └─────────┬───────────────┘
                                  │ publisher()
                                  ▼
                        ┌─────────────────────────┐
                        │    EventPublisher        │  事件发布
                        │    publish / publishAsync │
                        └─────────┬───────────────┘
                                  │
                    ┌─────────────┼─────────────┐
                    ▼             ▼             ▼
              ┌──────────┐ ┌──────────┐ ┌──────────┐
              │ Invoker1 │ │ Invoker2 │ │ InvokerN │  按优先级排序
              │ (sync)   │ │ (async)  │ │ (sync)   │
              └─────┬────┘ └─────┬────┘ └─────┬────┘
                    │            │            │
                    ▼            ▼            ▼
              ┌──────────────────────────────────────┐
              │          EventDispatcher              │  分发执行
              │   dispatch (同步) / dispatchAsync     │
              └──────────────────────────────────────┘

监听器发现链路：
  EventListenerHandlerFactory (SPI 或组合)
       └── EventListenerHandler (扫描 @EventListener 方法)
              └── EventInvoker (封装方法 + 目标对象)
```

#### 核心接口

| 接口 | 职责 |
|------|------|
| `EventPublisher` | 事件发布入口，支持同步和异步发布 |
| `EventDispatcher` | 底层分发执行器，管理同步/异步线程池 |
| `EventInvoker` | 封装单个监听器方法，负责类型匹配和反射调用 |
| `EventListenerRegistrar` | 监听器注册中心，管理注册/注销/获取发布器 |
| `EventListenerHandler` | 从监听器对象中提取 `EventInvoker` 列表 |
| `EventListenerHandlerFactory` | 获取所有可用的 `EventListenerHandler` |
| `EventPublisherFactory` | 创建 `EventPublisher` 实例 |
| `EventContext` | 事件上下文混入接口，携带键值对属性 |

#### 注解

**@EventListener** — 标注事件监听方法

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `events` | `Class<?>[]` | `{}` | 指定订阅的事件类型。为空时按方法参数类型自动推断 |

可标注在方法上，也可作为元注解使用。

**@EventDispatch** — 控制监听方法的分发策略

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `async` | `boolean` | `false` | 是否异步执行。`true` 时提交到线程池，不阻塞发布线程 |

可标注在方法、类或作为元注解使用。标注在类上时作为该类所有监听方法的默认策略。

#### 快速开始

```java
// 1. 定义事件（普通 POJO 即可）
public class UserCreatedEvent {
    private final String userId;
    private final String userName;

    public UserCreatedEvent(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
}

// 2. 定义监听器
public class UserEventHandlers {

    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        System.out.println("用户创建: " + event.getUserName());
    }

    @EventListener
    @EventDispatch(async = true)
    public void sendWelcomeEmail(UserCreatedEvent event) {
        // 异步执行，不阻塞发布线程
        emailService.sendWelcome(event.getUserId());
    }

    @EventListener
    @Order(50)
    public void onUserCreatedHighPriority(UserCreatedEvent event) {
        // 优先级高，先于其他监听器执行
    }
}

// 3. 创建注册器并注册
EventDispatcher dispatcher = new DefaultEventDispatcher(4);
EventListenerHandlerFactory handlerFactory = new SpiEventListenerHandlerFactory();
EventPublisherFactory publisherFactory = new DefaultEventPublisherFactory();
EventListenerRegistrar registrar = new DefaultEventListenerRegistrar(dispatcher, handlerFactory, publisherFactory);

registrar.register(new UserEventHandlers());

// 4. 获取发布器并发布事件
EventPublisher publisher = registrar.publisher();
publisher.publish(new UserCreatedEvent("u001", "张三"));
```

#### 事件匹配规则

发布事件时，`EventPublisher` 从所有已注册的 `EventInvoker` 中筛选支持该事件类型的调用器：

- **未指定 `@EventListener.events()`**（默认）：只要方法任一参数类型能接受该事件（`isAssignableFrom`），即视为匹配
- **指定了 `@EventListener.events()`**：事件类型必须出现在 `events` 列表中，且方法参数类型也兼容，才视为匹配

匹配结果按事件类型缓存，避免每次发布都重新筛选和排序。

```java
// 示例：精确订阅特定事件
@EventListener(events = {UserCreatedEvent.class})
public void onlyUserCreated(UserCreatedEvent event) {
    // 仅接收 UserCreatedEvent，不接收其他事件
}

// 示例：自动匹配（方法参数为父类，可接收所有子类事件）
@EventListener
public void onAnyUserEvent(BaseUserEvent event) {
    // 接收 BaseUserEvent 及其所有子类
}
```

#### 同步与异步分发

```java
// 同步监听器：在发布线程中执行
@EventListener
public void syncHandler(OrderCreatedEvent event) {
    // 当前线程执行
}

// 异步监听器：提交到线程池执行
@EventListener
@EventDispatch(async = true)
public void asyncHandler(OrderCreatedEvent event) {
    // 线程池中执行
}
```

**阻塞语义**：`publish()` 不是 fire-and-forget。当存在异步监听器时，`publish()` 会通过 `CompletableFuture.allOf().join()` 等待所有异步监听器执行完毕后才返回。

**异步发布**：`publishAsync()` 将整个发布过程（包含 `publish()` 调用）提交到线程池，调用方通过返回的 `CompletionStage` 感知完成时机：

```java
// 异步发布，使用 ForkJoinPool.commonPool()
CompletionStage<Void> stage = publisher.publishAsync(new OrderEvent("order-001"));

// 指定执行器
CompletionStage<Void> stage2 = publisher.publishAsync(new OrderEvent("order-002"), executor);
```

#### 多参数监听方法

监听方法支持多参数，框架自动定位事件参数，其余参数通过 `BeanFactories` 依赖注入：

```java
// 单参数：直接传入事件
@EventListener
public void onEvent(UserCreatedEvent event) { ... }

// 多参数：第一个与事件类型匹配的参数接收事件，其余通过 BeanFactories.getBeanOrNull() 注入
@EventListener
public void onEvent(UserCreatedEvent event, UserService userService, NotificationService notifyService) {
    userService.doSomething(event);
    notifyService.send(event);
}
```

参数定位策略：先精确匹配（`==`），再兼容匹配（`isAssignableFrom`）。如果无法匹配到任何参数，抛出 `EventException`。

#### 监听器排序

通过 `@Order` 注解或实现 `Ordered` 接口控制监听器执行顺序，数值越小优先级越高：

```java
@EventListener
@Order(100)
public void firstHandler(Event event) { }   // 最先执行

@EventListener
@Order(200)
public void secondHandler(Event event) { }  // 第二执行

@EventListener
public void lastHandler(Event event) { }    // 未标注 @Order，优先级最低
```

#### 事件上下文

`EventContext` 是一个混入接口（mixin），可让事件类携带上下文属性（如追踪 ID、租户信息等）：

```java
// 定义带上下文的事件
public class OrderEvent extends DefaultEventContext {
    private final String orderId;

    public OrderEvent(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderId() { return orderId; }
}

// 使用
OrderEvent event = new OrderEvent("order-001");
event.setAttribute("traceId", "abc-123");
event.setAttribute("tenantId", "tenant-001");

// 监听器中获取
@EventListener
public void onOrder(OrderEvent event) {
    String traceId = event.getAttribute("traceId");
}
```

#### SPI 扩展

`EventListenerHandler` 通过 SPI 注册。默认实现 `BeanFactoriesEventListenerHandler.Handler` 在 `META-INF/services/cn.maarlakes.common.event.EventListenerHandler` 中注册，负责扫描 `@EventListener` 方法。

**SpiEventListenerHandlerFactory** — 通过 SPI 加载处理器

```java
// 共享模式（单例缓存，推荐）
EventListenerHandlerFactory factory = new SpiEventListenerHandlerFactory();

// 非共享模式（每次创建新实例）
EventListenerHandlerFactory factory = new SpiEventListenerHandlerFactory(false);

// 指定 ClassLoader
EventListenerHandlerFactory factory = new SpiEventListenerHandlerFactory(myClassLoader);
```

**CombinedEventListenerHandlerFactory** — 组合多个工厂

```java
EventListenerHandlerFactory combined = new CombinedEventListenerHandlerFactory(
        Arrays.asList(spiFactory, customFactory));
```

**自定义监听器发现机制**：

```java
// 1. 实现 EventListenerHandler
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class CustomEventListenerHandler implements EventListenerHandler {
    @Override
    public <L> List<EventInvoker> getInvokers(L listener) {
        // 自定义扫描逻辑
    }
}

// 2. 在 META-INF/services/cn.maarlakes.common.event.EventListenerHandler 中注册
// com.example.CustomEventListenerHandler
```

#### 异常处理

遵循"发布者只管发布，订阅者自行负责异常"的设计原则：

- **单个监听器异常不阻断后续监听器执行**
- **异步分发**：异常通过 `CompletableFuture.exceptionally` 静默处理，记录 ERROR 日志
- **同步分发**：捕获异常后调用 `handleException`，记录 ERROR 日志，不向上抛出

```java
// 自定义异常处理：继承 DefaultEventPublisher
public class CustomPublisher extends DefaultEventPublisher {
    public CustomPublisher(EventDispatcher dispatcher, List<? extends EventInvoker> invokers) {
        super(dispatcher, invokers);
    }

    @Override
    protected <E> void handleException(E event, EventInvoker invoker, Throwable error, boolean async) {
        // 自定义异常处理逻辑（如发送告警、写入死信队列等）
        alertService.notify(event, error);
    }
}
```

#### 日志

框架内置 SLF4J 日志：

| 组件 | 级别 | 内容 |
|------|------|------|
| `DefaultEventPublisher` | DEBUG | 事件发布开始/完成、监听器匹配数量、分发详情 |
| `DefaultEventPublisher` | TRACE | 事件载荷内容 |
| `DefaultEventPublisher` | ERROR | 分发失败（事件、监听器、异常栈） |
| `DefaultEventDispatcher` | TRACE | 同步/异步分发到具体监听器 |
| `DefaultEventListenerRegistrar` | INFO | 监听器注册、注销、清空 |
| `BeanFactoriesEventInvoker` | DEBUG | 调用监听器方法 |
| `BeanFactoriesEventInvoker` | TRACE | 多参数解析结果、事件类型匹配详情 |
| `BeanFactoriesEventListenerHandler` | DEBUG | 发现监听方法、解析调用器数量 |
| `SpiEventListenerHandlerFactory` | DEBUG | SPI 加载处理器数量 |
| `SpiEventListenerHandlerFactory` | TRACE | 已加载的处理器类名列表 |
| `DefaultEventPublisherFactory` | DEBUG | 创建发布器及监听器数量 |
| `CombinedEventListenerHandlerFactory` | TRACE | 合并后的处理器数量 |

---

### 4. 责任链

基于动态代理的责任链框架，支持多种处理器发现方式（SPI、Bean 容器）和多种结果收集策略。

#### 架构

```
ChainFactory                         处理器发现层
├── SpiChainFactory                  META-INF/services SPI 加载
├── BeanFactoriesChainFactory        BeanFactories 全局容器
└── BeanProviderChainFactory         自定义 BeanProvider
        │
        ▼
AbstractChainFactory.createHandlers(type)
        │
        ▼
InvocationHandlerFactory             结果收集策略
├── FirstResult...Factory            首个非空 → 短路返回
├── LastResult...Factory             逐个调用 → 返回最后一个
└── NoneResult...Factory             逐个调用 → 忽略返回值
        │
ChainInvocationFactory               结果收集（含每个处理器的独立结果）
└── DefaultChainInvocationFactory    逐个调用 → 收集全部 KeyValuePair<H, R>
        │
        ▼
Proxy.newProxyInstance → 动态代理
```

#### 核心接口

| 接口 | 职责 |
|------|------|
| `Chain<H, R>` | 链入口，`create()` 生成独立调用器 |
| `ChainFactory` | 工厂，发现处理器并创建链 |
| `ChainInvoker<H, R>` | 持有代理实例，提供结果访问 |
| `ChainInvocationFactory` | 创建 ChainInvoker（可收集全部结果） |
| `InvocationHandlerFactory` | 创建 InvocationHandler（直接返回结果） |

#### 处理器发现方式

| 工厂 | 来源 | 适用场景 |
|------|------|----------|
| `SpiChainFactory` | `META-INF/services/` SPI 配置 | 模块化扩展、插件体系 |
| `BeanFactoriesChainFactory` | `BeanFactories` 全局容器 | 已有 BeanFactories 基础设施的项目 |
| `BeanProviderChainFactory` | 自定义 `BeanProvider` | 需要 IoC 容器集成的场景 |

#### 结果收集策略

| 策略 | 方法 | 行为 |
|------|------|------|
| 首个非空 | `createFirstResultChain(type)` | 按序调用，遇到非 null 立即返回 |
| 最后一个 | `createLastResultChain(type)` | 按序调用全部，返回最后一个结果 |
| 忽略返回值 | `createNoneResultChain(type)` | 按序调用全部，始终返回 null |
| 收集全部 | `createChain(type, ChainInvocationFactory)` | 调用全部，返回每个处理器的独立结果 |

每种策略均支持 `isReverse` 参数，设为 `true` 时逆序执行处理器。

#### SPI 完整示例

**1. 定义处理器接口**

```java
public interface PriceCalculator {
    BigDecimal calculate(Order order);
}
```

**2. 实现处理器**

```java
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
@Order(100)
public class DiscountCalculator implements PriceCalculator {
    @Override
    public BigDecimal calculate(Order order) {
        return order.getTotal().multiply(new BigDecimal("0.9"));
    }
}

@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
@Order(200)
public class TaxCalculator implements PriceCalculator {
    @Override
    public BigDecimal calculate(Order order) {
        return order.getTotal().multiply(new BigDecimal("1.13"));
    }
}
```

**3. 注册 SPI**

在 `META-INF/services/cn.maarlakes.common.chain.example.PriceCalculator` 中：

```
com.example.DiscountCalculator
com.example.TaxCalculator
```

**4. 创建链并调用**

```java
ChainFactory factory = new SpiChainFactory();

// 策略一：返回第一个非空结果（DiscountCalculator 优先级高，先执行）
PriceCalculator first = factory.createFirstResultChain(PriceCalculator.class);
BigDecimal result = first.calculate(order);

// 策略二：返回最后一个结果
PriceCalculator last = factory.createLastResultChain(PriceCalculator.class);
BigDecimal lastResult = last.calculate(order);

// 策略三：忽略返回值（纯副作用场景）
PriceCalculator none = factory.createNoneResultChain(PriceCalculator.class);
none.calculate(order); // 返回 null

// 策略四：收集全部结果
Chain<PriceCalculator, BigDecimal> chain = factory.createChain(
        PriceCalculator.class, new DefaultChainInvocationFactory(false));
ChainInvoker<PriceCalculator, BigDecimal> invoker = chain.create();
invoker.instance().calculate(order);

List<KeyValuePair<PriceCalculator, BigDecimal>> results = invoker.result();
for (KeyValuePair<PriceCalculator, BigDecimal> kv : results) {
    System.out.println(kv.getKey().getClass().getSimpleName() + " → " + kv.getValue());
}
// 输出：
// DiscountCalculator → 90.00
// TaxCalculator → 113.00
```

#### BeanFactories / BeanProvider 示例

```java
// 通过 BeanFactories 全局容器
ChainFactory factory = new BeanFactoriesChainFactory();
PriceCalculator calc = factory.createFirstResultChain(PriceCalculator.class);

// 通过自定义 BeanProvider
ChainFactory factory2 = new BeanProviderChainFactory(myBeanProvider);
PriceCalculator calc2 = factory.createLastResultChain(PriceCalculator.class);
```

#### 自定义结果策略

```java
// 自定义 InvocationHandlerFactory
public class MyStrategyFactory implements InvocationHandlerFactory {
    @Override
    public <H> InvocationHandler create(Class<H> type, H[] handlers) {
        // 自定义调用逻辑
    }
}

PriceCalculator custom = factory.createChain(PriceCalculator.class, new MyStrategyFactory());
```

#### 日志

框架内置 SLF4J 日志，级别如下：

| 级别 | 内容 |
|------|------|
| `DEBUG` | 链创建（类型、handler 数量）、处理器发现来源、方法调用分发策略 |
| `TRACE` | 每个 handler 的逐一调用细节、返回值 |
| `WARN` | SPI 未发现任何处理器 |

---

### 5. 分布式锁

统一的锁抽象框架，支持 JVM 本地锁和 Redis 分布式锁，通过 Spring AOP 注解实现声明式加锁。

#### 架构概览

```
                    ┌──────────────────────────────────────────┐
                    │           声明式（AOP 注解）              │
                    │  @SyncLock                    @SerialTask  │
                    │      │                            │       │
                    │ SyncLockMethod-    SerialTaskMethod-       │
                    │ Interceptor        Interceptor             │
                    └──────┬───────────────────────┬───┘
                           │                       │
                    ┌──────┴───────────────────────┴───┐
                    │      AbstractLockMethodInterceptor  │  锁获取/释放
                    │      LockContextResolver            │  上下文解析
                    │      SpelLockKeyResolver            │  SpEL key 解析
                    └──────────────┬────────────────────┘
                                   │
                    ┌──────────────┴────────────────────┐
                    │          LockClient（工厂）          │
                    │  ┌─────────────┐ ┌──────────────┐  │
                    │  │SystemLock-  │ │RedissonLock- │  │
                    │  │Client       │ │Client        │  │
                    │  │(JVM 本地)   │ │(Redis 分布式)│  │
                    │  └──────┬──────┘ └──────┬───────┘  │
                    └─────────┼───────────────┼──────────┘
                              │               │
                    ┌─────────┴─────┐ ┌───────┴────────────┐
                    │  JVM 锁实现    │ │  Redis 锁实现        │
                    │  Reentrant-   │ │  Redisson-          │
                    │  Mutex        │ │  Mutex              │
                    │  Semaphore-   │ │  RedissonSemaphore- │
                    │  Mutex        │ │  Mutex              │
                    └───────────────┘ └─────────────────────┘
```

#### 核心接口

| 接口/类 | 职责 |
|---------|------|
| `Mutex` | 锁的统一抽象，提供 lock/unlock/tryLock 方法 |
| `LockClient` | 锁工厂，根据 `LockContext` 创建 `Mutex` 实例 |
| `LockContext` | 不可变的锁配置（key、公平性、等待时间、持有时间） |
| `LockContextResolver` | AOP 场景下从方法调用解析 `LockContext` 的策略接口 |
| `LockGuard` | `AutoCloseable` 包装器，支持 try-with-resources 自动释放锁 |

#### 两种锁模式

| 特性 | 同步锁 | 异步锁 |
|------|--------|--------|
| 可重入 | 是 | 否 |
| 跨线程解锁 | 不支持 | 支持 |
| 适用场景 | 普通同步方法 | 返回 `CompletionStage`/`Callable`/`Runnable`/`Supplier` 的方法 |
| JVM 实现 | `ReentrantMutex`（基于 `ReentrantLock`） | `SemaphoreMutex`（基于 `Semaphore(1)`） |
| Redis 实现 | `RedissonMutex`（基于 `RLock`） | `RedissonSemaphoreMutex`（基于 `RSemaphore`） |
| 解锁要求 | 必须由持有锁的线程调用 | 可在任意线程调用 |

#### LockClient 实现

| 实现类 | 锁存储 | 适用场景 | 依赖 |
|--------|--------|----------|------|
| `SystemLockClient` | JVM 本地 `ConcurrentHashMap` | 有限的静态 key 集合（如固定业务锁名称） | 无（内置） |
| `RedissonLockClient` | Redis | 跨进程互斥、动态 key 场景 | `org.redisson:redisson`（可选） |

#### 注解

**@SyncLock** — 方法级自动加锁

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | `String` | `""` | 锁 key，支持 SpEL 表达式。为空时使用 `类全限定名.方法名` |
| `fair` | `boolean` | `false` | 是否公平锁 |
| `waitTime` | `long` | `-1` | 等待时间（毫秒）。-1 无限等待，0 不等待，>0 超时等待 |
| `leaseTime` | `long` | `-1` | 持有时间（毫秒）。-1 使用默认值（Redisson watchdog），>0 显式指定 |

**@SerialTask** — 按任务名串行化执行

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | `String` | `""` | 任务名，支持 SpEL 表达式 |
| `fair` | `boolean` | `false` | 是否公平锁 |
| `waitTime` | `long` | `60000` | 等待时间（毫秒），默认 60 秒 |
| `leaseTime` | `long` | `-1` | 持有时间（毫秒） |
| `strategy` | `Class<? extends SerialTaskExecuteStrategy>` | `IgnoredSerialTaskExecuteStrategy` | 获取锁超时时的执行策略 |

#### 快速开始（声明式）

**1. 注册拦截器为 Spring Bean**

```java
@Configuration
public class LockConfig {

    @Bean
    public LockClient lockClient() {
        // JVM 本地锁
        return new SystemLockClient();

        // 或 Redis 分布式锁
        // return new RedissonLockClient(redissonClient, "myapp");
    }

    @Bean
    public SyncLockMethodInterceptor syncLockInterceptor(LockClient lockClient) {
        return new SyncLockMethodInterceptor(lockClient);
    }

    @Bean
    public SerialTaskMethodInterceptor serialTaskInterceptor(LockClient lockClient) {
        return new SerialTaskMethodInterceptor(lockClient);
    }
}
```

**2. 使用 @SyncLock 注解**

```java
@Service
public class OrderService {

    // 默认 key：com.example.OrderService.processOrder
    @SyncLock
    public void processDefault() { ... }

    // SpEL 动态 key
    @SyncLock("#orderId")
    public void processOrder(String orderId) { ... }

    // 公平锁 + 超时等待 + 自动释放
    @SyncLock(value = "#order.id", fair = true, waitTime = 5000, leaseTime = 30000)
    public void processOrder(Order order) { ... }

    // 异步方法自动使用异步锁（跨线程解锁）
    @SyncLock("#userId")
    public CompletableFuture<Void> asyncUpdate(String userId) {
        return CompletableFuture.runAsync(() -> {
            // 异步执行，锁在 CompletableFuture 完成后释放
        });
    }
}
```

**3. 使用 @SerialTask 注解**

```java
@Service
public class TaskService {

    // 相同 taskName 的方法串行执行，等待 60 秒
    @SerialTask("#taskId")
    public void executeTask(String taskId) { ... }

    // 自定义超时策略：超时后执行降级逻辑
    @SerialTask(value = "#reportId", waitTime = 3000, strategy = FallbackStrategy.class)
    public Report generateReport(String reportId) { ... }
}
```

#### 编程式使用

```java
// 注入 LockClient
LockClient lockClient = new SystemLockClient();

// 创建锁上下文
LockContext context = LockContext.create("inventory:product-001", true, 5000, -1);

// 方式一：手动加锁/解锁
Mutex mutex = lockClient.getMutex(context);
try {
    mutex.lockInterruptibly();
    // 业务逻辑
} finally {
    mutex.unlock();
}

// 方式二：try-with-resources（推荐）
try (LockGuard guard = LockGuard.lock(lockClient.getMutex(context))) {
    // 业务逻辑
}

// 方式三：带超时的 try-with-resources
try (LockGuard guard = LockGuard.tryLock(mutex, 5, TimeUnit.SECONDS)) {
    // 业务逻辑
} catch (SyncLockTimeoutException e) {
    // 获取锁超时
}
```

#### SpEL 动态 key

锁 key 支持通过 SpEL 表达式引用方法参数和 Spring Bean：

```java
// 引用方法参数
@SyncLock("#userId")
public void update(String userId) { ... }

// 引用嵌套属性
@SyncLock("#order.userId")
public void process(Order order) { ... }

// 引用 Spring Bean
@SyncLock("@tenantService.getCurrentTenantId() + ':' + #resourceId")
public void access(String resourceId) { ... }

// 未指定 value 时，使用默认 key：类全限定名.方法名
@SyncLock
public void process() { ... }
```

#### 异步模式自动检测

当方法返回类型为以下之一时，拦截器自动使用异步锁（支持跨线程解锁）：

| 返回类型 | 解锁时机 |
|----------|----------|
| `CompletionStage` | `whenComplete` 回调触发时 |
| `Callable` | `call()` 方法执行完毕时 |
| `Runnable` | `run()` 方法执行完毕时 |
| `Supplier` | `get()` 方法执行完毕时 |
| 其他类型 | 方法执行完毕后立即解锁 |

#### 串行任务超时策略

通过实现 `SerialTaskExecuteStrategy` 接口自定义 `@SerialTask` 获取锁超时时的行为：

```java
// 默认策略：超时后返回 null
public class IgnoredSerialTaskExecuteStrategy implements SerialTaskExecuteStrategy {
    @Override
    public Object execute(String taskName, MethodInvocation invocation) {
        return null;
    }
}

// 自定义策略：超时后抛出异常
public class ThrowOnTimeoutStrategy implements SerialTaskExecuteStrategy {
    @Override
    public Object execute(String taskName, MethodInvocation invocation) {
        throw new RuntimeException("串行任务 [" + taskName + "] 获取锁超时");
    }
}

// 使用自定义策略
@SerialTask(value = "#taskId", waitTime = 3000, strategy = ThrowOnTimeoutStrategy.class)
public void execute(String taskId) { ... }
```

#### 异常体系

```
LockException (RuntimeException)
├── SyncLockTimeoutException    — 获取锁超时
└── MutexBindingException       — 同一 key 混用同步锁和异步锁
```

#### 日志

框架内置 SLF4J 日志：

| 组件 | 级别 | 内容 |
|------|------|------|
| `SystemLockClient` | DEBUG | 锁创建、移除、清除 |
| `SystemLockClient` | TRACE | 锁实例缓存命中 |
| `SystemLockClient` | WARN | key 混用同步/异步锁 |
| `RedissonLockClient` | INFO | 初始化完成（namespace、idleTimeout） |
| `RedissonLockClient` | DEBUG | 锁创建、移除、清除 |
| `RedissonLockClient` | TRACE | 公平/非公平锁选择 |
| `ReentrantMutex` / `SemaphoreMutex` | TRACE | 加锁/解锁/尝试加锁 |
| `ReentrantMutex` / `SemaphoreMutex` | DEBUG | tryLock 超时 |
| `RedissonMutex` / `RedissonSemaphoreMutex` | TRACE | 加锁/解锁 |
| `RedissonMutex` / `RedissonSemaphoreMutex` | DEBUG | tryLock 超时、信号量空闲 TTL 管理 |
| `LockGuard` | DEBUG | 通过 LockGuard 获取锁 |
| `LockGuard` | TRACE | 锁释放 |
| `LockGuard` | WARN | 获取锁超时 |
| `SpelLockKeyResolver` | TRACE | 默认 key 回退、表达式解析结果 |
| `SpelLockKeyResolver` | WARN | 表达式求值为 null、解析失败 |
| `SyncLockContextResolver` | TRACE | 锁上下文解析结果 |
| `SyncLockContextResolver` | ERROR | 方法上未找到 @SyncLock 注解 |
| `AbstractLockMethodInterceptor` | TRACE | 锁获取/释放成功、方法执行流程 |
| `AbstractLockMethodInterceptor` | WARN | 获取锁超时、释放锁异常 |
| `AbstractLockMethodInterceptor` | ERROR | 持有锁期间业务异常 |
| `SyncLockMethodInterceptor` | DEBUG | 锁获取成功 |
| `SyncLockMethodInterceptor` | TRACE | 方法拦截、异步模式检测 |
| `SerialTaskMethodInterceptor` | WARN | 串行任务获取锁超时，执行策略名称 |

---

### 6. 定时任务

统一的调度任务框架，支持 JVM 本地和 Redis 分布式两种后端，通过 `@Task` 注解进行执行器过滤。

#### 架构概览

```
                    ┌──────────────────────────────────────────────────────┐
                    │            ScheduledTaskServiceFactory                │
                    │            （按任务名缓存服务实例）                      │
                    │  ┌──────────────────────┐ ┌─────────────────────────┐ │
                    │  │SystemScheduled-      │ │RedissonScheduled-       │ │
                    │  │TaskServiceFactory    │ │TaskServiceFactory       │ │
                    │  │(JVM 本地)            │ │(Redis 分布式)           │ │
                    │  └──────────┬───────────┘ └───────────┬─────────────┘ │
                    └─────────────┼─────────────────────────┼───────────────┘
                                  │                         │
                    ┌─────────────┴─────────────────────────┴───────────────┐
                    │     AbstractScheduledTaskServiceFactory                 │
                    │     @Task 注解过滤执行器 / ConcurrentMap 缓存服务       │
                    └──────────────────────────┬────────────────────────────┘
                                               │ create(taskName)
                                               ▼
                    ┌────────────────────────────────────────────────────────┐
                    │         ScheduledTaskService<T>                        │
                    │         schedule / scheduleAsync / cancelTask / clear  │
                    │  ┌───────────────────────┐ ┌─────────────────────────┐ │
                    │  │SystemScheduled-       │ │RedissonScheduled-       │ │
                    │  │TaskService<T>         │ │TaskService<T>           │ │
                    │  │(ScheduledExecutor-    │ │(RDelayedQueue +         │ │
                    │  │ Service)              │ │ RBlockingQueue)         │ │
                    │  └───────────────────────┘ └─────────────────────────┘ │
                    └──────────────────────────┬────────────────────────────┘
                                               │ executeTask(task)
                                               ▼
                    ┌────────────────────────────────────────────────────────┐
                    │              TaskExecutor<T>                           │
                    │              execute(TaskContext<T>)                   │
                    │         按 @Order 排序后依次执行                        │
                    │         异常不阻断，suppressed 聚合                     │
                    └────────────────────────────────────────────────────────┘
```

#### 核心接口

| 接口 | 职责 |
|------|------|
| `ScheduledTaskService<T>` | 调度任务服务，提供 schedule/scheduleAsync/cancelTask/containsTask/clear 等生命周期操作 |
| `TaskExecutor<T>` | 任务执行器，定义单个任务的执行逻辑 |
| `TaskContext<T>` | 任务执行上下文，绑定任务数据与所属调度服务 |
| `ScheduledTaskServiceFactory` | 工厂接口，按任务名创建或获取缓存的 ScheduledTaskService 实例 |

#### 注解

**@Task** — 任务执行器标记注解

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | `String[]` | `{}` | 任务名称列表。为空时匹配所有任务，非空时仅匹配数组中包含的任务名 |

#### 快速开始

```java
// 1. 定义任务执行器
@Task("order-timeout")
@Order(100)
public class OrderTimeoutExecutor implements TaskExecutor<OrderTimeoutTask> {
    @Override
    public void execute(@Nonnull TaskContext<OrderTimeoutTask> context) {
        OrderTimeoutTask task = context.getTask();
        orderService.cancelOrder(task.getOrderId());
    }
}

// 2. JVM 本地调度
List<TaskExecutor<?>> executors = Collections.singletonList(new OrderTimeoutExecutor());
ScheduledTaskServiceFactory factory = new SystemScheduledTaskServiceFactory(executors);
ScheduledTaskService<OrderTimeoutTask> service = factory.create("order-timeout");

// 调度任务，30 分钟后执行
service.schedule(new OrderTimeoutTask("order-001"), Duration.ofMinutes(30));

// 异步调度
service.scheduleAsync(new OrderTimeoutTask("order-002"), Duration.ofMinutes(30));

// 3. Redis 分布式调度
ScheduledTaskServiceFactory factory = new RedissonScheduledTaskServiceFactory(
    redissonClient, "myapp:task", executors
);
ScheduledTaskService<OrderTimeoutTask> service = factory.create("order-timeout");
// 跨进程共享，同一任务只会被一个实例执行
```

#### 执行器过滤规则

`AbstractScheduledTaskServiceFactory` 通过 `@Task` 注解过滤匹配的执行器：

- 没有 `@Task` 注解 → 匹配所有任务
- `@Task` 注解的 `value` 为空 → 匹配所有任务
- `@Task` 注解的 `value` 包含任务名 → 匹配

过滤后的执行器按 `@Order` 排序，相同任务名返回缓存的服务实例。

#### 任务重调度

对同一任务对象再次调用 `schedule` 时，会取消旧的调度并替换为新的延迟。

#### 实现对比

| 特性 | SystemScheduledTaskService | RedissonScheduledTaskService |
|------|---------------------------|------------------------------|
| 存储 | JVM 本地（ConcurrentHashMap） | Redis（RDelayedQueue + RBlockingQueue） |
| 跨实例共享 | 不支持 | 支持 |
| 延迟机制 | ScheduledExecutorService.schedule | RDelayedQueue.offer + RBlockingQueue.take |
| 序列化 | 不需要 | Kryo5Codec（任务类需可序列化） |
| 守护线程 | 无（使用 ScheduledFuture） | 独立守护线程循环 take() |
| 适用场景 | 单实例部署 | 多实例部署 |
| 额外依赖 | 无（内置） | `org.redisson:redisson`（可选） |

#### 日志

| 组件 | 级别 | 内容 |
|------|------|------|
| `AbstractScheduledTaskService` | DEBUG | 任务调度（任务名称、任务内容） |
| `AbstractScheduledTaskService` | ERROR | 任务执行异常（任务名称、任务内容、异常栈） |
| `SystemScheduledTaskService` | DEBUG | 调度任务（延迟、任务内容）、取消任务 |
| `SystemScheduledTaskService` | INFO | 清除所有待执行调度（取消数量） |
| `RedissonScheduledTaskService` | INFO | 创建服务（任务名称、队列名称）、关闭服务 |
| `RedissonScheduledTaskService` | DEBUG | 调度任务（延迟、任务内容）、取消任务、从队列取出任务 |
| `RedissonScheduledTaskService` | WARN | 守护线程被中断 |
| `RedissonScheduledTaskService` | ERROR | take 异常 |
| `TaskExecutors` | TRACE | 开始执行（任务名称、执行器数量） |
| `TaskExecutors` | WARN | 单个执行器执行异常 |
| `TaskExecutors` | ERROR | 任务完成但有异常（异常数量） |
| `AbstractScheduledTaskServiceFactory` | INFO | 创建调度任务服务（任务名称） |
| `AbstractScheduledTaskServiceFactory` | DEBUG | 执行器过滤完成（任务名称、总数、匹配数） |
| `AbstractScheduledTaskServiceFactory` | TRACE | 执行器匹配/不匹配详情 |
| `SystemScheduledTaskServiceFactory` | INFO | 创建系统调度任务服务（任务名称） |
| `RedissonScheduledTaskServiceFactory` | INFO | 创建 Redisson 调度任务服务（任务名称、命名空间） |

---

### 7. 消息队列

统一的消息队列抽象框架，支持推模式（监听器自动消费）和拉模式（poll 主动拉取），提供内存和 Redis 两种后端。

#### 架构概览

```
                      ┌─────────────────────────────────────────────────────┐
                      │                  QueueClient                         │
                      │        （按队列名缓存 MessageQueue 实例）              │
                      │  ┌──────────────────┐ ┌─────────────────────────────┐│
                      │  │MemoryQueueClient │ │RedissonQueueClient          ││
                      │  │(JVM 本地)        │ │(Redis 分布式)               ││
                      │  └────────┬─────────┘ └──────────┬──────────────────┘│
                      └───────────┼──────────────────────┼──────────────────┘
                                  │                      │
                      ┌───────────┴──────────────────────┴──────────────────┐
                      │     getQueue(name) / getDelayedQueue(name)           │
                      └───────────┬──────────────────────┬──────────────────┘
                                  │                      │
               ┌──────────────────┴─────┐    ┌───────────┴──────────────────┐
               │    MessageQueue<T>      │    │     DelayedQueue<T>          │
               │    offer / poll         │    │     offer(value, Duration)   │
               │    addListener          │    │     extends MessageQueue     │
               │  ┌────────────────────┐ │    │  ┌─────────────────────────┐│
               │  │MemoryMessageQueue  │ │    │  │MemoryDelayQueue         ││
               │  │(BlockingQueue)     │ │    │  │(DelayQueue)             ││
               │  ├────────────────────┤ │    │  ├─────────────────────────┤│
               │  │RedissonMessage-    │ │    │  │RedissonDelayQueue       ││
               │  │Queue(RBlocking-    │ │    │  │(RDelayedQueue +         ││
               │  │ Queue)             │ │    │  │ RBlockingQueue)         ││
               │  └────────────────────┘ │    │  └─────────────────────────┘│
               └─────────────────────────┘    └─────────────────────────────┘
                                  │
                      ┌───────────┴───────────────────────────────────────────┐
                      │         AbstractMessageQueue<T>                        │
                      │  守护线程 take() → Executor 异步分发                     │
                      │  消息确认机制 / autoAck / 速率限制 / 指数退避           │
                      └───────────┬───────────────────────────────────────────┘
                                  │ dispatchMessage(context)
                                  ▼
                      ┌───────────────────────────────────────────────────────┐
                      │         QueueListener<T>                              │
                      │         onMessage(MessageContext<T>)                  │
                      │         异常隔离：任一监听器异常不影响其他监听器         │
                      └───────────────────────────────────────────────────────┘
```

#### 核心接口

| 接口 | 职责 |
|------|------|
| `MessageQueue<T>` | 消息队列，支持推模式（Listener）和拉模式（poll），继承 AutoCloseable |
| `DelayedQueue<T>` | 延迟队列，扩展 MessageQueue，支持按 Duration 延迟投递 |
| `QueueClient` | 队列客户端，按名称创建和缓存 MessageQueue/DelayedQueue 实例 |
| `QueueListener<T>` | 消息监听器，注册到队列后自动接收消息回调 |
| `MessageContext<T>` | 消息上下文，提供队列名称、消息内容和确认（acknowledge）能力 |

#### 消费模型

| 模式 | 方法 | 行为 |
|------|------|------|
| 推模式 | `addListener(QueueListener)` | 注册监听器，首个监听器注册时消费线程自动启动，消息通过 Executor 异步分发 |
| 拉模式 | `poll()` / `pollAsync()` | 主动拉取消息，调用方自行控制消费节奏 |

#### 消息确认机制

- **显式确认**：监听器调用 `context.acknowledge()`，消息不再重新投递
- **自动确认**：设置 `queue.setAutoAck(true)`，所有监听器无异常时自动确认
- **未确认消息**：监听器处理完成后未确认时，消息重新入队

#### 快速开始

```java
// 1. 内存消息队列（推模式）
QueueClient client = new MemoryQueueClient();
MessageQueue<String> queue = client.getQueue("orders");
queue.setAutoAck(true);
queue.addListener(context -> {
    System.out.println("收到消息: " + context.getMessage());
});
queue.offer("order-001");
queue.offer("order-002");

// 2. 延迟队列
DelayedQueue<String> delayQueue = client.getDelayedQueue("order-timeout");
delayQueue.addListener(context -> {
    System.out.println("超时订单: " + context.getMessage());
    context.acknowledge();
});
delayQueue.offer("order-001", Duration.ofMinutes(30));

// 3. Redis 分布式队列
QueueClient redisClient = new RedissonQueueClient(redissonClient, "myapp:queue");
MessageQueue<OrderEvent> orderQueue = redisClient.getQueue("order-events");
orderQueue.setAutoAck(true);
orderQueue.addListener(context -> {
    orderProcessor.process(context.getMessage());
});
orderQueue.offer(new OrderEvent("order-001", "CREATED"));

// 4. 拉模式
String task = queue.poll();
```

#### 实现对比

| 特性 | MemoryMessageQueue | MemoryDelayQueue | RedissonMessageQueue | RedissonDelayQueue |
|------|-------------------|------------------|---------------------|--------------------|
| 底层存储 | BlockingQueue | DelayQueue | RBlockingQueue | RDelayedQueue + RBlockingQueue |
| 延迟支持 | 不支持 | 支持 | 不支持 | 支持 |
| 跨进程共享 | 不支持 | 不支持 | 支持 | 支持 |
| 序列化 | 不需要 | 不需要 | Kryo5Codec | Kryo5Codec |
| 适用场景 | 单机即时消费 | 单机延迟消费 | 分布式即时消费 | 分布式延迟消费 |
| 额外依赖 | 无（内置） | 无（内置） | `org.redisson:redisson` | `org.redisson:redisson` |

#### 日志

| 组件 | 级别 | 内容 |
|------|------|------|
| `AbstractMessageQueue` | DEBUG | 添加/移除监听器 |
| `AbstractMessageQueue` | TRACE | 消息分发到 N 个监听器 |
| `AbstractMessageQueue` | WARN | 监听器处理消息异常 |
| `DefaultMessageContext` | DEBUG | 消息已确认 |
| `DefaultMessageContext` | WARN | 消息未确认，重新入队 |
| `MemoryMessageQueue` | DEBUG | 重新投递消息 |
| `MemoryQueueClient` | DEBUG | 创建内存消息队列/延迟队列 |
| `MemoryQueueClient` | INFO | 关闭客户端（队列数量） |
| `RedissonMessageQueue` | DEBUG | 重新投递消息、队列清空 |
| `RedissonQueueClient` | DEBUG | 创建 Redisson 消息队列/延迟队列 |
| `RedissonQueueClient` | INFO | 关闭客户端（队列数量） |

---

### 8. 工厂

统一的工厂模式工具集，通过 SPI 加载具体实现。

#### 日期时间工厂

通过 `DateTimeFactories` 提供灵活的日期时间解析、比较和转换。

```java
// 解析日期时间
LocalDateTime dateTime = DateTimeFactories.parse("2024-01-15 10:30:00");
LocalDateTime dateTime2 = DateTimeFactories.parse("2024-01-15", "yyyy-MM-dd");
LocalDateTime fromTs = DateTimeFactories.fromEpochSecond(1705309800);
LocalDateTime fromMs = DateTimeFactories.fromEpochMilli(1705309800000L);

// 比较
LocalDateTime min = DateTimeFactories.min(dt1, dt2, dt3);
LocalDateTime max = DateTimeFactories.max(dt1, dt2, dt3);
LocalDate minDate = DateTimeFactories.min(date1, date2);
LocalTime maxTime = DateTimeFactories.max(time1, time2);
Instant minInstant = DateTimeFactories.min(instant1, instant2);

// 转换
long epochSecond = DateTimeFactories.toEpochSecond(dateTime);
long epochMilli = DateTimeFactories.toEpochMilli(dateTime);
```

---

#### Bean 工厂

通过 SPI 加载 `BeanProvider`，提供 `getBean`/`getBeanOrNull` 等依赖查找能力。

```java
// 按类型获取 Bean
UserService service = BeanFactories.getBean(UserService.class);

// 安全获取（不存在返回 null）
UserService service = BeanFactories.getBeanOrNull(UserService.class);

// Optional 获取
Optional<UserService> opt = BeanFactories.getBeanOptional(UserService.class);

// 获取所有实现
List<UserService> all = BeanFactories.getBeans(UserService.class);

// 按名称获取
Object bean = BeanFactories.getBean("myService");

// 懒加载
Supplier<UserService> lazy = BeanFactories.getBeanLazy(UserService.class);

// 不存在时使用默认值
UserService service = BeanFactories.getBeanOrDefault(UserService.class, new DefaultUserService());

// 不存在时通过构造器创建
UserService service = BeanFactories.getBeanOrNew(UserService.class, config);
```

---

#### JSON 工厂

通过 SPI 加载 `JsonProvider`，提供 JSON 序列化/反序列化能力。默认使用 fastjson2 实现。

```java
// 序列化
String json = JsonFactories.toJson(myObject);

// 反序列化为对象
User user = JsonFactories.toModel(json, User.class);

// 反序列化为列表
List<User> users = JsonFactories.toList(json, User.class);

// 反序列化为 Set
Set<User> userSet = JsonFactories.toSet(json, User.class);

// 反序列化为数组
User[] users = JsonFactories.toArray(json, User.class);

// 反序列化为 Map
Map<String, Object> map = JsonFactories.toMap(json);
Map<String, User> userMap = JsonFactories.toMap(json, User.class);
Map<Long, User> typedMap = JsonFactories.toMap(json, Long.class, User.class);
```

---

### 9. SPI 扩展系统

自定义 `SpiServiceLoader`，相比 `java.util.ServiceLoader` 增加了生命周期管理、按优先级排序和类加载器级缓存。

#### 声明服务提供者

```java
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
@Order(100)
public class MyService implements SomeInterface {
    // ...
}
```

在 `META-INF/services/cn.maarlakes.common.SomeInterface` 中注册：

```
com.example.MyService
```

#### 加载服务

```java
// 共享加载器（单例缓存，推荐）
SpiServiceLoader<SomeInterface> loader = SpiServiceLoader.loadShared(SomeInterface.class);

// 按优先级获取
SomeInterface first = loader.first();                    // 优先级最高的
SomeInterface last = loader.last();                      // 优先级最低的
Optional<SomeInterface> opt = loader.firstOptional();    // 安全获取

// 遍历所有
loader.stream().forEach(service -> { /* ... */ });
```

**关键注解**：

| 注解/接口 | 说明 |
|-----------|------|
| `@SpiService(lifecycle = SINGLETON)` | 单例生命周期，按类加载器缓存 |
| `@Order(value)` | 数值越小优先级越高 |
| `Ordered` 接口 | 实现 `getOrder()` 方法，等效于 `@Order` |

---

### 10. 比较器工具

```java
// 自然排序
String min = Comparators.min("banana", "apple", "cherry");  // "apple"
String max = Comparators.max("banana", "apple", "cherry");  // "cherry"

// 集合
Integer minVal = Comparators.min(Arrays.asList(3, 1, 4, 1, 5));  // 1

// 自定义比较器
String longest = Comparators.max(
        Comparator.comparingInt(String::length),
        "a", "abc", "ab");  // "abc"

// Optional 版本（空集合返回 empty）
Optional<Integer> opt = Comparators.minOptional(list);
```

---

### 11. 元组

不可变元组，支持 Tuple1 到 Tuple9。

```java
// 创建
Tuple2<String, Integer> tuple = Tuple.of("hello", 42);
Tuple3<String, Integer, Double> t3 = Tuple.of("a", 1, 3.14);

// 访问
String item1 = tuple.item1();            // "hello"
Integer item2 = tuple.item2();           // 42
Optional<String> opt = tuple.optionalItem1();

// 通过索引访问
Object val = tuple.get(0);               // "hello"
Optional<Object> optVal = tuple.getOptional(1);

// 修改（返回新元组）
Tuple2<String, Integer> modified = tuple.withItem1("world");

// 映射
Tuple2<String, Integer> mapped = tuple.mapItem1(String::toUpperCase);

// 连接
Tuple3<String, Integer, Double> t3 = tuple.concat(3.14);
Tuple4<String, Integer, String, Boolean> t4 = t3.concat(Tuple.of("x", true));
```

---

### 12. 函数式接口扩展

提供可抛出检查异常的函数式接口，弥补 `java.util.function` 不支持 checked exception 的不足。

```java
// 可抛异常的函数
Function1<String, Integer> parser = str -> Integer.parseInt(str);
// 等效于：
Function1<String, Integer> parser = str -> {
    return Integer.parseInt(str);  // 无需 try-catch
};

// 不抛异常的安全调用
Integer result = parser.applyUnchecked("42");

// 带异常处理器的安全调用
Integer result = parser.applyUnchecked("abc", e -> -1);

// 部分应用
Function0<Integer> partial = parser.acceptPartially("42");
Integer val = partial.applyUnchecked();
```

可用接口：`Function0<R>`、`Function1<T1, R>`、`Function2<T1, T2, R>`、`Function3<T1, T2, T3, R>`，
以及对应的 Consumer 系列。

---

### 13. ID 生成器

```java
// Snowflake ID
SnowflakeIdGenerator generator = new SnowflakeIdGenerator(workerId, dataCenterId);
String id = generator.generateId();

// ObjectId
ObjectIdGenerator objectIdGenerator = new ObjectIdGenerator();
String objectId = objectIdGenerator.generateId();

// Oulid
OulidGenerator oulidGenerator = new OulidGenerator();
String oulid = oulidGenerator.generateId();
```

---

### 14. 其他工具

#### Lazy 惰性加载

线程安全的延迟初始化：

```java
Lazy<ExpensiveObject> lazy = Lazy.of(() -> new ExpensiveObject());

if (!lazy.isCreated()) {
    // 尚未初始化
}

ExpensiveObject obj = lazy.apply();  // 首次调用时初始化
```

#### Ordered / @Order 排序

```java
// 注解方式
@Order(100)
public class HighPriority implements Ordered { }

// 接口方式
public class LowPriority implements Ordered {
    @Override
    public int getOrder() {
        return 999;
    }
}
```

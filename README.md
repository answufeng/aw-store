# aw-store

[![JitPack](https://jitpack.io/v/answufeng/aw-store.svg)](https://jitpack.io/#answufeng/aw-store)

基于腾讯 **MMKV** 的 Android 键值存储封装：Kotlin **属性委托** + 命令式 API，支持加密、多 `mmapId`、多进程与 **SharedPreferences** 迁移。

如果你只想最快接入并写入第一个键值，直接看下面的「5 分钟上手」即可；其它内容都可以后置按需查阅。

---

## 5 分钟上手（最小接入）

### 1) 添加依赖（JitPack）

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.github.answufeng:aw-store:1.1.0")
}
```

`implementation` 中的 **版本号与 Git / JitPack 的 tag 一致**（上例为 `1.1.0`）。  
MMKV 通过本库以 **`api`** 方式传递，一般无需再额外声明。

---

### 2) 初始化（自动 / 手动）

- **默认**：内置 `ContentProvider` 自动初始化，引入依赖后即可用。
- **手动**：需要自定义根目录、日志等时，在 `Application.onCreate()` 调用 `AwStore.init(...)`。

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AwStore.init(this, logEnabled = BuildConfig.DEBUG)
    }
}
```

注意：`rootDir` 在**同一进程**内以首次初始化为准；若已初始化后又传入不同 `rootDir`，Release 会 `Log.w`，debuggable 包会抛 `IllegalStateException`。每次调用仍会应用最新的 `logEnabled`。

自定义目录示例：`AwStore.init(this, rootDir = "${filesDir}/mmkv_custom", logEnabled = true)`。

---

### 3) 定义 Store 并读写（属性委托）

```kotlin
object UserStore : MmkvDelegate() {
    var token by string()
    var userId by long()
    var isLoggedIn by boolean()
    var score by float()
    var tags by stringSet()
    var nickname by nullableString()
    var age by nullableInt()
}

UserStore.token = "abc123"
val token = UserStore.token
UserStore.nickname = null  // 赋 null = 删除键
```

Key 可省略（默认用**属性名**），也可 `long("user_id", 0L)`。未初始化就访问存储会抛 `IllegalStateException`（请确认清单已合并 Provider 或已调用 `init`）。

---

## 目录（按常见需求跳转）

| 想做什么 | 跳转到 |
|----------|--------|
| 最短时间跑通依赖 / 初始化 / 第一次读写 | [5 分钟上手（最小接入）](#5-分钟上手最小接入) · [环境要求](#环境要求) |
| 看看支持哪些类型、迁移、多进程、加密 | [功能概览](#功能概览) · [使用指南](#使用指南) |
| JSON / Parcelable / getOrPut / 批量 edit | [使用指南](#使用指南) |
| SharedPreferences 迁移 | [SharedPreferences 迁移](#sharedpreferences-迁移) |
| 多进程监听与单进程监听 | [监听](#监听) |
| 本地构建 / CI / Demo | [本仓库与工程检查](#本仓库与工程检查) |

---

## 环境要求

| 项目 | 建议 / 最低 |
|------|------------|
| Android minSdk | 24 |
| Demo compileSdk / targetSdk | 35 |
| JDK | 17+ |
| Kotlin | 2.0+ |

---

## 功能概览

| 类别 | 能力 |
|------|------|
| 委托 / 类型 | 基本类型、ByteArray、StringSet、Nullable、Parcelable、`json<T>()` |
| API 形态 | 属性委托、`getXxx`/`putXxx`、`edit { }`、`getOrPut*`（含 StringSet、Bytes、Json）、`effectiveMmapId` |
| 安全与隔离 | `CryptKey`、AES-CFB、按 `mmapId` 分文件、`StoreConfig` |
| 进程 | `multiProcess`、跨进程 `registerContentChange`、单进程 `registerOnKeyChanged` |
| 迁移 | `SpMigration.migrate` / `migrateAll`（支持 `StoreConfig`） |
| 其它 | `sync` / `async`、`mmkvInstance`、调试日志、consumer ProGuard 规则 |

### API 分层（推荐用法）

| 层级 | 典型 API | 说明 |
|------|----------|------|
| **日常** | `string()` / `putString` / `edit { }` | 业务 Store 继承 `MmkvDelegate` 即可 |
| **进阶** | `getOrPut*`、`registerOnKeyChanged`、`SpMigration` | 懒初始化、监听、SP 迁移 |
| **底层** | `mmkvInstance`、`exportToMap` / `importFromMap` | TTL、trim、调试备份（勿当生产严谨备份） |

---

## 架构

```
┌──────────────────────────────────────────────┐
│                  业务代码                       │
├──────────────────────────────────────────────┤
│ MmkvDelegate（委托 + 命令式 + edit + 导入导出） │
│ AwStore / AwStoreLogger / AwStoreJsonAdapter │
│ CryptKey / SpMigration                        │
├──────────────────────────────────────────────┤
│                    MMKV                       │
└──────────────────────────────────────────────┘
```

---

## 使用指南

### Nullable 与 default

可区分「**键不存在**」与「**有键且有值**」；赋 `null` 会删键。支持 `nullableString(default = "unknown")` 等，详见下表。

| 委托 | 键不存在 | 赋 `null` |
|------|----------|-----------|
| `nullable*(default = …)` | 返回 default | 删键，再读仍按「不存在」走 default |

```kotlin
object UserStore : MmkvDelegate() {
    var age by nullableInt()
    var nickname by nullableString(default = "unknown")
}
```

### 命令式 API

适合动态 key：`putString`/`getString`、`putInt`/`getInt`，以及 Long / Float / Double / Boolean / Bytes / StringSet、`putParcelable`/`getParcelable`、`putJson`/`getJson`。

### 带过期时间（TTL）

使用底层 `mmkvInstance.encode(key, value, expireSeconds)`，写入后若需单进程监听请自行在业务层处理（或 `edit` 内 `mmkv.encode` + `markKeyChanged`）：

```kotlin
UserStore.mmkvInstance.encode("captcha", "abc123", 300)
```

过期后键仍存在但读取返回默认值；`contains` 仍为 `true`，`allKeys` 仍包含该键。

### 批量写入 `edit { }`

```kotlin
UserStore.edit {
    encode("key1", "value1")
    encode("key2", 42)
}
```

事务结束后会汇总触发 `registerOnKeyChanged`。带过期时间等需使用 **`mmkv.encode(…)`** 的，请在写入后调用 **`markKeyChanged(key)`**。

### getOrPut

```kotlin
val token = UserStore.getOrPutString("token") { "default" }
val profile = UserStore.getOrPutJson("profile") { UserProfile("guest", 0) }
```

支持：`getOrPutString` … `Double`、`getOrPutStringSet`、`getOrPutBytes`、`getOrPutJson`（需先 `AwStoreJsonAdapter.setAdapter`）。**同一 key** 上并发时 default **至多执行一次**（不同 key 互不阻塞）。  
`getOrPutJson`：键不存在则写入；反序列化失败时默认（`recoverOnParseError = true`）用 `defaultValue` **覆盖**；设为 `false` 则只打日志、不覆写：

```kotlin
val profile = UserStore.getOrPutJson("profile", recoverOnParseError = false) { UserProfile("guest", 0) }
```

### 导出 / 导入（调试 / 简单迁移）

```kotlin
val data = UserStore.exportToMap()
UserStore.importFromMap(data)
UserStore.importFromMap(data, notifyKeyChanges = false) // 写完再按 key 去重回调，适合大批量
```

`exportToMap` 按类型试探解码，**不适合**严谨生产备份；`importFromMap` 支持 String / 数值 / Boolean / ByteArray / `Set<String>` / `null`（删键）。

### Parcelable

```kotlin
@Parcelize
data class UserProfile(val name: String, val age: Int) : Parcelable

object DataStore : MmkvDelegate() {
    var profile by parcelable<UserProfile>()
}
```

### JSON

实现 `StoreJsonAdapter`，`AwStoreJsonAdapter.setAdapter(…)` 后使用 `json<UserProfile>()` 或 `putJson`/`getJson`。

```kotlin
class GsonAdapter : StoreJsonAdapter { /* Gson */ }
AwStoreJsonAdapter.setAdapter(GsonAdapter())
```

### 加密

生产环境请用 **Keystore / 服务端下发** 等持久化密钥，勿把密钥提交进仓库：

```kotlin
object SecureStore : MmkvDelegate(
    mmapId = "secure",
    secureCryptKey = CryptKey.fromString(keystoreBackedKey)
) {
    var password by string()
}
```

`CryptKey.fromSecureRandom()` 仅适合进程内一次性场景；卸载重装密钥会变。`CryptKey.toString()` 为 `CryptKey(****)`。

### 多实例与 `StoreConfig`

参数 ≤2 时可直接用构造函数；≥3 时推荐 `StoreConfig`：

```kotlin
object ConfigStore : MmkvDelegate(mmapId = "config") { }
object SecureStore : MmkvDelegate(StoreConfig(
    mmapId = "secure",
    secureCryptKey = CryptKey.fromString(keystoreBackedKey),
    multiProcess = true
)) { }
```

| 参数 | 含义 |
|------|------|
| `mmapId` | 实例 ID，不同 ID 不同文件 |
| `cryptKey` / `secureCryptKey` | 加密；后者优先 |
| `multiProcess` | 多进程模式 |

### 多进程

```kotlin
object SharedStore : MmkvDelegate(mmapId = "shared", multiProcess = true) {
    var counter by int()
}
```

### SharedPreferences 迁移

```kotlin
val result = SpMigration.migrate(this, "app_prefs")
SpMigration.migrate(this, "app_prefs", mmapId = "user_store")
SpMigration.migrate(this, "app_prefs", config = StoreConfig(mmapId = "user_store", multiProcess = true))
SpMigration.migrate(this, "app_prefs", deleteAfterMigration = false)
val results = SpMigration.migrateAll(this, listOf("a", "b"))
```

`MigrationResult`：`totalKeys`、`successCount`、`failedCount`、`skippedKeys`；`isSuccess` 无失败且无跳过。

### 监听

- **跨进程**（他进程写入）：`registerContentChange` / `unregisterContentChange` / `unregisterAllContentChange`
- **单进程**（本进程写入具体 key）：`registerOnKeyChanged` / `unregisterOnKeyChanged` / `clearOnKeyChangedListeners`

默认监听目标为当前 Store 的 **`effectiveMmapId`**（与 MMKV 实际实例一致）：

| 配置 | `effectiveMmapId` |
|------|-------------------|
| 默认单进程 | `DefaultMMKV` |
| `multiProcess = true` 且无 `mmapId` | `aw_default_multi` |
| 仅加密且无 `mmapId` | `aw_crypt_{stableId}` |
| 显式 `mmapId` | 即该 `mmapId` |

一般可直接 `registerContentChange { … }`；监听其它实例时传入 `targetMmapId`。同一 listener **重复注册会回调多次**；listener 内异常不会影响其它 listener。

### 工具方法

`contains` / `"key" in store`、`allKeys`、`remove`、`clear`、`totalSize`、`sync`、`async`；底层：`mmkvInstance`。

### 日志

`AwStoreLogger.enabled`、`setLogger { level, tag, msg, t -> … }`；级别 d / i / w / e。

### 全局配置（速查）

| 项 | 说明 |
|----|------|
| 自动初始化 | 清单中的 `AwStoreInitializer` |
| `AwStore.init` | 自定义 `rootDir`、日志 |
| `AwStore.rootDir` | 当前根目录 |
| `AwStoreJsonAdapter` | `setAdapter` / `clearAdapter` |

---

## 集成约定与踩坑

| 主题 | 说明 |
|------|------|
| Key 默认值 | 省略 key 时，键名 = **属性名**；重命名属性会导致“读不到旧值”。请用显式 key 保持兼容：`long("user_id", 0L)`。 |
| 赋 `null` 的语义 | `nullable*` 委托：赋 `null` = **删键**。 |
| `CryptKey` | 不要把密钥写进仓库或日志；卸载重装后 `fromSecureRandom()` 会变化，旧数据将无法解密。 |
| 多进程 | 同一份数据各进程需相同 `mmapId` 且 `multiProcess = true`；跨进程监听用 `registerContentChange`（默认 `effectiveMmapId`）。 |
| 监听 | 重复注册同一 listener 会触发多次；跨进程请确认 `effectiveMmapId` 与目标实例一致。 |

### 密钥与加密（常见误用）

| 误用 | 后果 | 建议 |
|------|------|------|
| 明文密钥进仓库 / Log | 泄露即数据泄露 | `fromSecureRandom` 或服务端下发 + 安全存储 |
| 随意换密钥不迁移 | 旧数据无法解密 | 版本化密钥或新 `mmapId` 冷启动 |
| 多进程与错误 `mmapId` | 不一致或损坏 | 按 MMKV 文档配置 `multiProcess` |

---

## 本仓库与工程检查

| 项 | 说明 |
|----|------|
| Demo | 模块 `demo/`，能力索引：[demo/DEMO_MATRIX.md](demo/DEMO_MATRIX.md) |
| 本地建议命令 | `./gradlew :aw-store:assembleRelease :aw-store:ktlintCheck :aw-store:lintRelease :demo:assembleRelease`（需 **JDK 17+**） |
| CI | [`.github/workflows/ci.yml`](.github/workflows/ci.yml)：assemble、ktlint、Lint、demo release |
| 贡献 | [CONTRIBUTING.md](CONTRIBUTING.md) |

---

## FAQ

**Key 随属性重命名丢了？**  
省略 key 时键名=属性名。重命名后请显式保留旧键名：`long("userId", 0L)`。

**加密密钥丢了？**  
`fromSecureRandom` 随安装变；跨安装需服务端密钥或 Keystore 等方案。

**多进程？**  
同一数据各进程须相同 `mmapId` 且 `multiProcess = true`；可 `registerContentChange` 感知他进程写入。

**备份？**  
`exportToMap` + `importFromMap`；复杂类型导出类型不保证精确，见上文说明。

---

## 注意事项

- **Key / StringSet**：推断 key 改名需迁移；`stringSet` 返回不可变，修改请新建集合并赋值。
- **复杂类型**：优先 Parcelable / JSON，勿使用 Java 序列化。
- **ProGuard / R8**：[consumer-rules.pro](aw-store/consumer-rules.pro) 类级保留公开 API；**自有 JSON 模型类**仍需自行 keep；MMKV 随 AAR。
- **sync / async**：关键数据可考虑 `sync()`；非关键可用 `async()`。
- **线程**：MMKV 与封装侧常规使用为线程安全场景。

---

## 许可证

Apache License 2.0，见 [LICENSE](LICENSE)。

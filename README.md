# aw-store

[![](https://jitpack.io/v/answufeng/aw-store.svg)](https://jitpack.io/#answufeng/aw-store)

基于腾讯 MMKV 封装的 Android 键值存储库，提供 Kotlin 属性委托语法实现类型安全的存储，支持加密、多实例隔离、多进程和 SharedPreferences 一键迁移。

**验证环境**：仓库 **demo** 使用 compileSdk 35 / targetSdk 35（JDK 17）。

## 文档导读

1. [工程品质与发版检查](#工程品质与发版检查) → [快速开始](#快速开始) → [Nullable / 加密 / 多进程](#nullable-类型)  
2. [演示应用](#演示应用)：[demo/DEMO_MATRIX.md](demo/DEMO_MATRIX.md)（含 **推荐手测**）  
3. **密钥**：见下文 [密钥与加密（误用防火墙）](#密钥与加密误用防火墙)

## 工程品质与发版检查

- **CI**：[`.github/workflows/ci.yml`](.github/workflows/ci.yml) — `assembleRelease`、`ktlintCheck`、`lintRelease`、`:demo:assembleRelease`。
- **本地建议**：`./gradlew :aw-store:assembleRelease :aw-store:ktlintCheck :aw-store:lintRelease :demo:assembleRelease`（**需 JDK 17+**，与 CI 一致。）
- **演示**：[demo/DEMO_MATRIX.md](demo/DEMO_MATRIX.md)；demo 菜单 **「演示清单」**。
- **上线前**：确认 **MMKV 根目录**与备份策略；加密场景校验 **CryptKey** 不落日志；多进程与 `mmapId` 隔离在目标机型验证。

### 密钥与加密（误用防火墙）

| 误用 | 后果 | 正确做法 |
|------|------|----------|
| 把 **明文密码** 当 `cryptKey` 写进仓库或 Log | 密钥泄露即数据全泄露 | 使用 `CryptKey.fromSecureRandom()` 或服务端下发 + 安全存储；禁止打日志 |
| 多版本 **随意更换密钥** 不迁移数据 | 旧数据无法解密 | 设计密钥版本与迁移策略（或新 mmapId 冷启动） |
| **多进程** 与错误 `mmapId` 混用 | 读写不一致或损坏风险 | 严格按 MMKV 文档配置 `multiProcess` 与文件路径 |

## 特性

- **属性委托语法**：`by string()` / `by int()` / `by boolean()` … 声明即存储
- **Key 自动推断**：省略 key 参数，自动使用属性名作为键名
- **19+ 数据类型**：String、Int、Long、Float、Double、Boolean、ByteArray、Set\<String\>、Parcelable、Serializable、JSON 对象，以及对应的 Nullable 版本
- **命令式 API**：`getString()` / `putString()` 等命令式读写，适用于动态 key 场景
- **安全加密**：`CryptKey` 安全包装 + AES-CFB 加密，`CryptKey.fromSecureRandom()` 引导安全实践
- **多实例隔离**：通过 `mmapId` 创建独立存储文件
- **多进程模式**：`multiProcess = true` 启用跨进程读写一致性
- **SP 一键迁移**：`SpMigration.migrate()` 使用 MMKV 原生导入，高效批量迁移
- **批量迁移**：`SpMigration.migrateAll()` 支持多个 SP 文件一次性迁移
- **跨进程数据监听**：`registerContentChange()` 按 mmapId 过滤，多 listener 不覆盖
- **JSON 对象存储**：策略接口解耦，支持 Gson/Moshi/Kotlin Serialization
- **批量写入事务**：`edit {}` 批量写入，MMKV 自动合并为一次写操作
- **getOrPut 便捷方法**：`getOrPutString()` / `getOrPutInt()` 等，key 不存在时自动写入默认值
- **数据导出/导入**：`exportToMap()` / `importFromMap()` 支持数据备份恢复
- **自动初始化**：内置 ContentProvider，零配置接入
- **同步/异步写入**：`sync()` / `async()` 控制写入策略
- **底层实例访问**：`mmkvInstance` 属性可直接访问 MMKV 高级功能
- **reified 泛型**：`parcelable<T>()` / `json<T>()` 简化用法
- **操作符语法**：`"key" in store` 检查键是否存在，`remove("a", "b")` 批量删除
- **调试日志**：可选日志输出（d/i/w/e 四级），支持自定义日志输出

## 架构

```
┌─────────────────────────────────────────────────┐
│                   用户代码                        │
├─────────────────────────────────────────────────┤
│  MmkvDelegate (属性委托 + 命令式 API)             │
│  ├─ string/int/long/float/double/boolean/bytes   │
│  ├─ nullableString/nullableInt/...               │
│  ├─ stringSet/nullableStringSet                  │
│  ├─ parcelable<T>/json<T>                        │
│  ├─ getOrPutXxx / edit / exportToMap             │
│  └─ registerContentChange / sync / async         │
├─────────────────────────────────────────────────┤
│  AwStore (初始化管理)                              │
│  AwStoreLogger (日志)                             │
│  AwStoreJsonAdapter (JSON 适配器)                 │
│  CryptKey (加密密钥包装)                           │
│  SpMigration (SP 迁移)                            │
├─────────────────────────────────────────────────┤
│                  MMKV                             │
└─────────────────────────────────────────────────┘
```

- **AwStore**：全局初始化入口，管理 MMKV 的初始化状态
- **MmkvDelegate**：核心类，提供属性委托和命令式 API，继承此类即可声明存储属性
- **SpMigration**：SharedPreferences 迁移工具，使用 MMKV 原生导入
- **CryptKey**：加密密钥的安全包装，防止密钥在日志/调试中泄露
- **AwStoreJsonAdapter**：JSON 序列化适配器管理，解耦 JSON 库依赖
- **AwStoreLogger**：调试日志工具，支持自定义输出

## 引入

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}

// app/build.gradle.kts
dependencies {
    implementation("com.github.answufeng:aw-store:1.0.0")
}
```

> MMKV 以 `api` 方式传递，无需额外声明。

## 快速开始

### 初始化

**方式一：自动初始化（推荐）**

库内置 ContentProvider，无需手动调用 `init()`，引入即用。

**方式二：手动初始化**

如需自定义存储目录或启用日志，在 `Application.onCreate()` 中调用：

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AwStore.init(this, logEnabled = BuildConfig.DEBUG)
    }
}
```

### 自定义存储目录

```kotlin
AwStore.init(this, rootDir = "${filesDir}/mmkv_custom", logEnabled = true)
```

### 定义存储

```kotlin
object UserStore : MmkvDelegate() {
    var token by string()              // key="token", default=""
    var userId by long()               // key="userId", default=0L
    var isLoggedIn by boolean()        // key="isLoggedIn", default=false
    var score by float()               // key="score", default=0f
    var tags by stringSet()            // key="tags", default=emptySet()
    var nickname by nullableString()   // key="nickname", default=null
    var age by nullableInt()           // key="age", default=null
}
```

> Key 参数可省略，自动使用属性名。也可显式指定：`var userId by long("user_id", 0L)`

### 读写

```kotlin
UserStore.token = "abc123"
val token = UserStore.token

UserStore.nickname = null       // 删除键
val nick = UserStore.nickname   // null

UserStore.age = null            // 删除键
val age = UserStore.age         // null（区分"不存在"和"值为0"）
```

## 演示应用

`demo` 覆盖委托类型、加密、多 mmap、迁移、监听等；索引见 [demo/DEMO_MATRIX.md](demo/DEMO_MATRIX.md)。

## Nullable 类型

Nullable 委托可区分"key 不存在"和"值为默认值"的场景：

```kotlin
object UserStore : MmkvDelegate() {
    var age by nullableInt()          // null = key不存在, 0 = 值为0
    var timestamp by nullableLong()
    var score by nullableFloat()
    var ratio by nullableDouble()
    var enabled by nullableBoolean()  // null = key不存在, false = 值为false
    var data by nullableBytes()
    var nickname by nullableString()
    var tags by nullableStringSet()   // null = key不存在, emptySet = 值为空集合
}
```

> 赋值 `null` 时自动删除对应键。

Nullable 委托还支持 `default` 参数，指定 key 不存在时的返回值（区别于赋值为 `null` 的行为）：

```kotlin
var nickname by nullableString(default = "unknown")  // key不存在时返回"unknown"，赋null时删除key
var age by nullableInt(default = -1)                 // key不存在时返回-1，赋null时删除key
var enabled by nullableBoolean(default = true)       // key不存在时返回true，赋null时删除key
var tags by nullableStringSet(default = setOf("default"))  // key不存在时返回默认集合
```

| 委托方法 | key 不存在时 | 赋值 `null` 时 |
|----------|-------------|---------------|
| `nullableString(default)` | 返回 `default`（默认 null） | 删除 key，返回 `default` |
| `nullableInt(default)` | 返回 `default`（默认 null） | 删除 key，返回 `default` |
| `nullableLong(default)` | 返回 `default`（默认 null） | 删除 key，返回 `default` |
| `nullableFloat(default)` | 返回 `default`（默认 null） | 删除 key，返回 `default` |
| `nullableDouble(default)` | 返回 `default`（默认 null） | 删除 key，返回 `default` |
| `nullableBoolean(default)` | 返回 `default`（默认 null） | 删除 key，返回 `default` |
| `nullableBytes(default)` | 返回 `default`（默认 null） | 删除 key，返回 `default` |
| `nullableStringSet(default)` | 返回 `default`（默认 null） | 删除 key，返回 `default` |

## 命令式 API

除了属性委托，还提供命令式 get/put 方法，适用于动态 key 场景：

```kotlin
// 基本类型
UserStore.putString("dynamic_key", "value")
val value = UserStore.getString("dynamic_key")

UserStore.putInt("counter", 42)
val counter = UserStore.getInt("counter")

// 其他类型同理：putLong/getLong, putFloat/getFloat, putDouble/getDouble,
// putBoolean/getBoolean, putBytes/getBytes, putStringSet/getStringSet

// Parcelable
UserStore.putParcelable("profile", userProfile)
val profile = UserStore.getParcelable<UserProfile>("profile")

// Serializable
UserStore.putSerializable("config", appConfig)
val config = UserStore.getSerializable<AppConfig>("config")

// JSON 对象
UserStore.putJson("profile", userProfile)
val profile = UserStore.getJson<UserProfile>("profile")
```

## 批量写入事务

```kotlin
UserStore.edit {
    encode("key1", "value1")
    encode("key2", 42)
    encode("key3", true)
}
```

> MMKV 会自动将多次写入合并为一次写操作，提高写入效率。
> `edit {}` 内部会追踪所有变更的 key，并在事务结束后触发 `registerOnKeyChanged` 回调。

## getOrPut 便捷方法

如果 key 不存在，自动写入默认值并返回：

```kotlin
val token = UserStore.getOrPutString("token") { "default_token" }
val count = UserStore.getOrPutInt("launch_count") { 0 }
val lastTime = UserStore.getOrPutLong("last_time") { System.currentTimeMillis() }
```

> 支持：`getOrPutString`、`getOrPutInt`、`getOrPutLong`、`getOrPutBoolean`、`getOrPutFloat`、`getOrPutDouble`

## 数据导出/导入

```kotlin
// 导出所有键值对
val data: Map<String, Any?> = UserStore.exportToMap()

// 导入键值对（已存在的键会被覆盖）
val count = UserStore.importFromMap(data)
```

> 支持的类型：String、Int、Long、Float、Double、Boolean、ByteArray、Set\<String\>、null

## 加密存储

### 推荐方式：CryptKey 安全随机密钥

```kotlin
object SecureStore : MmkvDelegate(
    secureCryptKey = CryptKey.fromSecureRandom()
) {
    var password by string()
}
```

> **重要**：`CryptKey.fromSecureRandom()` 应在 `object` 声明中使用，确保只初始化一次。避免在函数中重复调用，否则每次会生成不同密钥导致数据无法读取。

### 从字符串/字节数组创建

```kotlin
val key1 = CryptKey.fromString("my_secret_key")   // 不推荐硬编码
val key2 = CryptKey.fromBytes(byteArrayOf(0x01, 0x02))
```

> `CryptKey.toString()` 返回 `"CryptKey(****)"`，不会泄露密钥内容。
> 不同 `cryptKey` 会自动使用不同的 MMKV 实例，不会相互覆盖。

## 多实例隔离

```kotlin
object UserStore : MmkvDelegate() { ... }
object ConfigStore : MmkvDelegate(mmapId = "config") { ... }
object SecureStore : MmkvDelegate(secureCryptKey = CryptKey.fromSecureRandom()) { ... }
```

当配置参数较多时，可以使用 `StoreConfig` 数据类：

```kotlin
object SecureStore : MmkvDelegate(StoreConfig(
    mmapId = "secure",
    secureCryptKey = CryptKey.fromSecureRandom(),
    multiProcess = true
)) {
    var password by string()
}
```

| 参数 | 说明 |
|------|------|
| `mmapId` | MMKV 实例 ID，不同 ID 对应不同存储文件 |
| `cryptKey` | 加密密钥字符串，传入后使用 AES-CFB 加密 |
| `secureCryptKey` | 安全加密密钥（CryptKey），优先级高于 cryptKey |
| `multiProcess` | 是否启用多进程模式，默认 false |

## 多进程模式

```kotlin
object SharedStore : MmkvDelegate(mmapId = "shared", multiProcess = true) {
    var counter by int()
}
```

> 启用后使用 `MMKV.MULTI_PROCESS_MODE`，确保主进程与子进程（如 :sync 服务）的读写一致性。

## Parcelable 存储

```kotlin
@Parcelize
data class UserProfile(val name: String, val age: Int) : Parcelable

object DataStore : MmkvDelegate() {
    var profile by parcelable<UserProfile>()
}

DataStore.profile = UserProfile("Alice", 25)
val p = DataStore.profile  // UserProfile?
```

> `parcelable` 委托返回可空类型，赋值 `null` 时自动删除对应键。

## Serializable 存储

```kotlin
object DataStore : MmkvDelegate() {
    @Suppress("DEPRECATION")
    var config by serializable<AppConfig>()
}
```

> ⚠️ Java 序列化性能较差且存在兼容性风险，已标记为 `@Deprecated(WARNING)`，推荐优先使用 `parcelable` 或 `json` 委托。

## JSON 对象存储

支持任意 data class，通过策略接口解耦 JSON 库：

```kotlin
// 1. 实现 StoreJsonAdapter（以 Gson 为例）
class GsonAdapter : StoreJsonAdapter {
    private val gson = Gson()
    override fun <T : Any> toJson(value: T, clazz: KClass<T>): String = gson.toJson(value)
    override fun <T : Any> fromJson(json: String, clazz: KClass<T>): T =
        gson.fromJson(json, clazz.java)
}

// 2. 注册适配器
AwStoreJsonAdapter.setAdapter(GsonAdapter())

// 3. 使用
data class UserProfile(val name: String, val age: Int)

object DataStore : MmkvDelegate() {
    var profile by json<UserProfile>()
}
```

> 库本身不强制依赖任何 JSON 库，用户可自由选择 Gson/Moshi/Kotlin Serialization。

## SharedPreferences 迁移

```kotlin
// 迁移单个 SP 文件
val result = SpMigration.migrate(this, "app_prefs")
Log.d("Migration", "$result")

// 迁移到指定 MMKV 实例
SpMigration.migrate(this, "app_prefs", mmapId = "user_store")

// 迁移到加密实例
SpMigration.migrate(this, "app_prefs", cryptKey = "secret")

// 迁移到 CryptKey 加密实例
SpMigration.migrate(this, "app_prefs", secureCryptKey = CryptKey.fromSecureRandom())

// 迁移到多进程实例
SpMigration.migrate(this, "app_prefs", mmapId = "shared", multiProcess = true)

// 迁移后保留原 SP 数据
SpMigration.migrate(this, "app_prefs", deleteAfterMigration = false)

// 批量迁移多个 SP 文件（每个 SP 文件迁移到同名的 MMKV 实例）
val results = SpMigration.migrateAll(this, listOf("app_prefs", "user_prefs", "settings"))
```

### MigrationResult

```kotlin
data class MigrationResult(
    val totalKeys: Int,       // SP 中总键数
    val successCount: Int,    // 成功迁移数
    val failedCount: Int,     // 失败数（= totalKeys - successCount - skippedKeys.size）
    val skippedKeys: List<String>  // 跳过的键（null 值）
)
```

> 使用 MMKV 原生 `importFromSharedPreferences` 批量导入，高效可靠。迁移是幂等的，已迁移的 SP 文件再次调用不会重复写入。`deleteAfterMigration` 使用同步写入（`commit()`），确保迁移返回后 SP 数据已被清除。

## 跨进程数据变化监听

当 MMKV 实例被其他进程修改时，可收到通知：

```kotlin
// 监听当前实例
UserStore.registerContentChange { mmapID ->
    Log.d("Store", "Instance $mmapID changed by other process")
}

// 监听指定实例
UserStore.registerContentChange(targetMmapId = "shared") { mmapID ->
    Log.d("Store", "Shared store changed: $mmapID")
}

// 取消指定监听
UserStore.unregisterContentChange(listener)

// 取消当前实例的所有监听
UserStore.unregisterAllContentChange()
```

> 支持按 mmapId 过滤，多个 listener 不会互相覆盖。此监听仅在其他进程修改数据时触发。多个 `MmkvDelegate` 实例的监听器互不影响，只有当所有实例的监听器都被移除后，才会注销全局通知。

## 单进程键变更回调

当通过属性委托或命令式 API 写入/删除键时，可收到通知：

```kotlin
// 注册回调
UserStore.onKeyChanged { key ->
    Log.d("Store", "Key changed: $key")
}

// 取消指定回调
UserStore.removeOnKeyChanged(listener)

// 取消所有回调
UserStore.clearOnKeyChangedListeners()
```

> 与 `registerContentChange` 的区别：`onKeyChanged` 在同进程内触发，能获取具体变更的键名；`registerContentChange` 仅在其他进程修改时触发。

## 访问底层 MMKV 实例

```kotlin
// 获取底层 MMKV 实例，用于高级功能
val mmkv = UserStore.mmkvInstance
mmkv.trim()
mmkv.close()
```

> 通常不需要直接使用，库已封装了常用操作。

## 工具方法

```kotlin
// 检查键是否存在（支持操作符语法）
UserStore.contains("token")       // Boolean
"token" in UserStore              // 等价写法

// 获取所有键名
UserStore.allKeys()                // Array<String>

// 删除单个键
UserStore.remove("token")

// 批量删除（vararg 语法）
UserStore.remove("token", "userId")

// 清空所有数据
UserStore.clear()

// 存储文件大小（字节）
UserStore.totalSize()              // Long

// 同步写入（等待写入磁盘完成，适用于关键数据）
UserStore.sync()

// 异步写入（立即返回，后台写入，适用于非关键数据）
UserStore.async()
```

## 调试日志

```kotlin
// 方式一：初始化时启用
AwStore.init(this, logEnabled = BuildConfig.DEBUG)

// 方式二：单独启用
AwStoreLogger.enabled = BuildConfig.DEBUG

// 自定义日志输出（如重定向到文件或远程日志系统）
AwStoreLogger.setLogger { level, tag, msg, t ->
    when (level) {
        AwStoreLogger.Level.DEBUG -> Log.d(tag, msg, t)
        AwStoreLogger.Level.INFO -> Log.i(tag, msg, t)
        AwStoreLogger.Level.WARN -> Log.w(tag, msg, t)
        AwStoreLogger.Level.ERROR -> Log.e(tag, msg, t)
    }
}
```

| 方法 | 级别 | 用途 |
|------|------|------|
| `AwStoreLogger.d(msg)` | DEBUG | 初始化、迁移等常规日志 |
| `AwStoreLogger.i(msg)` | INFO | 重要操作信息 |
| `AwStoreLogger.w(msg)` | WARN | 警告信息 |
| `AwStoreLogger.e(msg, t?)` | ERROR | 错误信息 |

## 全局配置

| 配置 | 方法 | 说明 |
|------|------|------|
| 自动初始化 | 内置 ContentProvider | 引入即用，无需手动调用 |
| 手动初始化 | `AwStore.init(context)` | 需自定义目录时使用 |
| 自定义目录 | `AwStore.init(context, rootDir)` | 指定 MMKV 文件存储根目录 |
| 启用日志 | `AwStore.init(context, logEnabled = true)` | 初始化时一并启用日志 |
| 调试日志 | `AwStoreLogger.enabled = true` | 单独启用日志 |
| 自定义日志 | `AwStoreLogger.setLogger { ... }` | 自定义日志输出 |
| 存储根目录 | `AwStore.rootDir` | 获取当前 MMKV 根目录路径 |
| JSON 适配器 | `AwStoreJsonAdapter.setAdapter(adapter)` | 使用 json() 委托前必须设置 |
| 清除 JSON 适配器 | `AwStoreJsonAdapter.clearAdapter()` | 调试或集成验证时移除注册，之后需再 `setAdapter` |

## 依赖说明

| 依赖 | 版本 | 用途 |
|------|------|------|
| MMKV | 2.0.1 | 高性能键值存储引擎 |

| 环境要求 | 版本 |
|----------|------|
| minSdk | 24+ |
| Kotlin | 2.0+ |
| JDK | 17+ |

## FAQ

### Key 重命名后旧数据怎么办？

省略 key 参数时，属性名即为 MMKV 键名。重命名属性会导致 key 变化，旧数据不会自动迁移。解决方案：

```kotlin
// 旧代码：var userId by long()
// 新代码：显式指定旧 key
var userId by long("userId")  // 保持与旧属性名一致
```

### 加密密钥丢失后怎么办？

如果加密密钥丢失（如 `CryptKey.fromSecureRandom()` 生成的密钥因卸载重装丢失），之前加密的数据将无法解密。建议：

- 对于需要跨安装保留的加密数据，使用 `CryptKey.fromString()` 配合服务端下发密钥
- 或使用 Android Keystore 存储密钥种子

### 多进程使用注意事项

- 必须设置 `multiProcess = true`
- 所有访问同一数据的进程都需要使用相同的 `mmapId` 和 `multiProcess` 配置
- 跨进程数据变化可通过 `registerContentChange()` 监听

### 数据备份恢复方案

```kotlin
// 备份
val backup = UserStore.exportToMap()

// 恢复
UserStore.importFromMap(backup)
```

> 注意：`exportToMap()` 仅支持基本类型，Parcelable/Serializable/JSON 对象以原始形式导出。

## 注意事项

- **初始化顺序**：使用自动初始化时无需关心；手动初始化时，访问任何存储属性前必须调用 `AwStore.init()`，否则抛出 `IllegalStateException`
- **Key 自动推断**：省略 key 时使用属性名，重命名属性会导致 key 变化（旧数据需迁移）
- **StringSet 不可变性**：`stringSet` 返回的是不可变集合，修改时需创建新集合并重新赋值
- **Nullable 类型**：`nullableXxx()` 委托赋值 `null` 时会删除对应键，读取不存在的键返回 `null`
- **CryptKey 安全**：`CryptKey.fromSecureRandom()` 应在 `object` 声明中使用，确保只初始化一次；每次安装生成不同密钥，卸载重装后之前加密的数据将无法解密
- **Serializable 风险**：Java 序列化性能较差且存在兼容性风险，已标记为 `@Deprecated(WARNING)`，推荐优先使用 `parcelable` 或 `json`
- **ProGuard**：库已内置 consumer ProGuard 规则，无需额外配置
- **多进程**：使用 `multiProcess = true` 启用多进程模式
- **sync vs async**：`sync()` 等待数据写入磁盘完成后再返回，适用于关键数据；`async()` 立即返回在后台写入，适用于非关键数据
- **线程安全**：`MmkvDelegate` 的所有操作都是线程安全的，MMKV 内部保证了并发读写的安全性

## 许可证

Apache License 2.0，详见 [LICENSE](LICENSE)。

*文档修订：2026-04-22。*

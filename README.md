# aw-store

[![](https://jitpack.io/v/answufeng/aw-store.svg)](https://jitpack.io/#answufeng/aw-store)

基于腾讯 MMKV 封装的 Android 键值存储库，提供 Kotlin 属性委托语法实现类型安全的存储，支持加密、多实例隔离、多进程和 SharedPreferences 一键迁移。

## 特性

- **属性委托语法**：`by string()` / `by int()` / `by boolean()` … 声明即存储
- **Key 自动推断**：省略 key 参数，自动使用属性名作为键名
- **15+ 数据类型**：String、Int、Long、Float、Double、Boolean、ByteArray、Set\<String\>、Parcelable、Serializable、JSON 对象，以及对应的 Nullable 版本
- **安全加密**：`CryptKey` 安全包装 + AES-CFB 加密，`CryptKey.fromSecureRandom()` 引导安全实践
- **多实例隔离**：通过 `mmapId` 创建独立存储文件
- **多进程模式**：`multiProcess = true` 启用跨进程读写一致性
- **SP 一键迁移**：`SpMigration.migrate()` 使用 MMKV 原生导入，高效批量迁移
- **跨进程数据监听**：`registerContentChange()` 按 mmapId 过滤，多 listener 不覆盖
- **JSON 对象存储**：策略接口解耦，支持 Gson/Moshi/Kotlin Serialization
- **自动初始化**：内置 ContentProvider，零配置接入
- **同步/异步写入**：`sync()` / `async()` 控制写入策略
- **reified 泛型**：`parcelable<T>()` / `serializable<T>()` / `json<T>()` 简化用法
- **调试日志**：可选日志输出，方便排查问题

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

如需自定义存储目录，在 `Application.onCreate()` 中调用：

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AwStore.init(this)
    }
}
```

### 自定义存储目录

```kotlin
AwStore.init(this, rootDir = "${filesDir}/mmkv_custom")
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
}
```

> 赋值 `null` 时自动删除对应键。

## 加密存储

### 推荐方式：CryptKey 安全随机密钥

```kotlin
object SecureStore : MmkvDelegate(
    secureCryptKey = CryptKey.fromSecureRandom()
) {
    var password by string()
}
```

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
    var config by serializable<AppConfig>()
}
```

> 利用 MMKV 原生 Serializable 支持，零额外依赖。

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
// 迁移默认 SP 到默认 MMKV
val result = SpMigration.migrate(this, "app_prefs")
Log.d("Migration", "$result")

// 迁移到指定 MMKV 实例
SpMigration.migrate(this, "app_prefs", mmapId = "user_store")

// 迁移到加密实例
SpMigration.migrate(this, "app_prefs", cryptKey = "secret")

// 迁移到多进程实例
SpMigration.migrate(this, "app_prefs", mmapId = "shared", multiProcess = true)

// 迁移后保留原 SP 数据
SpMigration.migrate(this, "app_prefs", deleteAfterMigration = false)
```

### MigrationResult

```kotlin
data class MigrationResult(
    val totalKeys: Int,       // SP 中总键数
    val successCount: Int,    // 成功迁移数
    val failedCount: Int,     // 失败数
    val skippedKeys: List<String>  // 跳过的键（null 值）
)
```

> 使用 MMKV 原生 `importFromSharedPreferences` 批量导入，高效可靠。迁移是幂等的，已迁移的 SP 文件再次调用不会重复写入。

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

// 取消所有监听
UserStore.unregisterAllContentChange()
```

> 支持按 mmapId 过滤，多个 listener 不会互相覆盖。此监听仅在其他进程修改数据时触发。

## 工具方法

```kotlin
// 检查键是否存在
UserStore.contains("token")       // Boolean

// 获取所有键名
UserStore.allKeys()                // Array<String>

// 删除单个键
UserStore.remove("token")

// 批量删除
UserStore.remove(arrayOf("token", "userId"))

// 清空所有数据
UserStore.clear()

// 存储文件大小（字节）
UserStore.totalSize()              // Long

// 同步写入（等待写入磁盘完成）
UserStore.sync()

// 异步写入（立即返回，后台写入）
UserStore.async()
```

## 全局配置

| 配置 | 方法 | 说明 |
|------|------|------|
| 自动初始化 | 内置 ContentProvider | 引入即用，无需手动调用 |
| 手动初始化 | `AwStore.init(context)` | 需自定义目录时使用 |
| 自定义目录 | `AwStore.init(context, rootDir)` | 指定 MMKV 文件存储根目录 |
| 调试日志 | `AwStoreLogger.enabled = BuildConfig.DEBUG` | 开启后输出迁移、初始化等日志 |
| 存储根目录 | `AwStore.rootDir` | 获取当前 MMKV 根目录路径 |
| JSON 适配器 | `AwStoreJsonAdapter.setAdapter(adapter)` | 使用 json() 委托前必须设置 |

## 依赖说明

| 依赖 | 版本 | 用途 |
|------|------|------|
| MMKV | 2.0.1 | 高性能键值存储引擎 |

## 注意事项

- **初始化顺序**：使用自动初始化时无需关心；手动初始化时，访问任何存储属性前必须调用 `AwStore.init()`，否则抛出 `IllegalStateException`
- **Key 自动推断**：省略 key 时使用属性名，重命名属性会导致 key 变化（旧数据需迁移）
- **StringSet 不可变性**：`stringSet` 返回的是不可变集合，修改时需创建新集合并重新赋值
- **Nullable 类型**：`nullableXxx()` 委托赋值 `null` 时会删除对应键，读取不存在的键返回 `null`
- **CryptKey 安全**：`CryptKey.fromSecureRandom()` 每次安装生成不同密钥，卸载重装后之前加密的数据将无法解密
- **ProGuard**：库已内置 consumer ProGuard 规则，无需额外配置
- **多进程**：使用 `multiProcess = true` 启用多进程模式

## 许可证

Apache License 2.0，详见 [LICENSE](LICENSE)。

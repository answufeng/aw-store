# aw-store

[![](https://jitpack.io/v/answufeng/aw-store.svg)](https://jitpack.io/#answufeng/aw-store)

基于腾讯 MMKV 封装的 Android 键值存储库，提供 Kotlin 属性委托语法实现类型安全的存储，支持加密、多实例隔离和 SharedPreferences 一键迁移。

## ✨ 功能特性

- **属性委托语法**：`by string()` / `by int()` / `by boolean()` … 声明即存储
- **9 种数据类型**：String、Int、Long、Float、Double、Boolean、ByteArray、Set\<String\>、Parcelable
- **Nullable String**：`nullableString()` 支持 null 值读写
- **多实例隔离**：通过 `mmapId` 创建独立存储文件
- **AES-CFB 加密**：通过 `cryptKey` 启用加密存储
- **SP 一键迁移**：`SpMigration.migrate()` 从 SharedPreferences 无缝迁移
- **跨进程数据监听**：`registerContentChange()` 监听其他进程的数据变化
- **同步/异步写入**：`sync()` / `async()` 控制写入策略
- **reified Parcelable**：泛型简化 Parcelable 委托用法
- **调试日志**：可选日志输出，方便排查问题

## 📦 引入方式

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

## 🚀 快速开始

### 初始化

在 `Application.onCreate()` 中调用一次：

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
    var token by string("token", "")
    var userId by long("user_id", 0L)
    var isLoggedIn by boolean("is_logged_in", false)
    var score by float("score", 0f)
    var tags by stringSet("tags")
    var nickname by nullableString("nickname")  // 支持 null
}
```

### 读写

```kotlin
UserStore.token = "abc123"
val token = UserStore.token

UserStore.nickname = null       // 删除键
val nick = UserStore.nickname   // null
```

## 🔐 加密存储

```kotlin
object SecureStore : MmkvDelegate(cryptKey = "my_secret_key") {
    var password by string("password", "")
}
```

> 不同 `cryptKey` 会自动使用不同的 MMKV 实例，不会相互覆盖。

## 📦 多实例隔离

```kotlin
object UserStore : MmkvDelegate() { ... }
object ConfigStore : MmkvDelegate(mmapId = "config") { ... }
object SecureStore : MmkvDelegate(cryptKey = "key123") { ... }
```

| 参数 | 说明 |
|------|------|
| `mmapId` | MMKV 实例 ID，不同 ID 对应不同存储文件 |
| `cryptKey` | 加密密钥，传入后使用 AES-CFB 加密 |

## 📋 Parcelable 存储

```kotlin
@Parcelize
data class UserProfile(val name: String, val age: Int) : Parcelable

object DataStore : MmkvDelegate() {
    // reified 简化版
    var profile by parcelable<UserProfile>("profile")
}

DataStore.profile = UserProfile("Alice", 25)
val p = DataStore.profile  // UserProfile?
```

> `parcelable` 委托返回可空类型，赋值 `null` 时自动删除对应键。

## 🔄 SharedPreferences 迁移

```kotlin
// 迁移默认 SP 到默认 MMKV
val result = SpMigration.migrate(this, "app_prefs")
Log.d("Migration", "$result")

// 迁移到指定 MMKV 实例
SpMigration.migrate(this, "app_prefs", mmapId = "user_store")

// 迁移到加密实例
SpMigration.migrate(this, "app_prefs", cryptKey = "secret")

// 迁移后保留原 SP 数据
SpMigration.migrate(this, "app_prefs", deleteAfterMigration = false)
```

### MigrationResult

```kotlin
data class MigrationResult(
    val totalKeys: Int,       // SP 中总键数
    val successCount: Int,    // 成功迁移数
    val failedCount: Int,     // 失败数
    val skippedKeys: List<String>  // 跳过的键（null 值或未知类型）
)
```

> 迁移是幂等的，已迁移的 SP 文件再次调用不会重复写入。当存在失败项时不会删除原 SP 数据。

## 👂 跨进程数据变化监听

当 MMKV 实例被其他进程修改时，可收到通知：

```kotlin
UserStore.registerContentChange { mmapID ->
    Log.d("Store", "Instance $mmapID changed by other process")
}

// 取消监听
UserStore.unregisterContentChange()
```

> 注意：此监听仅在其他进程修改数据时触发，当前进程的修改不会触发回调。回调参数为被修改的 MMKV 实例 ID（mmapID），而非具体键名。

## 🛠 工具方法

```kotlin
// 检查键是否存在
UserStore.contains("token")       // Boolean

// 获取所有键名
UserStore.allKeys()                // Array<String>

// 删除单个键
UserStore.remove("token")

// 批量删除
UserStore.remove(arrayOf("token", "user_id"))

// 清空所有数据
UserStore.clear()

// 存储文件大小（字节）
UserStore.totalSize()              // Long

// 同步写入（等待写入磁盘完成）
UserStore.sync()

// 异步写入（立即返回，后台写入）
UserStore.async()
```

## ⚙️ 全局配置

| 配置 | 方法 | 说明 |
|------|------|------|
| 初始化 | `AwStore.init(context)` | 必须在 Application.onCreate 中调用 |
| 自定义目录 | `AwStore.init(context, rootDir)` | 指定 MMKV 文件存储根目录 |
| 调试日志 | `AwStoreLogger.enabled = BuildConfig.DEBUG` | 开启后输出迁移、初始化等日志 |
| 存储根目录 | `AwStore.rootDir` | 获取当前 MMKV 根目录路径 |

## 📋 依赖说明

| 依赖 | 版本 | 用途 |
|------|------|------|
| MMKV | 2.0.1 | 高性能键值存储引擎 |

## ⚠️ 注意事项

- **初始化顺序**：访问任何存储属性前必须调用 `AwStore.init()`，否则抛出 `IllegalStateException`
- **StringSet 不可变性**：`stringSet` 返回的是不可变集合，修改时需创建新集合并重新赋值
- **Parcelable 空值**：`parcelable` 委托赋值 `null` 时会删除对应键，读取不存在的键返回 `null`
- **ProGuard**：库已内置 consumer ProGuard 规则，无需额外配置
- **多进程**：当前封装使用 `SINGLE_PROCESS_MODE`，多进程场景需自行扩展

## 许可证

Apache License 2.0，详见 [LICENSE](LICENSE)。

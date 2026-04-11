# aw-store

基于腾讯 MMKV 的键值存储库，提供 Kotlin 属性委托语法实现类型安全的存储，支持加密。

## 引入

```kotlin
dependencies {
    implementation("com.github.answufeng:aw-store:1.0.0")
}
```

## 功能特性

- 属性委托语法读写 MMKV
- 9 种数据类型：String、Int、Long、Float、Double、Boolean、ByteArray、Set<String>、Parcelable
- 多实例隔离（mmapId）
- AES-CFB 加密（cryptKey）
- SharedPreferences 一键迁移工具（SpMigration）
- reified Parcelable 委托简化用法

## 使用示例

```kotlin
// 初始化
BrickStore.init(this)

// 定义存储
object UserStore : MmkvDelegate() {
    var token by string("token", "")
    var userId by long("user_id", 0L)
    var isLoggedIn by boolean("is_logged_in", false)
}

// 加密存储
object SecureStore : MmkvDelegate(cryptKey = "secret") {
    var password by string("password", "")
}

// 读写
UserStore.token = "abc123"
val token = UserStore.token

// Parcelable（reified 简化版）
object DataStore : MmkvDelegate() {
    var profile by parcelable<UserProfile>("profile")
}

// SP 迁移
SpMigration.migrate(this, "old_prefs")
```

## 许可证

Apache License 2.0，详见 [LICENSE](LICENSE)。

# aw-store

Key-value storage library based on Tencent MMKV. Provides Kotlin property delegate for type-safe storage with encryption support.

## Installation

Add the dependency in your module-level `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.answufeng:aw-store:1.0.0")
}
```

Make sure you have the JitPack repository in your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

## Features

- Property delegate syntax for MMKV read/write
- 9 data types: String, Int, Long, Float, Double, Boolean, ByteArray, Set<String>, Parcelable
- Multi-instance isolation with mmapId
- AES-CFB encryption with cryptKey
- SharedPreferences migration tool (SpMigration)
- Reified parcelable delegate for simplified usage

## Usage

```kotlin
// Initialize
BrickStore.init(this)

// Define store
object UserStore : MmkvDelegate() {
    var token by string("token", "")
    var userId by long("user_id", 0L)
    var isLoggedIn by boolean("is_logged_in", false)
}

// Encrypted store
object SecureStore : MmkvDelegate(cryptKey = "secret") {
    var password by string("password", "")
}

// Read/Write
UserStore.token = "abc123"
val token = UserStore.token

// Parcelable (reified)
object DataStore : MmkvDelegate() {
    var profile by parcelable<UserProfile>("profile")
}

// SP Migration
SpMigration.migrate(this, "old_prefs")
```

## License

Apache License 2.0. See [LICENSE](LICENSE) for details.

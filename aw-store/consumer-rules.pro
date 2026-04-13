# aw-store consumer ProGuard rules

# MMKV native bridge
-keep class com.tencent.mmkv.** { *; }

# Public API — AwStore entry point
-keep public class com.answufeng.store.AwStore {
    public *;
}

# Public API — MmkvDelegate and its delegate factory methods
-keep public class com.answufeng.store.MmkvDelegate {
    public *;
}

# Public API — SpMigration
-keep public class com.answufeng.store.SpMigration {
    public *;
}

# Public API — MigrationResult
-keep public class com.answufeng.store.MigrationResult {
    public *;
}

# Parcelable implementations used with parcelable() delegate
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

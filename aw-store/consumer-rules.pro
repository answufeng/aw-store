# aw-store consumer ProGuard rules

# MMKV native bridge
-keep class com.tencent.mmkv.** { *; }

# AwStore
-keep public class com.answufeng.store.AwStore {
    public *** init(...);
    public *** isInitialized();
    public *** rootDir();
}

# MmkvDelegate
-keep public class com.answufeng.store.MmkvDelegate {
    public <init>(...);
    public *;
}

# StoreConfig
-keep public class com.answufeng.store.StoreConfig { *; }

# SpMigration
-keep public class com.answufeng.store.SpMigration {
    public *** migrate(...);
    public *** migrateAll(...);
}

# MigrationResult
-keep public class com.answufeng.store.MigrationResult { *; }

# CryptKey
-keep public class com.answufeng.store.CryptKey {
    public static *** fromString(...);
    public static *** fromBytes(...);
    public static *** fromSecureRandom(...);
}

# AwStoreLogger
-keep public class com.answufeng.store.AwStoreLogger {
    public *** enabled;
    public static *** setLogger(...);
}
-keep public class com.answufeng.store.AwStoreLogger$Level { *; }

# StoreJsonAdapter
-keep public interface com.answufeng.store.StoreJsonAdapter { *; }
-keep public class com.answufeng.store.AwStoreJsonAdapter {
    public static *** setAdapter(...);
}

# AwStoreInitializer
-keep public class com.answufeng.store.AwStoreInitializer { *; }

# JSON data classes warning
# If you use json<T>() delegate, make sure to keep your data classes:
# -keep class com.yourpackage.YourDataClass { *; }
# Or use @SerializedName (Gson) / @Json (Moshi) annotations

# Parcelable CREATOR
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Kotlin property delegate
-keepclassmembers class * implements kotlin.properties.ReadWriteProperty {
    *;
}

# Kotlin metadata
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations

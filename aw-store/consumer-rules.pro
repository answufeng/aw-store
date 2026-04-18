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
    public *** clear();
    public *** remove(...);
    public *** contains(...);
    public *** allKeys();
    public *** totalSize();
    public *** sync();
    public *** async();
    public *** registerContentChange(...);
    public *** unregisterContentChange(...);
    public *** unregisterAllContentChange(...);
    public *** getString(...);
    public *** putString(...);
    public *** getInt(...);
    public *** putInt(...);
    public *** getLong(...);
    public *** putLong(...);
    public *** getFloat(...);
    public *** putFloat(...);
    public *** getDouble(...);
    public *** putDouble(...);
    public *** getBoolean(...);
    public *** putBoolean(...);
    public *** getBytes(...);
    public *** putBytes(...);
    public *** getStringSet(...);
    public *** putStringSet(...);
    public *** getJson(...);
    public *** putJson(...);
}

# SpMigration
-keep public class com.answufeng.store.SpMigration {
    public *** migrate(...);
}

# MigrationResult
-keep public class com.answufeng.store.MigrationResult { *; }

# CryptKey
-keep public class com.answufeng.store.CryptKey {
    public static *** fromString(...);
    public static *** fromBytes(...);
    public static *** fromSecureRandom(...);
}

# StoreJsonAdapter
-keep public interface com.answufeng.store.StoreJsonAdapter { *; }
-keep public class com.answufeng.store.AwStoreJsonAdapter {
    public static *** setAdapter(...);
}

# AwStoreInitializer
-keep public class com.answufeng.store.AwStoreInitializer { *; }

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

# Kotlin metadata
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations

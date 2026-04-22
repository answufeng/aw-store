# aw-store consumer ProGuard rules
# 此文件中的规则会自动应用到引用本库的宿主应用中

# ===========================================================
# 基础属性保留
# ===========================================================

-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations

# ===========================================================
# 核心公开 API
# ===========================================================

# AwStore 初始化入口
-keep class com.answufeng.store.AwStore {
    public static void init(android.content.Context, java.lang.String, boolean);
    public static boolean isInitialized();
    public static java.lang.String getRootDir();
}

# 批量编辑 API（与 edit {} 同文件对外可见）
-keep class com.answufeng.store.MmkvEditScope { *; }

# MmkvDelegate 核心存储类
-keep class com.answufeng.store.MmkvDelegate {
    public <init>(...);
    public com.tencent.mmkv.MMKV getMmkvInstance();
    public *** clear();
    public *** remove(...);
    public boolean contains(java.lang.String);
    public java.lang.String[] allKeys();
    public long totalSize();
    public *** sync();
    public *** async();
    public *** registerOnKeyChanged(...);
    public *** unregisterOnKeyChanged(...);
    public *** clearOnKeyChangedListeners();
    public *** registerContentChange(...);
    public *** unregisterContentChange(...);
    public *** unregisterAllContentChange();
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
    public *** getParcelable(...);
    public *** getSerializable(...);
    public *** putSerializable(...);
    public *** getOrPutString(...);
    public *** getOrPutInt(...);
    public *** getOrPutLong(...);
    public *** getOrPutBoolean(...);
    public *** getOrPutFloat(...);
    public *** getOrPutDouble(...);
    public java.util.Map exportToMap();
    public int importFromMap(java.util.Map);
    public *** getJson(...);
    public *** putJson(...);
    public *** edit(...);
}

# StoreConfig 配置类
-keep class com.answufeng.store.StoreConfig { *; }

# SpMigration 迁移工具
-keep class com.answufeng.store.SpMigration {
    public static com.answufeng.store.MigrationResult migrate(android.content.Context, java.lang.String, java.lang.String, java.lang.String, com.answufeng.store.CryptKey, boolean, boolean);
    public static java.util.List migrateAll(android.content.Context, java.util.List, boolean);
}

# MigrationResult 迁移结果
-keep class com.answufeng.store.MigrationResult { *; }

# CryptKey 加密密钥包装
-keep class com.answufeng.store.CryptKey {
    public static com.answufeng.store.CryptKey fromString(java.lang.String);
    public static com.answufeng.store.CryptKey fromBytes(byte[]);
    public static com.answufeng.store.CryptKey fromSecureRandom(int);
    public java.lang.String getValue();
}

# AwStoreLogger 日志工具
-keep class com.answufeng.store.AwStoreLogger {
    public static boolean isEnabled();
    public static void setEnabled(boolean);
    public static void setLogger(...);
    public static void d(java.lang.String);
    public static void i(java.lang.String);
    public static void w(java.lang.String, java.lang.Throwable);
    public static void e(java.lang.String, java.lang.Throwable);
}
-keep class com.answufeng.store.AwStoreLogger$Level { *; }

# StoreJsonAdapter JSON 适配器接口
-keep public interface com.answufeng.store.StoreJsonAdapter { *; }

# AwStoreJsonAdapter JSON 适配器管理器
-keep class com.answufeng.store.AwStoreJsonAdapter {
    public static void setAdapter(com.answufeng.store.StoreJsonAdapter);
    public static java.lang.String toJson(java.lang.Object, kotlin.reflect.KClass);
    public static java.lang.Object fromJson(java.lang.String, kotlin.reflect.KClass);
}

# AwStoreInitializer ContentProvider 自动初始化
-keep class com.answufeng.store.AwStoreInitializer { *; }

# ===========================================================
# 用户自定义数据类（需由使用者自行配置）
# ===========================================================

# 如果使用 json<T>() 委托，请保持你的 data class：
#   -keep class com.yourpackage.YourDataClass { *; }
# 或使用注解：
#   Gson: @SerializedName
#   Moshi: @Json
#   Kotlin Serialization: @SerialName

# ===========================================================
# 系统类型支持
# ===========================================================

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

# Kotlin 元数据
-keep class kotlin.Metadata { *; }

# 枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

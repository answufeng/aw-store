# aw-store ProGuard Rules
# 此文件用于库自身的 release 构建混淆规则
# Consumer-facing rules（供使用者混淆时使用）位于 consumer-rules.pro

# ===========================================================
# 保留公共 API 和 MMKV 相关类
# ===========================================================

# 保留所有公共类
-keep class com.answufeng.store.** { *; }

# 保留 MMKV 相关类
-keep class com.tencent.mmkv.** { *; }

# 保留 Kotlin 反射和元数据
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes Exceptions
-keep class kotlin.Metadata { *; }

# ===========================================================
# 保留枚举和 sealed class
# ===========================================================

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===========================================================
# 保留 DSL 相关类
# ===========================================================

-keep class kotlin.jvm.functions.** { *; }
-keepclassmembers class kotlin.** { *; }
-keep class kotlin.properties.** { *; }

# ===========================================================
# 保留 Kotlin 序列化相关
# ===========================================================

-keepattributes *Serializer*
-keepclassmembers class kotlin.Metadata {
    *;
}

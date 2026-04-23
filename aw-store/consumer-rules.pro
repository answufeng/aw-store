# aw-store consumer ProGuard / R8 rules (applied to the hosting app)
# Kotlin `object` / properties compile to patterns that differ from naive Java `public static` keeps;
# use class-level keeps for the public API surface.

-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations

# Public API
-keep class com.answufeng.store.AwStore { *; }
-keep class com.answufeng.store.MmkvDelegate { *; }
-keep class com.answufeng.store.MmkvEditScope { *; }
-keep class com.answufeng.store.StoreConfig { *; }
-keep class com.answufeng.store.SpMigration { *; }
-keep class com.answufeng.store.MigrationResult { *; }
-keep class com.answufeng.store.CryptKey { *; }
-keep class com.answufeng.store.AwStoreLogger { *; }
-keep class com.answufeng.store.AwStoreLogger$Level { *; }
-keep interface com.answufeng.store.StoreJsonAdapter { *; }
-keep class com.answufeng.store.AwStoreJsonAdapter { *; }
-keep class com.answufeng.store.AwStoreInitializer { *; }

# User model types (json / reflection); host app should add -keep for their own classes.
# Gson: @SerializedName  Moshi: @Json  Kotlin Serialization: @SerialName

# Parcelable CREATOR
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Serializable (library still supports deprecated Serializable paths)
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keep class kotlin.Metadata { *; }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

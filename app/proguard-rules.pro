# ThingsPath R8/ProGuard Rules
# ===========================================

# ---------- Gson ----------
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.thingspath.data.remote.model.** { *; }
-keep class com.thingspath.data.remote.D1QueryRequest { *; }
-keep class com.thingspath.data.remote.D1Result { *; }
-keep class com.thingspath.data.remote.D1Error { *; }
-keep class com.thingspath.data.remote.D1Meta { *; }
-keep class com.thingspath.data.local.db.ItemEntity { *; }
-keep class com.thingspath.data.model.** { *; }

# ---------- Room ----------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
-keep class com.thingspath.data.local.db.ThingsPathDatabase { *; }
-keep class com.thingspath.data.local.db.ItemDao { *; }

# ---------- Hilt / Dagger ----------
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn dagger.hilt.**
-keep class com.thingspath.ThingsPathApp { *; }
-keep class com.thingspath.ui.MainActivity { *; }

# ---------- Retrofit ----------
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepclassmembers,allowshrinking class * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation interface com.thingspath.data.remote.D1ApiService
-keep,allowobfuscation interface com.thingspath.data.remote.api.SiliconFlowApi

# ---------- Coroutines ----------
-dontwarn kotlinx.coroutines.**

# ---------- DataStore ----------
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
-keepclassmembers class * extends java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ---------- General ----------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================================================
# VibeUp R8 / ProGuard rules
#
# Ordering note: these rules must be correct BEFORE isMinifyEnabled is turned
# on. R8 renames classes and strips members it cannot see being used, and two
# subsystems in this app resolve types by name at runtime:
#   1. Java serialization of the saved playback session (resolves by class name
#      + implicit serialVersionUID, which changes if the class is renamed).
#   2. Gson, which reads DTO fields reflectively.
# Getting either wrong fails silently at runtime, not at build time.
# ============================================================================

# Keep generic signatures (Gson TypeToken, Retrofit return types), annotations
# (@SerializedName, @GET/@Query), and inner-class metadata.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations

# Readable stack traces from release crashes.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile


# ---------------------------------------------------------------------------
# 1. Java-serialized playback session  (PROTECTS "resume where you left off")
#
# PlayerManager writes/reads playback_state.bin with ObjectOutputStream /
# ObjectInputStream. Java serialization resolves the class by NAME and verifies
# an implicit serialVersionUID derived from the class name, fields and methods.
# If R8 renames or strips any of it, readObject() throws InvalidClassException
# and every saved session is lost. Keeping these classes whole keeps the
# implicit UID stable.
# ---------------------------------------------------------------------------
-keep class com.vibeup.android.domain.model.** { *; }
-keep class com.vibeup.android.service.PlayerManager$PlaybackState { *; }

# Standard serialization members for anything else Serializable.
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}


# ---------------------------------------------------------------------------
# 2. Gson DTOs — fields are only ever read reflectively, so R8 cannot see them
#    being used and would otherwise strip them.
# ---------------------------------------------------------------------------
-keep class com.vibeup.android.data.remote.dto.** { *; }

# LyricsOvhResponse is declared in the api package, not the dto package.
-keep class com.vibeup.android.data.remote.api.LyricsOvhResponse { *; }
-keepclassmembers class com.vibeup.android.data.remote.api.** {
    <fields>;
}

# Anything annotated for Gson.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken


# ---------------------------------------------------------------------------
# 3. Retrofit / OkHttp
#    Retrofit ships consumer rules, but the API interfaces here use suspend
#    functions with many defaulted @Query params, which generate synthetic
#    $default bridges that must survive.
# ---------------------------------------------------------------------------
-keep,allowobfuscation interface com.vibeup.android.data.remote.api.**
-keepclassmembers,allowobfuscation interface com.vibeup.android.data.remote.api.** {
    @retrofit2.http.* <methods>;
}
-keep class kotlin.coroutines.Continuation
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**


# ---------------------------------------------------------------------------
# 4. Kotlin / coroutines metadata
# ---------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}


# ---------------------------------------------------------------------------
# 5. Enum valueOf() — ThemeManager does VibeTheme.valueOf(savedString)
# ---------------------------------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


# ---------------------------------------------------------------------------
# 6. Strip verbose/debug logging from release builds.
#    PlayerManager alone has ~27 Log calls, several on per-track-transition
#    paths. Note the string interpolation still runs unless the whole call is
#    removed, which is what -assumenosideeffects does.
# ---------------------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}


# ---------------------------------------------------------------------------
# 7. Third-party libraries that ship their own consumer rules (Room, Hilt,
#    Media3, Coil, Firebase) need nothing here. These just silence warnings.
# ---------------------------------------------------------------------------
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**

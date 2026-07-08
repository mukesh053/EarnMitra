# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep all classes in our own package to prevent any reflection, serialization, or layout instantiation crashes
-keep class com.example.** { *; }

# Keep ViewModel classes and their constructors intact for reflection/instantiation
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep AppDatabase and Room classes
-keep class * extends androidx.room.RoomDatabase { <init>(...); }
-keep class androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.**

# Keep Moshi JSON annotations and codegen adapters
-keep @com.squareup.moshi.JsonQualifier public @interface *
-keep class * {
    @com.squareup.moshi.JsonClass <init>(...);
}
-keep class *JsonAdapter { *; }
-dontwarn com.squareup.moshi.**

# Keep Retrofit and OkHttp classes
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn retrofit2.**
-keepclassmembernames class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Keep Firebase models and Firestore property names
-keep class com.google.firebase.** { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-dontwarn com.google.firebase.**

# Keep Google Play Core, Play Services & ML Kit
-keep class com.google.android.play.core.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.android.gms.**
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.play.core.**

# Ktor refers to JVM-specific classes not available on Android
-dontwarn java.lang.management.**

# Keep Kotlin coroutines classes
-dontwarn kotlinx.coroutines.**

# Preserve the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# Skip slow optimization passes to speed up compilation under container constraints
-dontoptimize



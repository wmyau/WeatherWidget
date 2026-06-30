-keepattributes *Annotation*

# kotlinx.serialization generates code at compile time — no reflection, no field-keep needed.
# The library ships its own consumer rules; these cover any gaps.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# OkHttp/Okio — suppress warnings, keep TLS platform detection
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.internal.platform.** { *; }

# WorkManager
-keep class androidx.work.** { *; }

# Keep ViewModel constructors so ViewModelProvider can instantiate them
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Add project specific ProGuard rules here.
-keepattributes *Annotation*

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Hilt
-keep class dagger.hilt.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }

# Google API Client (Drive)
-keep class com.google.api.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}

# Suppress warnings for missing Apache HttpClient classes (removed in Android 6.0)
-dontwarn org.apache.http.**
-dontwarn com.google.api.client.http.apache.v2.**
-dontwarn com.google.api.client.googleapis.apache.v2.**

# Add project specific ProGuard rules here.

# Keep Firebase classes
-keepattributes Signature
-keepattributes *Annotation*

# Firebase Realtime Database
-keepclassmembers class com.elites.fullcharge.data.** {
    *;
}

# Keep data classes for Firebase serialization
-keep class com.elites.fullcharge.data.ChatMessage { *; }
-keep class com.elites.fullcharge.data.EliteUser { *; }
-keep class com.elites.fullcharge.data.AllTimeRecord { *; }
-keep class com.elites.fullcharge.data.ChatEvent { *; }

# Jsoup
-keeppackagenames org.jsoup.nodes

# Coil
-dontwarn coil.**

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**

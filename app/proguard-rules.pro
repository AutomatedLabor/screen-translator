# Keep ML Kit classes and GMS internal classes.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# Preserve generic signatures and inner class metadata needed by ML Kit reflection.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Suppress warnings about missing optional GMS / ML Kit transitive classes.
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

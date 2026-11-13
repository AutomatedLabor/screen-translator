# Keep ML Kit classes and GMS internal classes.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# Preserve generic signatures and inner class metadata needed by ML Kit reflection.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Suppress warnings about missing optional GMS / ML Kit transitive classes.
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# Keep all of the app's own classes — the app is small (~16 source files) and runtime
# testing isn't available in CI, so this belt-and-suspenders prevents any reflection /
# serialization edge case from stripping a class. The R8 size win comes from removing
# unused LIBRARY code (Compose, AndroidX, ML Kit transitive deps), not from obfuscating
# the app's own code. Manifest-referenced classes are auto-kept by AGP regardless.
-keep class com.translator.screen.** { *; }

# Keep kotlinx.coroutines (it ships consumer rules, but be explicit for safety).
-keep class kotlinx.coroutines.** { *; }

# Preserve annotations + signatures used by ML Kit / Compose reflection.
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeInvisibleParameterAnnotations, AnnotationDefault

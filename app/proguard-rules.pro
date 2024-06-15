-keepattributes SourceFile, LineNumberTable

-renamesourcefileattribute SourceFile
-repackageclasses

-ignorewarnings
-dontwarn
-dontnote

-dontwarn android.arch.**
-dontwarn android.lifecycle.**
-keep class android.arch.** { *; }
-keep class android.lifecycle.** { *; }

-dontwarn androidx.arch.**
-dontwarn androidx.lifecycle.**
-keep class androidx.arch.** { *; }
-keep class androidx.lifecycle.** { *; }

-dontwarn androidx.core.**
-keep class androidx.core.** { *; }
-dontwarn androidx.media.**
-keep class androidx.media.** { *; }

-dontwarn com.google.android.material.**
-keep class com.google.android.material.** { *; }

-dontwarn com.levkopo.**
-keep class com.levkopo.** { *; }

-dontwarn com.bumptech.glide.**
-keep class com.bumptech.glide.** { *; }

-dontwarn com.psoffritti.slidingpanel.**
-keep class com.psoffritti.slidingpanel.** { *; }

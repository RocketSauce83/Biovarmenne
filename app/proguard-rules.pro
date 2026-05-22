# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Biovarmenne specific
-keep class com.rocketsauce83.biovarmenne.** { *; }

# Accessibility Service - must not be obfuscated
-keep class * extends android.accessibilityservice.AccessibilityService { *; }

# Biometric
-keep class androidx.biometric.** { *; }

# Security Crypto / EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# Tink missing classes - safe to ignore, never used in our app
-dontwarn com.google.api.client.http.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**
-dontwarn org.joda.time.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.*
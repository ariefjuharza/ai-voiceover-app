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

#-keep public class com.google.android.gms.**
#-keep public class io.bidmachine.**
#-keep class io.bidmachine.protobuf.**
#-keep class * extends com.appodeal.ads.modules.common.internal.service.Service
#-keep @androidx.annotation.Keep class *

# Keep only the necessary classes
-keep public class com.rpmstudio.** { *; }

# Don't warn about missing classes that you know are not used
-dontwarn ru.ok.tracer.manifest.TracerLiteManifest

# Assume that certain methods have no side effects
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

-keep class com.bytedance.component.sdk.annotation.** { *; }
-keep class com.bytedance.sdk.component.uchain.** { *; }
-keep class com.bytedance.JProtect { *; }
-keep class com.bytedance.sdk.openadsdk.** { *; }
-keep class com.facebook.infer.annotation.** { *; }


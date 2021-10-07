# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\FEMI\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesour8cefileattribute SourceFile

-keepattributes *Annotation*
-keepclassmembers class com.google.**.R$* {
    public static <fields>;
}
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}

-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault

-keep class .R
-keep class **.R$* {
    <fields>;
}

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep public class com.davidfadare.notes.**
-keep class android.support.v7.widget.**{*;}
-keep class com.fasterxml.** {*;}
-keep class com.google.*{*;}
-keep class * implements com.coremedia.iso.boxes.Box {*;}
-keep class com.android.vending.billing.**

-dontwarn com.coremedia.iso.boxes.*
-dontwarn com.googlecode.mp4parser.authoring.tracks.mjpeg.**
-dontwarn com.googlecode.mp4parser.authoring.tracks.ttml.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn java.lang.**
-dontwarn javax.servlet.**
-dontwarn org.apache.**
-dontwarn org.codehaus.**


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
-keep class com.kt.apps.core.tv.model.** {*;}
-keep class com.kt.apps.core.tv.datasource.** {*;}
-keep class com.kt.apps.core.utils** {*;}
-keepclassmembers class com.google.firebase.database.GenericTypeIndicator { *; }
-keep class cn.pedant.SweetAlert.** {*; }
-dontwarn com.google.protobuf.java_com_google_ads_interactivemedia_v3__sdk_1p_binary_b0308732GeneratedExtensionRegistryLite$Loader
-dontwarn javax.xml.stream.Location
-dontwarn javax.xml.stream.XMLEventReader
-dontwarn javax.xml.stream.XMLInputFactory
-dontwarn javax.xml.stream.events.Attribute
-dontwarn javax.xml.stream.events.Characters
-dontwarn javax.xml.stream.events.StartElement
-dontwarn javax.xml.stream.events.XMLEvent
-dontwarn javax.xml.stream.events.XMLEvent
-dontwarn com.google.protobuf.**
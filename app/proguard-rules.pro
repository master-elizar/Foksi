-keepattributes *Annotation*, InnerClasses, Signature
-keep,includedescriptorclasses class com.foksi.app.data.backup.**$$serializer { *; }
-keepclassmembers class com.foksi.app.data.backup.** {
    *** Companion;
}
-keepclasseswithmembers class com.foksi.app.data.backup.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.foksi.app.notifications.** { *; }
-keep class com.foksi.app.widgets.** { *; }
-dontwarn kotlinx.serialization.**

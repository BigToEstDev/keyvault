# ProGuard rules of the desktop release (packageRelease*). The rules of Compose itself are added by its plugin.

# The JNI bridge to the rust core. The symbols exported by rust/pass-ffi carry the package, the class and the
# method names (Java_ru_kino_dev_ffi_PassFfi_invoke): removed or renamed, the link to the native library breaks at
# runtime with UnsatisfiedLinkError - neither compilation nor tests see it.
-keep,includedescriptorclasses class ru.kino.dev.ffi.PassFfi {
    native <methods>;
}

# kotlinx.serialization - the rules of its documentation: the serializers of @Serializable classes are found through
# their companions and INSTANCE (the ffi dto-s, the routes of the navigation, the json of the stores).
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# FileKit - the dialogs of files (pass-docs, Step 92): JNA on every OS, DBus and the portal of XDG on Linux - the rules of
# its documentation.
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-keep class org.freedesktop.dbus.** { *; }
-keep class io.github.vinceglb.filekit.dialogs.platform.xdg.** { *; }
-keepattributes Signature,InnerClasses,RuntimeVisibleAnnotations

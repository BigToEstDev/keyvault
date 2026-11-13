# R8 rules of the android app. Libraries (kotlinx.serialization, Koin, ...) bring their own consumer rules.

# The JNI bridge to the rust core. The symbols exported by rust/pass-ffi carry the package, the class and the
# method names (Java_ru_kino_dev_ffi_PassFfi_invoke): renamed by R8, the link to the native library breaks at
# runtime with UnsatisfiedLinkError - neither compilation nor tests see it.
-keep,includedescriptorclasses class ru.kino.dev.ffi.PassFfi {
    native <methods>;
}

# Line numbers in the stack traces of the release - with mapping.txt of the release (a file of its GitHub release)
# a reported crash is retraced to the source. The names of the source files are hidden behind one word.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

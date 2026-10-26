# Keep custom R8 rules here only when required by release-build tests.
# Avoid broad keep rules that disable useful shrinking or obfuscation.

# SQLCipher accesses SQLiteDatabase.mNativeHandle through JNI. R8 cannot infer
# that native field lookup from Java references alone, so preserve the SQLCipher
# classes and member names required by the native library in minified release builds.
-keep class net.sqlcipher.** { *; }

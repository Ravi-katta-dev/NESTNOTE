# Keep custom R8 rules here only when required by release-build tests.
# Avoid broad keep rules that disable useful shrinking or obfuscation.

# SQLCipher 4.5.4 performs JNI field lookups on net.sqlcipher.database.SQLiteDatabase.
# The reproduced release crash names mNativeHandle as missing during native loading.
# Preserve the SQLCipher API and JNI-visible members when R8 minifies the app.
# This is scoped to the SQLCipher namespace; it does not disable app shrinking.
-keep class net.sqlcipher.** { *; }

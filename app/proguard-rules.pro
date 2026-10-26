# Keep custom R8 rules here only when required by release-build tests.
# Avoid broad keep rules that disable useful shrinking or obfuscation.
# Add SQLCipher-specific rules only after a minified-release regression test proves
# that R8 changes a JNI-referenced member and the rule fixes that test.

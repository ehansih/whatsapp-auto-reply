# Keep R8 happy with missing javax.annotation classes (from Tink/security-crypto)
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**

# Keep encrypted shared preferences
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# Keep Room entities
-keep class com.ehansih.whatsapprules.Rule { *; }

# Keep coroutines
-keepnames class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

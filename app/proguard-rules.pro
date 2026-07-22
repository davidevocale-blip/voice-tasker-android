# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Room entities
-keep class com.voicetasker.app.data.local.entity.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep WorkManager workers
-keep class com.voicetasker.app.worker.** { *; }

# Supabase & Ktor
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.voicetasker.app.**$$serializer { *; }
-keepclassmembers class com.voicetasker.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.voicetasker.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.voicetasker.app.data.auth.SupabaseProfile { *; }

# Google Play Billing
-keep class com.android.vending.billing.** { *; }

# Credentials / Google Identity
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.** { *; }

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

-keep class com.recipegrabber.domain.llm.** { *; }
-keep class com.recipegrabber.data.local.entity.** { *; }
-keep class com.recipegrabber.data.remote.** { *; }

-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

-keep class com.google.android.libraries.identity.googleid.** { *; }

# Add project specific ProGuard rules here.

  # Keep all Notion SDK classes
  -keep class notion.api.v1.** { *; }

  # Keep Gson classes
  -keepattributes Signature
  -keepattributes *Annotation*
  -keep class com.google.gson.** { *; }
  -keep class * implements com.google.gson.TypeAdapterFactory
  -keep class * implements com.google.gson.JsonSerializer
  -keep class * implements com.google.gson.JsonDeserializer

  # Keep OkHttp
  -keepattributes Signature
  -keepattributes *Annotation*
  -keep class okhttp3.** { *; }
  -keep interface okhttp3.** { *; }
  -dontwarn okhttp3.**

  # Keep Kotlin metadata
  -keepattributes *Annotation*, InnerClasses
  -dontnote kotlinx.serialization.AnnotationsKt


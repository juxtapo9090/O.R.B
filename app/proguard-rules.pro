# Add project specific ProGuard rules here.
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

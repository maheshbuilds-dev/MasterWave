# MasterWave Proguard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}
-dontwarn org.schabi.newpipe.extractor.**
-dontwarn io.ktor.**

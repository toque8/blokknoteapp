# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.RoomDatabase { *; }

# RichEditor
-keep class jp.wasabeef.richeditor.** { *; }
-dontwarn jp.wasabeef.richeditor.**

# Keep application class
-keep public class space.blokknote.BlokknoteApplication

# Keep Hilt
-keep @javax.inject.Inject class *
-keepclassmembers class * {
    @javax.inject.Inject *;
}
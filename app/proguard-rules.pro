# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclasseswithmembers class * {
    @androidx.room.Dao <methods>;
}

# RichEditor
-keep class jp.wasabeef.richeditor.** { *; }
-dontwarn jp.wasabeef.richeditor.**

# Application class
-keep public class space.blokknote.BlokknoteApp

# Hilt (Dagger)
-keep class **.*Hilt* { *; }
-keep class **.*_Factory { *; }
-keep class **.*_Module { *; }
-keep class **.*_MembersInjector { *; }
-keep class **.*_InjectAdapter { *; }
-keep class **.*_Factory { *; }
-keep class dagger.hilt.internal.GeneratedComponent { *; }
-keep class dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }
-keep class dagger.hilt.android.internal.managers.ActivityComponentManager { *; }
-keep class dagger.hilt.android.internal.managers.ServiceComponentManager { *; }
-keep class dagger.hilt.android.internal.managers.FragmentComponentManager { *; }
-keep class dagger.hilt.android.internal.managers.ViewComponentManager { *; }
-keep class dagger.hilt.android.internal.managers.ViewWithFragmentComponentManager { *; }
-keep class dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }
-keep class dagger.hilt.android.internal.lifecycle.HiltViewModelMap { *; }
-keep class dagger.hilt.android.internal.lifecycle.HiltWrapper_HiltViewModelFactory { *; }
-keep class dagger.hilt.android.internal.lifecycle.HiltWrapper_HiltViewModelMap { *; }

# Keep all injected fields and methods
-keepclassmembers class * {
    @javax.inject.Inject *;
    @dagger.hilt.android.scopes.ActivityScoped *;
    @dagger.hilt.android.scopes.FragmentScoped *;
    @dagger.hilt.android.scopes.ServiceScoped *;
    @dagger.hilt.android.scopes.ViewModelScoped *;
}

# Prevent obfuscation of types which are used in generated code
-keep,allowobfuscation @interface javax.inject.*
-keep,allowobfuscation @interface dagger.hilt.*
-keep,allowobfuscation @interface dagger.hilt.android.*
-keep,allowobfuscation @interface dagger.hilt.android.entrypoint.*
-keep,allowobfuscation @interface dagger.hilt.android.lifecycle.*
-keep,allowobfuscation @interface dagger.hilt.android.scopes.*
-keep,allowobfuscation @interface dagger.hilt.internal.*

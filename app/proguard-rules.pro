# Meridian11 LLC — Awake (com.meridian11.books.awake) ProGuard / R8 Rules

# Keep dynamic launcher activity-aliases and all manifest entry points referenced by ComponentName
-keep class com.meridian11.books.awake.AwakeApp { *; }
-keep class com.meridian11.books.awake.ui.ToggleTrampolineActivity { *; }
-keep class com.meridian11.books.awake.ui.ShortcutActionActivity { *; }
-keep class com.meridian11.books.awake.ui.SettingsDialogActivity { *; }
-keep class com.meridian11.books.awake.service.KeepAwakeService { *; }
-keep class com.meridian11.books.awake.service.KeepAwakeTileService { *; }
-keep class com.meridian11.books.awake.widget.AwakeSquircleWidgetProvider { *; }
-keep class com.meridian11.books.awake.receiver.SystemEventReceiver { *; }

# Keep domain state and models
-keep class com.meridian11.books.awake.domain.** { *; }

# Keep Jetpack DataStore & Kotlinx Coroutines
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
-dontwarn kotlinx.coroutines.**

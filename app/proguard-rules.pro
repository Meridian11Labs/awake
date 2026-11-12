# Meridian11 LLC — Awake (com.meridian11.books.awake) ProGuard / R8 Rules

# Keep manifest entry points and classes instantiated by framework reflection (<init> only so R8 shrinks internals)
-keep class com.meridian11.books.awake.AwakeApp { <init>(); }
-keep class com.meridian11.books.awake.ui.ToggleTrampolineActivity { <init>(); }
-keep class com.meridian11.books.awake.ui.ShortcutActionActivity { <init>(); }
-keep class com.meridian11.books.awake.ui.SettingsDialogActivity { <init>(); }
-keep class com.meridian11.books.awake.service.KeepAwakeService { <init>(); }
-keep class com.meridian11.books.awake.service.KeepAwakeTileService { <init>(); }
-keep class com.meridian11.books.awake.widget.AwakeSquircleWidgetProvider { <init>(); }
-keep class com.meridian11.books.awake.widget.AwakeWidgetActionReceiver { <init>(); }
-keep class com.meridian11.books.awake.receiver.SessionExpirationReceiver { <init>(); }
-keep class com.meridian11.books.awake.receiver.SystemEventReceiver { <init>(); }

# Keep Jetpack DataStore Preferences protobuf fields
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
-dontwarn kotlinx.coroutines.**

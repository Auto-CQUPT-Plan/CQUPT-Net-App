# Preserve generic signatures and annotations used by Android libraries.
-keepattributes Signature
-keepattributes *Annotation*

# Dependencies in this project ship their own consumer ProGuard rules. Avoid broad
# keep rules here so R8 can remove unused app, Compose, OkHttp and coroutine code.

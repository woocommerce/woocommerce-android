-dontobfuscate

###### OkHttp (the library ships its own consumer rules) - begin
-dontwarn okio.**
-dontwarn okhttp3.**
-dontwarn com.squareup.okhttp.**

-keepattributes Signature
-keepattributes *Annotation*
###### OkHttp - end

###### Event Bus 3 (the @Subscribe rule comes from the library's consumer rules)
-keepattributes *Annotation*
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}
###### Event Bus 3 - end

###### Event Bus 2 - begin
-keepclassmembers class ** {
    public void onEvent*(**);
}

# Only required if you use AsyncExecutor
-keepclassmembers class * extends de.greenrobot.event.util.ThrowableFailureEvent {
    ** *(java.lang.Throwable);
}
###### Event Bus 2 - end

##### WooCommerce - begin
# Gson populates fields reflectively, so keep fields and enum constants of our classes
# (we don't obfuscate, so names are stable). Methods stay eligible for R8 optimization.
-keepclassmembers class com.woocommerce.** { <fields>; }
-keepclassmembers enum com.woocommerce.** { *; }
##### WooCommerce - end

###### FluxC (Gson deserialization; model/network field keeps come from fluxc's consumer-rules.pro) - begin
-keepclassmembers class org.wordpress.android.fluxc.** { <fields>; }
-keepclassmembers enum org.wordpress.android.fluxc.** { *; }
###### FluxC - end

###### FluxC - WellSql (needed for Addon support) - begin
-keep class com.wellsql** { *; }
###### FluxC - end

###### Dagger - begin
-dontwarn com.google.errorprone.annotations.*
###### Dagger - end

-keep class com.google.common.** { *; }
-dontwarn com.google.common.**

###### Zendesk - begin
-keep class com.zendesk.** { *; }
-keep class zendesk.** { *; }
-keep class javax.inject.Provider
-keep class com.squareup.picasso.** { *; }
-keep class com.jakewharton.disklrucache.** { *; }
-keep class com.google.gson.** { *; }
-keep class okio.** { *; }
-keep class retrofit2.** { *; }
-keep class uk.co.senab.photoview.** { *; }
###### Zendesk - end

###### Glide - begin
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
###### Glide - end

###### SavedStateHandleExt - begin
###### We use reflection so we have to keep this method
-keepclassmembers class * extends androidx.navigation.NavArgs {
    fromSavedStateHandle(androidx.lifecycle.SavedStateHandle);
}
###### SavedStateHandleExt - end

###### Google Crypto Tink dependencies - begin
-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn com.google.api.client.http.javanet.NetHttpTransport
-dontwarn org.joda.time.Instant
###### Google Crypto Tink dependencies - end

# This is generated automatically by the Android Gradle plugin.
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder

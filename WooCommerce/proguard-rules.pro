-dontobfuscate

##### WooCommerce - begin
# Gson populates fields reflectively, so keep fields and enum constants of our classes
# (we don't obfuscate, so names are stable). Methods stay eligible for R8 optimization.
-keepclassmembers class com.woocommerce.** { <fields>; }
-keepclassmembers enum com.woocommerce.** { *; }
##### WooCommerce - end

###### FluxC - WellSql (needed for Addon support) - begin
-keep class com.wellsql** { *; }
###### FluxC - end

-dontwarn com.google.common.**

###### Zendesk (the SDK ships its own consumer rules; Gson/Retrofit/OkHttp ship theirs too)

###### Glide - begin
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
###### Glide - end

###### SavedStateHandleExt - begin
###### We use reflection so we have to keep this method
-keepclassmembers class * extends androidx.navigation.NavArgs {
    fromSavedStateHandle(androidx.lifecycle.SavedStateHandle);
}
###### SavedStateHandleExt - end

###### Google Crypto Tink (still transitively present; KeysDownloader references
###### google-http-client + joda-time which aren't on the classpath) - begin
-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn com.google.api.client.http.javanet.NetHttpTransport
-dontwarn org.joda.time.Instant
###### Google Crypto Tink - end

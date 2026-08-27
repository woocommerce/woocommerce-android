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

###### SavedStateHandleExt - begin
###### We use reflection so we have to keep this method
-keepclassmembers class * extends androidx.navigation.NavArgs {
    fromSavedStateHandle(androidx.lifecycle.SavedStateHandle);
}
###### SavedStateHandleExt - end

# Crypto Tink is still transitively present; its KeysDownloader references google-http-client
# and joda-time, which aren't on the classpath. R8 fails to build without these.
-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn com.google.api.client.http.javanet.NetHttpTransport
-dontwarn org.joda.time.Instant

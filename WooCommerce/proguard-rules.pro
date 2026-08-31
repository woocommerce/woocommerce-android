-dontobfuscate

##### WooCommerce - begin
# Gson instantiates DTOs reflectively and populates their fields, so R8 full mode must not
# remove, abstract, or merge any of our classes it can't trace (TypeToken / ::class.java
# targets and their transitively-reached field types), or deserialization breaks in release
# builds only (LinkedTreeMap ClassCastException, "Abstract classes can't be instantiated").
# Keep all our classes with their fields - we don't obfuscate, so names are stable, and
# methods stay eligible for R8 optimization.
#
# We considered scoping this to the packages Gson DTOs conventionally live in:
#   -keep class com.woocommerce.android.network.** { <fields>; }
#   -keep class com.woocommerce.android.**.networking.** { <fields>; }
# but several Gson round-trip models live elsewhere (SharedPreferences/DataStore-persisted
# models, the cardreader remote wire protocol) and only survived because their write paths
# happen to construct them directly, and nothing would catch a covered DTO with a field
# typed to a class outside these packages. The broad rule costs ~0.8 MB APK over the
# per-package keeps, but can't be silently broken by a refactor and spares developers from
# thinking about proguard when adding a Gson-deserialized class.
-keep class com.woocommerce.** { <fields>; }
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

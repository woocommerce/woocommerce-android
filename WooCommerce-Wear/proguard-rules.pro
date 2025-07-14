-dontobfuscate

###### OkHttp - begin
-dontwarn okio.**
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn com.squareup.okhttp.**
-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }

-keepattributes Signature
-keepattributes *Annotation*
###### OkHttp - end

###### Event Bus 3
-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
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

##### WooCommerce (this is needed for Json deserializers, but generally, we should keep our own classes) - begin
-keep class com.woocommerce.** { *; }
##### WooCommerce - end

###### FluxC (was needed for Json deserializers) - begin
-keep class org.wordpress.android.fluxc** { *; }
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

###### Encrypted Logs - begin
-dontwarn java.awt.*
-keep class com.sun.jna.* { *; }
-keepclassmembers class * extends com.sun.jna.* { public *; }
###### Encrypted Logs - end

###### Glide - begin
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
###### Glide - end

###### SavedStateHandleExt - begin
###### We use reflection so we have to keep this method
-keepclassmembers class * extends androidx.navigation.NavArgs {
    fromSavedStateHandle(androidx.lifecycle.SavedStateHandle);
}
###### SavedStateHandleExt - end

# This is generated automatically by the Android Gradle plugin. (8.1.0)
-dontwarn com.google.auto.service.AutoService
-dontwarn com.squareup.kotlinpoet.FileSpec
-dontwarn com.squareup.kotlinpoet.OriginatingElementsHolder
-dontwarn com.squareup.kotlinpoet.OriginatingElementsHolder$Builder
-dontwarn com.squareup.kotlinpoet.javapoet.J2kInteropKt
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient
-dontwarn javax.lang.model.SourceVersion
-dontwarn javax.lang.model.element.AnnotationMirror
-dontwarn javax.lang.model.element.AnnotationValue
-dontwarn javax.lang.model.element.AnnotationValueVisitor
-dontwarn javax.lang.model.element.Element
-dontwarn javax.lang.model.element.ElementKind
-dontwarn javax.lang.model.element.ElementVisitor
-dontwarn javax.lang.model.element.ExecutableElement
-dontwarn javax.lang.model.element.Modifier
-dontwarn javax.lang.model.element.Name
-dontwarn javax.lang.model.element.NestingKind
-dontwarn javax.lang.model.element.PackageElement
-dontwarn javax.lang.model.element.QualifiedNameable
-dontwarn javax.lang.model.element.TypeElement
-dontwarn javax.lang.model.element.TypeParameterElement
-dontwarn javax.lang.model.element.VariableElement
-dontwarn javax.lang.model.type.ArrayType
-dontwarn javax.lang.model.type.DeclaredType
-dontwarn javax.lang.model.type.ErrorType
-dontwarn javax.lang.model.type.ExecutableType
-dontwarn javax.lang.model.type.IntersectionType
-dontwarn javax.lang.model.type.NoType
-dontwarn javax.lang.model.type.NullType
-dontwarn javax.lang.model.type.PrimitiveType
-dontwarn javax.lang.model.type.TypeKind
-dontwarn javax.lang.model.type.TypeMirror
-dontwarn javax.lang.model.type.TypeVariable
-dontwarn javax.lang.model.type.TypeVisitor
-dontwarn javax.lang.model.type.UnionType
-dontwarn javax.lang.model.type.WildcardType
-dontwarn javax.lang.model.util.AbstractAnnotationValueVisitor8
-dontwarn javax.lang.model.util.AbstractElementVisitor8
-dontwarn javax.lang.model.util.AbstractTypeVisitor8
-dontwarn javax.lang.model.util.ElementFilter
-dontwarn javax.lang.model.util.Elements
-dontwarn javax.lang.model.util.SimpleAnnotationValueVisitor6
-dontwarn javax.lang.model.util.SimpleAnnotationValueVisitor7
-dontwarn javax.lang.model.util.SimpleAnnotationValueVisitor8
-dontwarn javax.lang.model.util.SimpleElementVisitor8
-dontwarn javax.lang.model.util.SimpleTypeVisitor7
-dontwarn javax.lang.model.util.SimpleTypeVisitor8
-dontwarn javax.lang.model.util.Types
-dontwarn javax.tools.Diagnostic$Kind
-dontwarn javax.tools.FileObject
-dontwarn javax.tools.JavaFileManager$Location
-dontwarn javax.tools.JavaFileObject
-dontwarn javax.tools.JavaFileObject$Kind
-dontwarn javax.tools.SimpleJavaFileObject
-dontwarn javax.tools.StandardLocation
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder

package com.woocommerce.android.di

import android.content.Context
import com.android.volley.RequestQueue
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.volley.VolleyUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.woocommerce.android.WooCommerce
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Named

/**
 * Custom [AppGlideModule] that replaces Glide's default [RequestQueue] with FluxC's.
 */
@GlideModule
class WooCommerceGlideModule : AppGlideModule() {
    @field:[Inject Named("custom-ssl")] internal lateinit var requestQueue: RequestQueue

    override fun applyOptions(context: Context?, builder: GlideBuilder?) {}

    override fun isManifestParsingEnabled(): Boolean = false

    override fun registerComponents(context: Context?, glide: Glide?, registry: Registry?) {
        (context as WooCommerce).membersInjector.injectMembers(this)
        glide?.registry?.replace(GlideUrl::class.java, InputStream::class.java, VolleyUrlLoader.Factory(requestQueue))
    }
}

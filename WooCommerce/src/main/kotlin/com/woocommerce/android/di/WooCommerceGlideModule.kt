package com.woocommerce.android.di

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import com.android.volley.RequestQueue
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.volley.VolleyUrlLoader
import com.bumptech.glide.load.engine.cache.DiskCache
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.woocommerce.android.WooCommerce
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Named

/**
 * Custom [AppGlideModule] that replaces Glide's default [RequestQueue] with FluxC's.
 */
@GlideModule
class WooCommerceGlideModule : AppGlideModule() {
    @field:[Inject Named("custom-ssl")] internal lateinit var requestQueue: RequestQueue

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        initGlideCache(context, builder)
    }

    override fun isManifestParsingEnabled(): Boolean = false

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        (context as WooCommerce).membersInjector.injectMembers(this)
        glide.registry.replace(GlideUrl::class.java, InputStream::class.java, VolleyUrlLoader.Factory(requestQueue))
    }

    /**
     * Reduces the size of the disk cache if Android tells us our cache quota is smaller than Glide's
     * default cache size. Note that this only affects devices running API 26 or later since earlier
     *  APIs don't support getCacheQuotaBytes().
     */
    private fun initGlideCache(context: Context, builder: GlideBuilder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appContext = context.applicationContext
            val storageManager = appContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            try {
                val uuid = storageManager.getUuidForPath(appContext.getCacheDir())
                val quota = storageManager.getCacheQuotaBytes(uuid)
                if (quota > 0 && quota < DiskCache.Factory.DEFAULT_DISK_CACHE_SIZE) {
                    val cacheFactory = InternalCacheDiskCacheFactory(appContext, quota)
                    builder.setDiskCache(cacheFactory)
                    val diff = DiskCache.Factory.DEFAULT_DISK_CACHE_SIZE - quota
                    WooLog.d(T.UTILS, "Reduced size of image disk cache by $diff bytes")
                }
            } catch (e: IOException) {
                WooLog.e(T.UTILS, "Unable to change image cache size", e)
            }
        }
    }
}

package com.woocommerce.android.wear.di

import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.network.rest.wpcom.wc.UnknownBlogListener

/**
 * `WooNetwork` injects an `Optional<UnknownBlogListener>`. The Wear app doesn't recover from
 * `unknown_blog` errors, so it provides only the optional binding (left empty) to satisfy the graph.
 */
@Module
@InstallIn(SingletonComponent::class)
interface UnknownBlogModule {
    @BindsOptionalOf
    fun bindOptionalUnknownBlogListener(): UnknownBlogListener
}

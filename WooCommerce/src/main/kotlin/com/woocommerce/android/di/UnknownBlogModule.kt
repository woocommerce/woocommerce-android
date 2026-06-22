package com.woocommerce.android.di

import com.woocommerce.android.network.UnknownBlogNotifier
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.network.rest.wpcom.wc.UnknownBlogListener

@Module
@InstallIn(SingletonComponent::class)
interface UnknownBlogModule {
    @BindsOptionalOf
    fun bindOptionalUnknownBlogListener(): UnknownBlogListener

    @Binds
    fun bindUnknownBlogListener(notifier: UnknownBlogNotifier): UnknownBlogListener
}

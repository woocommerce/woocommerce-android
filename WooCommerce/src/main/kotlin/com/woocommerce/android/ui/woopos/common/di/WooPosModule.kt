package com.woocommerce.android.ui.woopos.common.di

import com.woocommerce.android.ui.products.selector.ProductListHandler
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentCommunication
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventReceiver
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenCommunication
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventSender
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
class WooPosModule {
    @Provides
    fun provideProductList(
        handler: ProductListHandler
    ): WooPosProductsDataSource {
        return WooPosProductsDataSourceImpl(handler)
    }
}

@InstallIn(ActivityRetainedComponent::class)
@Module
abstract class WooPosActivityModule {
    @Binds
    abstract fun bindParentToChildrenEventReceiver(
        parentToChildrenCommunication: WooPosParentToChildrenCommunication
    ): WooPosParentToChildrenEventReceiver

    @Binds
    abstract fun bindParentToChildrenEventSender(
        parentToChildrenCommunication: WooPosParentToChildrenCommunication
    ): WooPosParentToChildrenEventSender

    @Binds
    abstract fun bindChildrenToParentEventReceiver(
        childToParentCommunication: WooPosChildrenToParentCommunication
    ): WooPosChildrenToParentEventReceiver

    @Binds
    abstract fun bindChildrenToParentEventSender(
        childToParentCommunication: WooPosChildrenToParentCommunication
    ): WooPosChildrenToParentEventSender
}

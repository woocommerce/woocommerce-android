package com.woocommerce.android.di

import com.woocommerce.android.ui.aztec.AztecModule
import com.woocommerce.android.ui.main.MainModule
import com.woocommerce.android.ui.mystore.MyStoreModule
import com.woocommerce.android.ui.orders.OrdersModule
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelsModule
import com.woocommerce.android.ui.products.ProductsModule
import com.woocommerce.android.ui.refunds.RefundsModule
import com.woocommerce.android.ui.reviews.ReviewsModule
import com.woocommerce.android.ui.sitepicker.SitePickerModule
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

/**
 * A temporary class to allow injecting fragments relying on dagger.
 * We should remove it after finishing the migration
 */
@InstallIn(ActivityComponent::class)
@Module(
    includes = [
        MainModule::class,
        MyStoreModule::class,
        OrdersModule::class,
        RefundsModule::class,
        ProductsModule::class,
        ReviewsModule::class,
        SitePickerModule::class,
        AztecModule::class,
        ShippingLabelsModule::class
    ]
)
interface MainActivityAggregatorModule

package com.woocommerce.android.ui.products

import com.woocommerce.android.di.FragmentScope
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class ProductVariantsFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = [ProductVariantsModule::class])
    abstract fun productVariantsFragment(): ProductVariantsFragment
}

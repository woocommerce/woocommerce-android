package com.woocommerce.android.ui.orders.shippinglabels

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelsModule.CreateShippingLabelFragmentModule
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelsModule.EditShippingLabelAddressFragmentModule
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelsModule.EditShippingLabelPackagesFragmentModule
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelsModule.EditShippingLabelPaymentFragmentModule
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelsModule.PrintShippingLabelFragmentModule
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelsModule.ShippingCarriersFragmentModule
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelsModule.ShippingLabelAddressSuggestionFragmentModule
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelsModule.ShippingLabelRefundFragmentModule
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelsModule.ShippingPackageSelectorFragmentModule
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelFragment
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelModule
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelAddressFragment
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelAddressModule
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPackagesFragment
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPackagesModule
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPaymentFragment
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPaymentModule
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarriersFragment
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarriersModule
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressSuggestionFragment
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressSuggestionModule
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingPackageSelectorFragment
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingPackageSelectorModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [
    ShippingLabelRefundFragmentModule::class,
    PrintShippingLabelFragmentModule::class,
    CreateShippingLabelFragmentModule::class,
    EditShippingLabelAddressFragmentModule::class,
    ShippingLabelAddressSuggestionFragmentModule::class,
    EditShippingLabelPackagesFragmentModule::class,
    ShippingPackageSelectorFragmentModule::class,
    ShippingCarriersFragmentModule::class,
    EditShippingLabelPaymentFragmentModule::class
])
object ShippingLabelsModule {
    @Module
    abstract class ShippingLabelRefundFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ShippingLabelRefundModule::class])
        abstract fun shippingLabelRefundFragment(): ShippingLabelRefundFragment
    }
    @Module
    abstract class PrintShippingLabelFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [PrintShippingLabelModule::class])
        abstract fun printShippingLabelFragment(): PrintShippingLabelFragment
    }
    @Module
    abstract class CreateShippingLabelFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [CreateShippingLabelModule::class])
        abstract fun createShippingLabelFragment(): CreateShippingLabelFragment
    }
    @Module
    abstract class EditShippingLabelAddressFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [EditShippingLabelAddressModule::class])
        abstract fun editShippingLabelAddressFragment(): EditShippingLabelAddressFragment
    }
    @Module
    abstract class ShippingLabelAddressSuggestionFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ShippingLabelAddressSuggestionModule::class])
        abstract fun shippingLabelAddressSuggestionFragment(): ShippingLabelAddressSuggestionFragment
    }
    @Module
    abstract class EditShippingLabelPackagesFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [EditShippingLabelPackagesModule::class])
        abstract fun editShippingLabelPackagesFragment(): EditShippingLabelPackagesFragment
    }
    @Module
    abstract class ShippingPackageSelectorFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ShippingPackageSelectorModule::class])
        abstract fun shippingPackageSelectorFragment(): ShippingPackageSelectorFragment
    }
    @Module
    abstract class EditShippingLabelPaymentFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [EditShippingLabelPaymentModule::class])
        abstract fun shippingPackageSelectorFragment(): EditShippingLabelPaymentFragment
    }
    @Module
    abstract class ShippingCarriersFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [ShippingCarriersModule::class])
        abstract fun shippingCarriersFragment(): ShippingCarriersFragment
    }
}

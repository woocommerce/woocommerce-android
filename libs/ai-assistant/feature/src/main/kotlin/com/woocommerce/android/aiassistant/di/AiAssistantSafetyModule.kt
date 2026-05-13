package com.woocommerce.android.aiassistant.di

import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewProvider
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewProviderRegistry
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewProviderRegistryImpl
import com.woocommerce.android.aiassistant.safety.GenericSchemaConfirmationPreviewProvider
import com.woocommerce.android.aiassistant.safety.OrdersConfirmationPreviewProvider
import com.woocommerce.android.aiassistant.safety.ProductVariationsConfirmationPreviewProvider
import com.woocommerce.android.aiassistant.safety.ProductsConfirmationPreviewProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AiAssistantSafetyModule {
    @Binds
    @Singleton
    internal abstract fun bindConfirmationPreviewProviderRegistry(
        impl: ConfirmationPreviewProviderRegistryImpl,
    ): ConfirmationPreviewProviderRegistry

    @Binds
    @IntoSet
    internal abstract fun bindGenericSchemaConfirmationPreviewProvider(
        impl: GenericSchemaConfirmationPreviewProvider,
    ): ConfirmationPreviewProvider

    @Binds
    @IntoSet
    internal abstract fun bindOrdersConfirmationPreviewProvider(
        impl: OrdersConfirmationPreviewProvider,
    ): ConfirmationPreviewProvider

    @Binds
    @IntoSet
    internal abstract fun bindProductsConfirmationPreviewProvider(
        impl: ProductsConfirmationPreviewProvider,
    ): ConfirmationPreviewProvider

    @Binds
    @IntoSet
    internal abstract fun bindProductVariationsConfirmationPreviewProvider(
        impl: ProductVariationsConfirmationPreviewProvider,
    ): ConfirmationPreviewProvider
}

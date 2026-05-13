package com.woocommerce.android.aiassistant.safety

import javax.inject.Inject

internal interface ConfirmationPreviewProviderRegistry {
    fun providerFor(context: ConfirmationPreviewContext): ConfirmationPreviewProvider
    suspend fun buildPreview(context: ConfirmationPreviewContext): ConfirmationPreview
}

internal class ConfirmationPreviewProviderRegistryImpl @Inject constructor(
    providers: Set<@JvmSuppressWildcards ConfirmationPreviewProvider>,
) : ConfirmationPreviewProviderRegistry {
    private val sortedProviders = providers.sortedWith(
        compareByDescending<ConfirmationPreviewProvider> { it.priority }
            .thenBy { it.key }
    )

    override fun providerFor(context: ConfirmationPreviewContext): ConfirmationPreviewProvider =
        sortedProviders.first { it.canPreview(context) }

    override suspend fun buildPreview(context: ConfirmationPreviewContext): ConfirmationPreview =
        providerFor(context).buildPreview(context)
}

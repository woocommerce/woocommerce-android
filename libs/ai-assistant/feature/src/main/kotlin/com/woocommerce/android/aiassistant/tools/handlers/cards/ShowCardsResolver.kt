package com.woocommerce.android.aiassistant.tools.handlers.cards

import kotlinx.serialization.json.JsonObject

internal interface ShowCardsResolver {
    suspend fun resolve(refs: List<ValidatedRef>): List<ShowCardsResolution>
}

internal sealed interface ShowCardsResolution {
    val ref: ValidatedRef

    data class Resolved(
        override val ref: ValidatedRef,
        val summary: JsonObject,
        val card: ShowCardPayload,
    ) : ShowCardsResolution

    data class Missing(
        override val ref: ValidatedRef,
        val reason: ShowCardsRejectionReason,
    ) : ShowCardsResolution
}

internal class DefaultShowCardsResolver : ShowCardsResolver {
    override suspend fun resolve(refs: List<ValidatedRef>): List<ShowCardsResolution> =
        refs.map { ref ->
            ShowCardsResolution.Missing(
                ref = ref,
                reason = ShowCardsRejectionReason.NotFound,
            )
        }
}

package com.woocommerce.android.aiassistant.tools.handlers.cards

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal class ShowCardsReferenceValidator {
    fun validate(references: List<JsonElement>): ShowCardsValidationResult {
        val validRefs = mutableListOf<ValidatedRef>()
        val rejectedRefs = mutableListOf<RejectedRef>()

        references.forEachIndexed { index, reference ->
            val ref = reference as? JsonObject
            if (ref == null) {
                rejectedRefs += RejectedRef(index = index, reason = ShowCardsRejectionReason.MalformedRef)
                return@forEachIndexed
            }

            val rawFamily = ref.stringOrNull("family")
            if (rawFamily == null) {
                rejectedRefs += RejectedRef(index = index, id = ref.stringOrNull("id"), reason = ShowCardsRejectionReason.MissingFamily)
                return@forEachIndexed
            }

            val family = ShowCardFamily.from(rawFamily)
            if (family == null) {
                rejectedRefs += RejectedRef(
                    index = index,
                    family = rawFamily,
                    id = ref.stringOrNull("id"),
                    reason = ShowCardsRejectionReason.UnsupportedFamily,
                )
                return@forEachIndexed
            }

            val id = ref.stringOrNull("id")
            if (id == null) {
                rejectedRefs += RejectedRef(
                    index = index,
                    family = family.serializedName,
                    reason = ShowCardsRejectionReason.MissingId,
                )
                return@forEachIndexed
            }

            if (!id.isValidShowCardsId()) {
                rejectedRefs += RejectedRef(
                    index = index,
                    family = family.serializedName,
                    id = id,
                    reason = ShowCardsRejectionReason.InvalidId,
                )
                return@forEachIndexed
            }

            validRefs += ValidatedRef(index = index, family = family, id = id)
        }

        return ShowCardsValidationResult(
            requested = references.size,
            validRefs = validRefs,
            rejectedRefs = rejectedRefs,
        )
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        (get(key) as? JsonPrimitive)?.contentOrNull

    private fun String.isValidShowCardsId(): Boolean = isNotBlank()
}

internal data class ShowCardsValidationResult(
    val requested: Int,
    val validRefs: List<ValidatedRef>,
    val rejectedRefs: List<RejectedRef>,
)

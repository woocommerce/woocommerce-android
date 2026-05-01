package com.woocommerce.android.aiassistant.tools.handlers.cards

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal class ShowCardsReferenceValidator {
    fun validate(references: List<JsonElement>): ShowCardsValidationResult {
        val state = ValidationState()

        references.forEachIndexed { index, reference ->
            validateReference(index, reference, state)
        }

        return ShowCardsValidationResult(
            requested = references.size,
            validRefs = state.validRefs,
            rejectedRefs = state.rejectedRefs,
        )
    }

    @Suppress("ReturnCount")
    private fun validateReference(index: Int, reference: JsonElement, state: ValidationState) {
        val ref = reference as? JsonObject
            ?: return state.reject(index = index, reason = ShowCardsRejectionReason.MalformedRef)

        val rawFamily = ref.stringOrNull("family")
            ?: return state.reject(
                index = index,
                id = ref.stringOrNull("id"),
                reason = ShowCardsRejectionReason.MissingFamily,
            )

        val family = ShowCardFamily.from(rawFamily)
            ?: return state.reject(
                index = index,
                family = rawFamily,
                id = ref.stringOrNull("id"),
                reason = ShowCardsRejectionReason.UnsupportedFamily,
            )

        val idElement = ref["id"]
            ?: return state.reject(
                index = index,
                family = family.serializedName,
                reason = ShowCardsRejectionReason.MissingId,
            )
        val id = (idElement as? JsonPrimitive)
            ?.stringContentOrNull()
            ?: return state.rejectInvalidId(index, family)

        when {
            !id.isValidShowCardsId() -> state.rejectInvalidId(index, family, id)
            !state.seen.add(family to id) -> state.rejectDuplicate(index, family, id)
            state.validRefs.size >= MAX_SHOW_CARDS_REFS -> state.rejectOverLimit(index, family, id)
            else -> state.validRefs += ValidatedRef(index = index, family = family, id = id)
        }
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        (get(key) as? JsonPrimitive)?.contentOrNull

    private fun JsonPrimitive.stringContentOrNull(): String? =
        contentOrNull?.takeIf { toString().startsWith("\"") }

    private fun String.isValidShowCardsId(): Boolean = isNotBlank()

    private fun ValidationState.rejectInvalidId(index: Int, family: ShowCardFamily) =
        reject(index, family.serializedName, reason = ShowCardsRejectionReason.InvalidId)

    private fun ValidationState.rejectInvalidId(index: Int, family: ShowCardFamily, id: String) =
        reject(index, family.serializedName, id, ShowCardsRejectionReason.InvalidId)

    private fun ValidationState.rejectDuplicate(index: Int, family: ShowCardFamily, id: String) =
        reject(index, family.serializedName, id, ShowCardsRejectionReason.DuplicateRef)

    private fun ValidationState.rejectOverLimit(index: Int, family: ShowCardFamily, id: String) =
        reject(index, family.serializedName, id, ShowCardsRejectionReason.OverLimit)

    private class ValidationState {
        val validRefs = mutableListOf<ValidatedRef>()
        val rejectedRefs = mutableListOf<RejectedRef>()
        val seen = linkedSetOf<Pair<ShowCardFamily, String>>()

        fun reject(
            index: Int,
            family: String? = null,
            id: String? = null,
            reason: ShowCardsRejectionReason,
        ) {
            rejectedRefs += RejectedRef(
                index = index,
                family = family,
                id = id,
                reason = reason,
            )
        }
    }
}

internal data class ShowCardsValidationResult(
    val requested: Int,
    val validRefs: List<ValidatedRef>,
    val rejectedRefs: List<RejectedRef>,
)

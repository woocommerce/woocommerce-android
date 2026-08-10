package com.woocommerce.android.ui.products.images

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ProductImageDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs by savedStateHandle.navArgs<ProductImageDetailsFragmentArgs>()
    private val storedImage = navArgs.image

    private val imageDraft = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = ImageDraft(
            altText = storedImage.alt.orEmpty(),
            name = storedImage.name.orEmpty()
        ),
        key = "imageDraft"
    )

    val state = imageDraft.map { buildUiState(it) }.toStateFlow(buildUiState(imageDraft.value))

    private fun buildUiState(draft: ImageDraft) = UiState(
        imageUrl = storedImage.source,
        altText = draft.altText,
        name = draft.name,
        altTextPlaceholder = storedImage.alt,
        namePlaceholder = storedImage.name,
        isAltTextRemovalBlocked = draft.altText.isEmpty() && !storedImage.alt.isNullOrEmpty(),
        isNameRemovalBlocked = draft.name.isEmpty() && !storedImage.name.isNullOrEmpty()
    )

    // The update request can't clear a value on the server, so an emptied field is not a change;
    // the stored value stays and is shown as the field's placeholder
    private fun String.isChangedFrom(storedValue: String?) = isNotEmpty() && this != storedValue.orEmpty()

    fun onAltTextChanged(altText: String) {
        imageDraft.update { it.copy(altText = altText) }
    }

    fun onNameChanged(name: String) {
        imageDraft.update { it.copy(name = name) }
    }

    fun onExit() {
        val draft = imageDraft.value
        val hasChanges = draft.altText.isChangedFrom(storedImage.alt) || draft.name.isChangedFrom(storedImage.name)
        if (!hasChanges) {
            triggerEvent(MultiLiveEvent.Event.Exit)
            return
        }

        val updatedImage = storedImage.copy(
            alt = if (draft.altText.isChangedFrom(storedImage.alt)) draft.altText else storedImage.alt,
            name = if (draft.name.isChangedFrom(storedImage.name)) draft.name else storedImage.name
        )
        triggerEvent(MultiLiveEvent.Event.ExitWithResult(data = updatedImage, key = KEY_IMAGE_DETAILS_RESULT))
    }

    data class UiState(
        val imageUrl: String,
        val altText: String,
        val name: String,
        val altTextPlaceholder: String? = null,
        val namePlaceholder: String? = null,
        val isAltTextRemovalBlocked: Boolean = false,
        val isNameRemovalBlocked: Boolean = false
    )

    @Parcelize
    private data class ImageDraft(
        val altText: String,
        val name: String
    ) : Parcelable

    companion object {
        const val KEY_IMAGE_DETAILS_RESULT = "key_image_details_result"
    }
}

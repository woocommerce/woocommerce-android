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
    savedStateHandle: SavedStateHandle,
    productImagesRepository: ProductImagesRepository
) : ScopedViewModel(savedStateHandle) {
    private val navArgs by savedStateHandle.navArgs<ProductImageDetailsFragmentArgs>()
    private val draftImage = navArgs.image

    // The image as the site knows it. The draft can carry unsaved edits, so blocking removal and
    // showing the kept value are based on the stored image; clearing an unsaved edit stays possible.
    private val storedImage = productImagesRepository.getProduct(navArgs.remoteProductId)
        ?.images
        ?.firstOrNull { it.id == draftImage.id }

    private val imageDraft = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = ImageDraft(
            altText = draftImage.alt.orEmpty(),
            name = draftImage.name.orEmpty()
        ),
        key = "imageDraft"
    )

    val state = imageDraft.map { buildUiState(it) }.toStateFlow(buildUiState(imageDraft.value))

    private fun buildUiState(draft: ImageDraft) = UiState(
        imageUrl = draftImage.source,
        altText = draft.altText,
        name = draft.name,
        altTextPlaceholder = storedImage?.alt,
        namePlaceholder = storedImage?.name,
        isAltTextRemovalBlocked = draft.altText.isEmpty() && !storedImage?.alt.isNullOrEmpty(),
        isNameRemovalBlocked = draft.name.isEmpty() && !storedImage?.name.isNullOrEmpty()
    )

    fun onAltTextChanged(altText: String) {
        imageDraft.update { it.copy(altText = altText) }
    }

    fun onNameChanged(name: String) {
        imageDraft.update { it.copy(name = name) }
    }

    fun onExit() {
        val draft = imageDraft.value
        // A cleared field falls back to the stored value because the update request can't remove
        // it from the server
        val updatedImage = draftImage.copy(
            alt = draft.altText.ifEmpty { storedImage?.alt },
            name = draft.name.ifEmpty { storedImage?.name }
        )
        if (updatedImage == draftImage) {
            triggerEvent(MultiLiveEvent.Event.Exit)
        } else {
            triggerEvent(MultiLiveEvent.Event.ExitWithResult(data = updatedImage, key = KEY_IMAGE_DETAILS_RESULT))
        }
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

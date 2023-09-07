package com.woocommerce.android.ui.products.variations.attributes

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AddAttributeTermsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val termsListHandler: AttributeTermsListHandler
): ScopedViewModel(savedState) {
    init {

    }

    private fun fetchAttributeTerms() = launch {

    }
}

package com.woocommerce.android.ui.blaze.creation.targets

import android.os.Parcelable
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SearchState.Hidden
import kotlinx.parcelize.Parcelize

data class TargetSelectionViewState(
    val items: List<SelectionItem>,
    val selectedItems: List<SelectionItem>,
    val title: String,
    val searchQuery: String = "",
    val searchState: SearchState = Hidden
) {

    val isSaveButtonEnabled: Boolean
        get() {
            return if (searchState !is Hidden) {
                true
            } else {
                selectedItems.isNotEmpty()
            }
        }

    val isAllButtonToggled: Boolean
        get() {
            return if (searchState !is Hidden) {
                selectedItems.isEmpty()
            } else {
                selectedItems.size == items.size
            }
        }

    data class SelectionItem(
        val id: String,
        val title: String
    )

    sealed class SearchState(
        val isVisible: Boolean = true
    ) : Parcelable {
        @Parcelize
        object Hidden : SearchState(isVisible = false)

        @Parcelize
        object Inactive : SearchState()

        @Parcelize
        object Ready : SearchState()

        @Parcelize
        object Searching : SearchState()

        @Parcelize
        object NoResults : SearchState()

        @Parcelize
        object Error : SearchState()

        @Parcelize
        data class Results(val resultItems: List<SearchItem>) : SearchState() {
            @Parcelize
            data class SearchItem(
                val id: String,
                val title: String,
                val subtitle: String? = null,
                val type: String? = null
            ) : Parcelable
        }
    }
}

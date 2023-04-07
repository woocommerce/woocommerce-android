package com.woocommerce.android.ui.payments.cardreader.hub

import androidx.annotation.DrawableRes
import com.woocommerce.android.model.UiString

data class CardReaderHubViewState(
    val rows: List<ListItem>,
    val isLoading: Boolean,
    val onboardingErrorAction: OnboardingErrorAction?,
) {
    sealed class ListItem {
        abstract val label: UiString?
        abstract val icon: Int?
        abstract val onClick: (() -> Unit)?
        abstract val index: Int
        abstract var isEnabled: Boolean

        data class NonToggleableListItem(
            @DrawableRes override val icon: Int,
            override val label: UiString,
            val description: UiString? = null,
            override var isEnabled: Boolean = true,
            override val index: Int,
            override val onClick: () -> Unit,
            val shortDivider: Boolean = false,
        ) : ListItem()

        data class ToggleableListItem(
            @DrawableRes override val icon: Int,
            override val label: UiString,
            val description: UiString,
            override var isEnabled: Boolean = true,
            val isChecked: Boolean,
            override val index: Int,
            override val onClick: (() -> Unit)? = null,
            val onToggled: (Boolean) -> Unit,
            val onLearnMoreClicked: () -> Unit
        ) : ListItem()

        data class HeaderItem(
            @DrawableRes override val icon: Int? = null,
            override val label: UiString,
            override val index: Int,
            override var isEnabled: Boolean = false,
            override val onClick: (() -> Unit)? = null
        ) : ListItem()

        data class GapBetweenSections(
            @DrawableRes override val icon: Int? = null,
            override val label: UiString? = null,
            override val index: Int,
            override var isEnabled: Boolean = false,
            override val onClick: (() -> Unit)? = null
        ) : ListItem()
    }

    data class OnboardingErrorAction(
        val text: UiString?,
        val onClick: () -> Unit,
    )

    data class LearnMoreIppState(
        val label: UiString,
        val onClick: () -> Unit,
    )
}

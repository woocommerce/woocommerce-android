package com.woocommerce.android.ui.analytics.hub.settings

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class AnalyticsHubSettingsViewModel @Inject constructor(savedState: SavedStateHandle) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, AnalyticsHubSettingsViewState())
    private var viewState by viewStateData
    fun onBackPressed() {
        viewState = viewState.copy(showDismissDialog = true)
    }

    fun onDismissDiscardChanges() {
        viewState = viewState.copy(showDismissDialog = false)
    }

    fun onDiscardChanges() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onSaveChanges() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onSelectionChange(id: Long, isSelected: Boolean) {
        val updatedList = viewState.cards.map { card ->
            if (card.id == id) card.copy(isVisible = isSelected)
            else card
        }
        viewState = viewState.copy(cards = updatedList)
    }
}

@Suppress("MagicNumber")
@Parcelize
data class AnalyticsHubSettingsViewState(
    val cards: List<AnalyticCardConfiguration> = listOf(
        AnalyticCardConfiguration(1L, "Revenue", true),
        AnalyticCardConfiguration(2L, "Orders", true),
        AnalyticCardConfiguration(3L, "Stats", false)
    ),
    val showDismissDialog: Boolean = false
) : Parcelable

@Parcelize
data class AnalyticCardConfiguration(
    val id: Long,
    val title: String,
    val isVisible: Boolean
) : Parcelable

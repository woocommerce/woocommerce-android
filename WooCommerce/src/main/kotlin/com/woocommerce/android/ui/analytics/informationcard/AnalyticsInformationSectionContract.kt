package com.woocommerce.android.ui.analytics.informationcard

class AnalyticsInformationSectionContract {
    sealed class SectionViewState {
        object SectionHiddenViewState : SectionViewState()
        data class SectionDataViewState(val title: String, val value: String, val delta: Int) : SectionViewState() {
            fun getSign(): String = if (delta > 0) "+" else "-"
        }
    }
}

package com.woocommerce.android.ui.analytics.informationcard

sealed class SectionViewState {
    object SectionHiddenViewState : SectionViewState()
    data class SectionDataViewState(val title: String, val value: String, val delta: Int) : SectionViewState() {
        fun getSign(): String = when {
            delta > 0 -> "+"
            delta == 0 -> ""
            else -> "-"
        }
    }
}

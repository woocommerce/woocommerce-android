package com.woocommerce.android.ui.woopos.root

data class BottomToolbarState(
    val cardReaderStatus: CardReaderStatus
) {
    data class CardReaderStatus(
        val title: String,
    )
}

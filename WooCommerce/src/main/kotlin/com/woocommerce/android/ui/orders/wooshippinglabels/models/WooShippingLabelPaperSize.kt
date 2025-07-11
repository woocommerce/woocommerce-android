package com.woocommerce.android.ui.orders.wooshippinglabels.models

import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import com.woocommerce.android.R

enum class WooShippingLabelPaperSize(@StringRes val stringResource: Int) {
    @SerializedName("a4")
    A4(R.string.shipping_label_paper_size_a4),

    @SerializedName("label")
    LABEL(R.string.shipping_label_paper_size_label),

    @SerializedName("letter")
    LETTER(R.string.shipping_label_paper_size_letter)
}

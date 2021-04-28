package com.woocommerce.android.ui.products.viewholders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView.ViewHolder

open class ProductPropertyViewHolder(
    view: View
) : ViewHolder(view) {
    constructor(
        parent: ViewGroup,
        @LayoutRes layout: Int
    ) : this(LayoutInflater.from(parent.context).inflate(layout, parent, false))
}

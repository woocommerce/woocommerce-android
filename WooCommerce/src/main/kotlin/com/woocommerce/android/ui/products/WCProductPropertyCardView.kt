package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.ui.products.adapters.ProductPropertiesAdapter
import com.woocommerce.android.ui.products.models.ProductProperty
import kotlinx.android.synthetic.main.product_property_cardview_layout.view.*

/**
 * CardView with an optional caption (title), used for product detail properties
 */
class WCProductPropertyCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : MaterialCardView(context, attrs, defStyle) {
    private var view: View = View.inflate(context, R.layout.product_property_cardview_layout, this)

    fun show(caption: String?, properties: List<ProductProperty>) {
        val captionTextView = view.findViewById<MaterialTextView>(R.id.cardCaptionText)
        val divider = view.findViewById<View>(R.id.cardCaptionDivider)
        if (caption.isNullOrBlank()) {
            captionTextView.visibility = View.GONE
            divider.hide()
        } else {
            captionTextView.visibility = View.VISIBLE
            captionTextView.text = caption
            divider.show()
        }

        if (propertiesRecyclerView.layoutManager == null) {
            propertiesRecyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            propertiesRecyclerView.itemAnimator = null
        }

        loadData(properties)
    }

    private fun loadData(data: List<ProductProperty>) {
        val adapter: ProductPropertiesAdapter
        if (propertiesRecyclerView.adapter == null) {
            adapter = ProductPropertiesAdapter()
            propertiesRecyclerView.adapter = adapter
        } else {
            adapter = propertiesRecyclerView.adapter as ProductPropertiesAdapter
        }

        val recyclerViewState = propertiesRecyclerView.layoutManager?.onSaveInstanceState()
        adapter.update(data)
        propertiesRecyclerView.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }
}

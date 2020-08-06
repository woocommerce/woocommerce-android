package com.woocommerce.android.ui.products.viewholders

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.woocommerce.android.R
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.ui.products.models.ProductProperty.Switch

class SwitchViewHolder(parent: ViewGroup) : ProductPropertyViewHolder(parent, R.layout.product_property_switch_layout) {
    fun bind(item: Switch) {
        val title = itemView.findViewById<TextView>(R.id.switchTitle)
        val icon = itemView.findViewById<ImageView>(R.id.switchIcon)
        val switch = itemView.findViewById<SwitchCompat>(R.id.switchView)

        title.text = itemView.context.getString(item.title)
        switch.isChecked = item.isOn

        if (item.icon != null) {
            icon.setImageDrawable(itemView.context.getDrawable(item.icon))
            icon.show()
        } else {
            icon.hide()
        }

        itemView.setOnClickListener {
            switch.toggle()
        }
        
        if (item.onStateChanged != null) {
            switch.setOnCheckedChangeListener { _, isOn ->
                (item.onStateChanged)(isOn)
            }
            switch.isEnabled = true
            itemView.isEnabled = true
        } else {
            switch.isEnabled = false
            itemView.isEnabled = false
        }
    }
}

package com.woocommerce.android.ui.main

import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.woocommerce.android.R
import org.wordpress.android.mediapicker.util.setImageResourceWithTint

sealed class FabStatus {
    object Hidden : FabStatus()
    data class Visible(
        @StringRes
        val contentDescription: Int,
        @DrawableRes
        val icon: Int,
        @ColorRes
        val tint: Int = R.color.white,
        @ColorRes
        val backgroundTint: Int? = R.color.color_secondary,
        val onClick: View.OnClickListener? = null
    ) : FabStatus()
}

fun FloatingActionButton.setVisibleFabStatus(status: FabStatus.Visible) {
    this.setImageResourceWithTint(status.icon, status.tint)
    status.backgroundTint?.let {
        this.backgroundTintList = AppCompatResources.getColorStateList(this.context, it)
    }
    setOnClickListener(status.onClick)
}

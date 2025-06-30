package com.woocommerce.android.ui.orders.wooshippinglabels.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R

@Composable
fun NoticeBanner(noticeBannerUiState: NoticeBannerUiState?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = noticeBannerUiState != null,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 180)
        ) + scaleIn(
            animationSpec = tween(durationMillis = 180)
        )
    ) {
        if (noticeBannerUiState == null) return@AnimatedVisibility

        val icon = if (noticeBannerUiState.error) {
            Icons.Outlined.Info
        } else {
            Icons.Outlined.CheckCircleOutline
        }

        val color = if (noticeBannerUiState.error) {
            R.color.woo_shipping_label_error
        } else {
            R.color.woo_shipping_label_success
        }

        val backgroundColor = if (noticeBannerUiState.error) {
            R.color.woo_shipping_label_error_surface
        } else {
            R.color.woo_shipping_label_success_surface
        }

        val rowModifier =
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            } else {
                modifier.fillMaxWidth()
            }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = rowModifier
                .padding(dimensionResource(R.dimen.major_100))
                .background(
                    color = colorResource(backgroundColor),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
                )
                .clickable(enabled = noticeBannerUiState.onTapped != null) {
                    noticeBannerUiState.onTapped?.invoke()
                }
                .padding(vertical = 8.dp, horizontal = 16.dp),
        ) {
            Icon(
                imageVector = icon,
                tint = colorResource(color),
                contentDescription = null
            )
            Spacer(Modifier.size(dimensionResource(R.dimen.minor_100)))
            Text(
                text = stringResource(noticeBannerUiState.message),
                style = MaterialTheme.typography.bodyMedium,
                color = colorResource(color),
                modifier = Modifier.weight(1f)
            )
            if (!noticeBannerUiState.autoDismiss) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    tint = colorResource(color),
                    contentDescription = null,
                    modifier = Modifier.clickable { noticeBannerUiState.onDismissed?.invoke() }
                )
            }
        }
    }
}

package com.woocommerce.android.ui.woopos.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartViewModel

@Composable
fun WooPosPhoneCartFab(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val cartViewModel: WooPosCartViewModel = hiltViewModel()
    val cartState by cartViewModel.state.observeAsState()
    val itemCount = cartState?.body?.amountOfItems ?: 0

    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_add_shopping_cart_24dp),
                contentDescription = "Cart",
                modifier = Modifier.size(24.dp),
            )
        }

        if (itemCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(22.dp)
                    .background(
                        color = Color.White,
                        shape = CircleShape,
                    )
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                WooPosText(
                    text = itemCount.toString(),
                    style = WooPosTypography.BodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

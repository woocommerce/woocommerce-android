package com.woocommerce.android.ui.moremenu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells.Fixed
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.R.color

@ExperimentalFoundationApi
@Composable
fun MoreMenu(buttons: List<MenuButton>, settingsOnClick: () -> Unit = {}) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = { settingsOnClick() },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_more_screen_settings),
                    contentDescription = stringResource(id = R.string.settings),
                    tint = Color.Unspecified
                )
            }
        }
        LazyVerticalGrid(
            cells = Fixed(2),
            contentPadding = PaddingValues(ButtonDefaults.IconSpacing),
            horizontalArrangement = Arrangement.spacedBy(ButtonDefaults.IconSpacing),
            verticalArrangement = Arrangement.spacedBy(ButtonDefaults.IconSpacing)
        ) {
            itemsIndexed(buttons) { _, item ->
                MoreMenuButton(
                    text = item.text,
                    iconDrawable = item.icon,
                    badgeCount = item.badgeCount,
                    onClick = item.onClick
                )
            }
        }
    }
}

@Composable
private fun MoreMenuButton(
    @StringRes text: Int,
    @DrawableRes iconDrawable: Int,
    badgeCount: Int,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(12.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colorResource(id = color.color_surface)
        ),
        modifier = Modifier.height(190.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            MoreMenuBadge(badgeCount = badgeCount)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(colorResource(id = color.woo_gray_0))
                ) {
                    Image(
                        painter = painterResource(id = iconDrawable),
                        contentDescription = stringResource(id = text),
                        modifier = Modifier
                            .size(35.dp)
                            .align(Alignment.Center)
                    )
                }
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(
                    text = stringResource(id = text),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = colorResource(id = color.color_on_surface)
                )
            }
        }
    }
}


@Composable
fun MoreMenuBadge(badgeCount: Int) {
    if (badgeCount > 0) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(colorResource(id = color.color_primary))
            ) {
                Text(
                    text = badgeCount.toString(),
                    fontSize = 13.sp,
                    color = colorResource(id = color.color_on_surface_inverted),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
fun MoreMenuPreview() {
    val buttons = listOf(
        MenuButton(R.string.more_menu_button_woo_admin, R.drawable.ic_more_menu_wp_admin),
        MenuButton(R.string.more_menu_button_store, R.drawable.ic_more_menu_store),
        MenuButton(R.string.more_menu_button_analytics, R.drawable.ic_more_menu_analytics),
        MenuButton(R.string.more_menu_button_payments, R.drawable.ic_more_menu_payments),
        MenuButton(R.string.more_menu_button_inbox, R.drawable.ic_more_menu_inbox),
        MenuButton(R.string.more_menu_button_reviews, R.drawable.ic_more_menu_reviews)
    )
    MoreMenu(buttons)
}

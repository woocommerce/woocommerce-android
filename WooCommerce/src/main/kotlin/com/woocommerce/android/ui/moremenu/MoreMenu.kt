package com.woocommerce.android.ui.moremenu

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells.Fixed
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.woocommerce.android.R.color
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.moremenu.MenuButtonType.COUPONS
import com.woocommerce.android.ui.moremenu.MenuButtonType.PRODUCT_REVIEWS
import com.woocommerce.android.ui.moremenu.MenuButtonType.VIEW_ADMIN
import com.woocommerce.android.ui.moremenu.MenuButtonType.VIEW_STORE
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuViewState

@ExperimentalFoundationApi
@Composable
fun MoreMenu(viewModel: MoreMenuViewModel) {
    val moreMenuState by viewModel.moreMenuViewState.observeAsState(initial = (MoreMenuViewState()))
    MoreMenu(
        moreMenuState,
        viewModel::onSwitchStoreClick,
        viewModel::onSettingsClick
    )
}

@ExperimentalFoundationApi
@Composable
@Suppress("LongMethod")
fun MoreMenu(
    state: MoreMenuViewState,
    onSwitchStore: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column {
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MoreMenuMyStoreHeader(
                    state.userAvatarUrl,
                    state.siteName,
                    state.siteUrl,
                    onSwitchStore
                )
            }
            SettingsButton(onSettingsClick)
        }
        LazyVerticalGrid(
            cells = Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                state.moreMenuItems.filter { it.isEnabled }
            ) { _, item ->
                MoreMenuButton(
                    text = item.text,
                    iconDrawable = item.icon,
                    badgeCount = item.badgeCount,
                    onClick = { item.onClick(item.type) }
                )
            }
        }
    }
}

@Composable
private fun SettingsButton(onSettingsClick: () -> Unit) {
    IconButton(
        onClick = { onSettingsClick() },
    ) {
        Icon(
            painter = painterResource(id = drawable.ic_more_screen_settings),
            contentDescription = stringResource(id = string.settings),
            tint = Color.Unspecified
        )
    }
}

@Composable
private fun MoreMenuMyStoreHeader(
    userAvatarUrl: String,
    siteName: String,
    siteUrl: String,
    onSwitchStore: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(
                enabled = true,
                onClickLabel = stringResource(id = string.settings_switch_store),
                role = Role.Button,
                onClick = onSwitchStore
            )
            .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Column {
            Spacer(modifier = Modifier.height(8.dp))
            MoreMenuUserAvatar(avatarUrl = userAvatarUrl)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = siteName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h6,
            )
            Text(
                text = siteUrl,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = stringResource(string.settings_switch_store),
                color = MaterialTheme.colors.secondary,
                style = MaterialTheme.typography.body2,
            )
        }
    }
}

@Composable
private fun MoreMenuUserAvatar(avatarUrl: String) {
    val bitmapState = remember { mutableStateOf<Bitmap?>(null) }

    if (avatarUrl.isNotEmpty()) {
        Glide.with(LocalContext.current)
            .asBitmap()
            .load(avatarUrl)
            .into(
                object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        bitmapState.value = resource
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // Nothing to do here.
                    }
                }
            )
    }

    val circledModifier = Modifier
        .size(48.dp)
        .clip(CircleShape)

    bitmapState.value?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = stringResource(id = string.more_menu_avatar),
            modifier = circledModifier
        )
    } ?: Image(
        painter = painterResource(id = drawable.img_gravatar_placeholder),
        contentDescription = stringResource(id = string.more_menu_avatar),
        modifier = circledModifier
    )
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
            backgroundColor = colorResource(id = color.more_menu_button_background)
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
                        .background(colorResource(id = color.more_menu_button_icon_background))
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
                    .background(MaterialTheme.colors.primary)
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
@Suppress("MagicNumber")
fun MoreMenuPreview() {
    val state = MoreMenuViewState(
        moreMenuItems = listOf(
            MenuUiButton(VIEW_ADMIN, string.more_menu_button_woo_admin, drawable.ic_more_menu_wp_admin),
            MenuUiButton(VIEW_STORE, string.more_menu_button_store, drawable.ic_more_menu_store),
            MenuUiButton(COUPONS, string.more_menu_button_coupons, drawable.ic_more_menu_coupons),
            MenuUiButton(PRODUCT_REVIEWS, string.more_menu_button_reviews, drawable.ic_more_menu_reviews, 3)
        ),
        siteName = "Example Site",
        siteUrl = "woocommerce.com",
        userAvatarUrl = "" // To force displaying placeholder image
    )
    MoreMenu(state, {}, {})
}

package com.woocommerce.android.ui.moremenu

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.GridCells.Fixed
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.woocommerce.android.R
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
fun MoreMenuScreen(viewModel: MoreMenuViewModel) {
    val moreMenuState by viewModel.moreMenuViewState.observeAsState(initial = (MoreMenuViewState()))
    MoreMenuScreen(
        moreMenuState,
        viewModel::onSwitchStoreClick,
        viewModel::onSettingsClick
    )
}

@ExperimentalFoundationApi
@Composable
fun MoreMenuScreen(
    state: MoreMenuViewState,
    onSwitchStore: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        MoreMenuHeader(onSwitchStore, state, onSettingsClick)
        MoreMenuItems(state)
    }
}

@ExperimentalFoundationApi
@Composable
private fun MoreMenuItems(state: MoreMenuViewState) {
    LazyVerticalGrid(
        cells = Fixed(2),
        contentPadding = PaddingValues(
            horizontal = dimensionResource(id = R.dimen.major_100),
            vertical = dimensionResource(id = R.dimen.major_100)
        ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_75)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_75))
    ) {
        itemsIndexed(
            state.moreMenuItems.filter { it.isEnabled }
        ) { _, item ->
            MoreMenuButton(
                text = item.text,
                iconDrawable = item.icon,
                badgeCount = item.badgeCount,
                onClick = item.onClick
            )
        }
    }
}

@Composable
private fun MoreMenuHeader(
    onSwitchStore: () -> Unit,
    state: MoreMenuViewState,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = true,
                onClickLabel = stringResource(id = string.settings_switch_store),
                role = Role.Button,
                onClick = onSwitchStore
            )
            .padding(
                top = dimensionResource(id = R.dimen.major_100),
                bottom = dimensionResource(id = R.dimen.major_100)
            ),
    ) {
        StoreDetailsHeader(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(
                    start = dimensionResource(id = R.dimen.minor_100),
                    end = dimensionResource(id = R.dimen.major_325)
                ),
            userAvatarUrl = state.userAvatarUrl,
            siteName = state.siteName,
            siteUrl = state.siteUrl
        )
        SettingsButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = dimensionResource(id = R.dimen.minor_100)),
            onSettingsClick = onSettingsClick
        )
    }
}

@Composable
private fun SettingsButton(modifier: Modifier, onSettingsClick: () -> Unit) {
    IconButton(
        modifier = modifier,
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
private fun StoreDetailsHeader(
    modifier: Modifier,
    userAvatarUrl: String,
    siteName: String,
    siteUrl: String
) {
    Row(modifier = modifier) {
        MoreMenuUserAvatar(avatarUrl = userAvatarUrl)
        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.major_100)))
        Column {
            Text(
                text = siteName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.h6,
            )
            Text(
                text = siteUrl,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.minor_50))
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
        .size(dimensionResource(id = R.dimen.major_300))
        .padding(
            top = dimensionResource(id = R.dimen.minor_75),
            start = dimensionResource(id = R.dimen.minor_100)
        )
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
        contentPadding = PaddingValues(dimensionResource(id = R.dimen.major_75)),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colorResource(id = color.more_menu_button_background)
        ),
        modifier = Modifier.height(dimensionResource(id = R.dimen.more_menu_button_height)),
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.major_75))
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
                        .size(dimensionResource(id = R.dimen.major_350))
                        .clip(CircleShape)
                        .background(colorResource(id = color.more_menu_button_icon_background))
                ) {
                    Image(
                        painter = painterResource(id = iconDrawable),
                        contentDescription = stringResource(id = text),
                        modifier = Modifier
                            .size(dimensionResource(id = R.dimen.major_200))
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
                    .size(dimensionResource(id = R.dimen.major_150))
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
private fun MoreMenuPreview() {
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
    MoreMenuScreen(state, {}, {})
}

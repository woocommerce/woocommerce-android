package com.woocommerce.android.ui.moremenu

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells.Fixed
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.*
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
import com.woocommerce.android.ui.moremenu.MenuButtonType.*
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuViewState

@ExperimentalFoundationApi
@Composable
fun MoreMenu(viewModel: MoreMenuViewModel) {
    val moreMenuState by viewModel.moreMenuViewState.observeAsState(initial = (MoreMenuViewState()))
    MoreMenu(
        moreMenuState.moreMenuItems,
        moreMenuState.siteName,
        moreMenuState.siteUrl,
        moreMenuState.userAvatarUrl,
        viewModel::onSwitchStoreClick,
        viewModel::onSettingsClick
    )
}

@ExperimentalFoundationApi
@Composable
@Suppress("LongMethod", "FunctionNaming", "LongParameterList")
fun MoreMenu(
    uiButtons: List<MenuUiButton>,
    siteName: String,
    siteUrl: String,
    userAvatarUrl: String,
    onSwitchStore: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column {
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth(fraction = 0.8f)
            ) {
                Row {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        MoreMenuUserAvatar(avatarUrl = userAvatarUrl)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = siteName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = colorResource(id = color.color_on_surface)
                        )
                        Text(
                            text = siteUrl,
                            fontSize = 14.sp,
                            color = colorResource(id = color.color_on_surface),
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                        )
                        Text(
                            text = stringResource(string.settings_switch_store),
                            color = colorResource(color.color_secondary),
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable(
                                    enabled = true,
                                    onClickLabel = stringResource(id = string.settings_switch_store),
                                    role = Role.Button
                                ) { onSwitchStore() }
                        )
                    }
                }
            }

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
        Spacer(modifier = Modifier.height(32.dp))
        LazyVerticalGrid(
            cells = Fixed(2),
            contentPadding = PaddingValues(ButtonDefaults.IconSpacing),
            horizontalArrangement = Arrangement.spacedBy(ButtonDefaults.IconSpacing),
            verticalArrangement = Arrangement.spacedBy(ButtonDefaults.IconSpacing)
        ) {
            itemsIndexed(uiButtons) { _, item ->
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
private fun MoreMenuUserAvatar(avatarUrl: String) {
    val bitmapState = remember { mutableStateOf<Bitmap?>(null) }
    Glide.with(LocalContext.current)
        .asBitmap()
        .load(avatarUrl)
        .into(
            object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    bitmapState.value = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            }
        )
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
@Suppress("MagicNumber")
fun MoreMenuPreview() {
    val buttons = listOf(
        MenuUiButton(VIEW_ADMIN, string.more_menu_button_woo_admin, drawable.ic_more_menu_wp_admin),
        MenuUiButton(VIEW_STORE, string.more_menu_button_store, drawable.ic_more_menu_store),
        MenuUiButton(PRODUCT_REVIEWS, string.more_menu_button_reviews, drawable.ic_more_menu_reviews, 3)
    )
    val exampleSiteName = "Example Site"
    val exampleSiteUrl = "woocommerce.com"
    val exampleUserAvatarUrl = "https://woocommerce.com/"
    MoreMenu(uiButtons = buttons, exampleSiteName, exampleSiteUrl, exampleUserAvatarUrl, {}, {})
}

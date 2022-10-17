package com.woocommerce.android.ui.moremenu

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
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
                badgeState = item.badgeState,
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
    badgeState: BadgeState?,
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
            MoreMenuBadge(badgeState = badgeState)
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
fun MoreMenuBadge(badgeState: BadgeState?) {
    if (badgeState != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            val visible = remember {
                MutableTransitionState(badgeState.animateAppearance.not()).apply { targetState = true }
            }
            AnimatedVisibility(
                visibleState = visible,
                enter = BadgeEnterAnimation()
            ) {
                val backgroundColor = colorResource(id = badgeState.backgroundColor)
                Text(
                    text = badgeState.textState.text,
                    fontSize = dimensionResource(id = badgeState.textState.fontSize).value.sp,
                    color = colorResource(id = badgeState.textColor),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .size(dimensionResource(id = badgeState.badgeSize))
                        .drawBehind { drawCircle(color = backgroundColor) }
                        .wrapContentHeight()
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun BadgeEnterAnimation(): EnterTransition {
    val animationSpec = TweenSpec<Float>(durationMillis = 400, delay = 200)
    return scaleIn(animationSpec = animationSpec) + fadeIn(animationSpec = animationSpec)
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Composable
private fun MoreMenuPreview() {
    val state = MoreMenuViewState(
        moreMenuItems = listOf(
            MenuUiButton(
                string.more_menu_button_payments, drawable.ic_more_menu_payments,
                BadgeState(
                    badgeSize = R.dimen.major_110,
                    backgroundColor = color.color_secondary,
                    textColor = color.color_on_surface_inverted,
                    textState = TextState("", R.dimen.text_minor_80),
                    animateAppearance = true
                )
            ),
            MenuUiButton(string.more_menu_button_wс_admin, drawable.ic_more_menu_wp_admin),
            MenuUiButton(string.more_menu_button_store, drawable.ic_more_menu_store),
            MenuUiButton(
                string.more_menu_button_reviews, drawable.ic_more_menu_reviews,
                BadgeState(
                    badgeSize = R.dimen.major_150,
                    backgroundColor = color.color_primary,
                    textColor = color.color_on_surface_inverted,
                    textState = TextState("3", R.dimen.text_minor_80),
                    animateAppearance = false
                )
            ),
            MenuUiButton(string.more_menu_button_coupons, drawable.ic_more_menu_coupons),
        ),
        siteName = "Example Site",
        siteUrl = "woocommerce.com",
        userAvatarUrl = "" // To force displaying placeholder image
    )
    MoreMenuScreen(state, {}, {})
}

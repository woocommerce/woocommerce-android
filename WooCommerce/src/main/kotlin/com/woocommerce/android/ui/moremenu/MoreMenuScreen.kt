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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuViewState

@ExperimentalFoundationApi
@Composable
fun MoreMenuScreen(viewModel: MoreMenuViewModel) {
    val moreMenuState by viewModel.moreMenuViewState.observeAsState(initial = (MoreMenuViewState()))
    MoreMenuScreen(
        moreMenuState,
        viewModel::onSwitchStoreClick
    )
}

@ExperimentalFoundationApi
@Composable
fun MoreMenuScreen(
    state: MoreMenuViewState,
    onSwitchStore: () -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        MoreMenuHeader(onSwitchStore, state)

        if (state.settingsMenuItems.isNotEmpty()) {
            MoreMenuSection(
                title = stringResource(id = R.string.more_menu_settings_section_title),
                items = state.settingsMenuItems
            )
        }

        if (state.generalMenuItems.isNotEmpty()) {
            MoreMenuSection(
                title = stringResource(id = R.string.more_menu_general_section_title),
                items = state.generalMenuItems
            )
        }
    }
}

@ExperimentalFoundationApi
@Composable
private fun MoreMenuSection(
    title: String,
    items: List<MenuUiButton>
) {
    Column(
        modifier = Modifier.padding(
            PaddingValues(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.major_100)
            )
        )
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = R.color.color_on_surface)
        )

        items.forEach { item ->
            MoreMenuButton(
                title = item.title,
                description = item.description,
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
    state: MoreMenuViewState
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = state.isStoreSwitcherEnabled,
                onClickLabel = stringResource(id = R.string.settings_switch_store),
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
            siteUrl = state.siteUrl,
            isStoreSwitcherEnabled = state.isStoreSwitcherEnabled
        )
    }
}

@Composable
private fun StoreDetailsHeader(
    modifier: Modifier,
    userAvatarUrl: String,
    siteName: String,
    siteUrl: String,
    isStoreSwitcherEnabled: Boolean
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
            if (isStoreSwitcherEnabled) {
                Text(
                    text = stringResource(R.string.settings_switch_store),
                    color = MaterialTheme.colors.primary,
                    style = MaterialTheme.typography.body2,
                )
            }
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
            contentScale = ContentScale.Crop,
            contentDescription = stringResource(id = R.string.more_menu_avatar),
            modifier = circledModifier
        )
    } ?: Image(
        painter = painterResource(id = R.drawable.img_gravatar_placeholder),
        contentDescription = stringResource(id = R.string.more_menu_avatar),
        modifier = circledModifier
    )
}

@Composable
private fun MoreMenuButton(
    @StringRes title: Int,
    @StringRes description: Int,
    @DrawableRes iconDrawable: Int,
    badgeState: BadgeState?,
    onClick: () -> Unit,
) {
    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_75)))
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(dimensionResource(id = R.dimen.major_75)),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colorResource(id = R.color.more_menu_button_background)
        ),
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.major_75))
    ) {
        Box(Modifier.fillMaxSize()) {
            MoreMenuBadge(badgeState = badgeState)
            Row(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .size(dimensionResource(id = R.dimen.major_250))
                        .clip(CircleShape)
                        .background(colorResource(id = R.color.more_menu_button_icon_background))
                ) {
                    Image(
                        painter = painterResource(id = iconDrawable),
                        contentDescription = stringResource(id = title),
                        modifier = Modifier
                            .size(dimensionResource(id = R.dimen.major_125))
                            .align(Alignment.Center)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(start = dimensionResource(id = R.dimen.major_100))
                ) {
                    Text(
                        text = stringResource(id = title),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(id = description),
                        style = MaterialTheme.typography.caption,
                        textAlign = TextAlign.Center,
                    )
                }
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
                enter = createBadgeEnterAnimation()
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
private fun createBadgeEnterAnimation(): EnterTransition {
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
        generalMenuItems = listOf(
            MenuUiButton(
                R.string.more_menu_button_payments,
                R.string.more_menu_button_payments_description,
                R.drawable.ic_more_menu_payments,
                BadgeState(
                    badgeSize = R.dimen.major_110,
                    backgroundColor = R.color.color_secondary,
                    textColor = R.color.color_on_primary,
                    textState = TextState("", R.dimen.text_minor_80),
                    animateAppearance = true
                )
            ),
            MenuUiButton(
                R.string.more_menu_button_wс_admin,
                R.string.more_menu_button_wc_admin_description,
                R.drawable.ic_more_menu_wp_admin
            ),
            MenuUiButton(
                R.string.more_menu_button_store,
                R.string.more_menu_button_store_description,
                R.drawable.ic_more_menu_store
            ),
            MenuUiButton(
                R.string.more_menu_button_reviews,
                R.string.more_menu_button_reviews_description,
                R.drawable.ic_more_menu_reviews,
                BadgeState(
                    badgeSize = R.dimen.major_150,
                    backgroundColor = R.color.color_primary,
                    textColor = R.color.color_on_primary,
                    textState = TextState("3", R.dimen.text_minor_80),
                    animateAppearance = false
                )
            ),
            MenuUiButton(
                R.string.more_menu_button_coupons,
                R.string.more_menu_button_coupons_description,
                R.drawable.ic_more_menu_coupons
            ),
        ),
        settingsMenuItems = listOf(
            MenuUiButton(
                R.string.more_menu_button_settings,
                R.string.more_menu_button_settings_description,
                R.drawable.ic_more_screen_settings
            ),
            MenuUiButton(
                R.string.more_menu_button_upgrades,
                R.string.more_menu_button_upgrades_description,
                R.drawable.ic_more_menu_upgrades
            )
        ),
        siteName = "Example Site",
        siteUrl = "woocommerce.com",
        userAvatarUrl = "" // To force displaying placeholder image
    )
    MoreMenuScreen(state, {})
}

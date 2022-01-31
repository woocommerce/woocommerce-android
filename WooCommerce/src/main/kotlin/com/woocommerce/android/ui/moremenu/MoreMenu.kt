package com.woocommerce.android.ui.moremenu

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import coil.transform.CircleCropTransformation
import com.woocommerce.android.R.color
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuViewState

@ExperimentalFoundationApi
@Composable
@Suppress("FunctionNaming")
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
            modifier = Modifier
                .fillMaxWidth(),
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
                        Image(
                            painter = rememberImagePainter(
                                data = userAvatarUrl,
                                builder = {
                                    transformations(CircleCropTransformation())
                                    placeholder(drawable.img_gravatar_placeholder)
                                }
                            ),
                            contentDescription = stringResource(id = string.more_menu_avatar),
                            modifier = Modifier
                                .size(48.dp)
                        )
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
                    onClick = item.onClick
                )
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun MoreMenuButton(
    @StringRes text: Int,
    @DrawableRes iconDrawable: Int,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(20.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colorResource(id = color.color_surface)
        ),
        modifier = Modifier.height(190.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 30.dp)
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
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = colorResource(id = color.color_on_surface)
            )
        }
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
@Suppress("FunctionNaming")
fun MoreMenuPreview() {
    val buttons = listOf(
        MenuUiButton(string.more_menu_button_woo_admin, drawable.ic_more_menu_wp_admin),
        MenuUiButton(string.more_menu_button_store, drawable.ic_more_menu_store),
        MenuUiButton(string.more_menu_button_reviews, drawable.ic_more_menu_reviews)
    )
    val exampleSiteName = "Example Site"
    val exampleSiteUrl = "woocommerce.com"
    val exampleUserAvatarUrl = "https://woocommerce.com/"
    MoreMenu(uiButtons = buttons, exampleSiteName, exampleSiteUrl, exampleUserAvatarUrl, {}, {})
}

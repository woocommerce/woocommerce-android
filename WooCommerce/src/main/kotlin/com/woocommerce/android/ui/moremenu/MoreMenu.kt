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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.moremenu.MenuButtonType.*
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuViewState

@ExperimentalFoundationApi
@Composable
@Suppress("LongMethod")
fun moreMenu(
    buttons: List<MenuButton>
) {
    val viewModel: MoreMenuViewModel = viewModel()
    val state: MoreMenuViewState by viewModel.viewStateLiveData.liveData.observeAsState(MoreMenuViewState())

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .padding(10.dp)
            ) {
                Text(
                    text = stringResource(string.settings_switch_store),
                    color = colorResource(color.color_secondary),
                    modifier = Modifier
                        .clickable(
                            enabled = true,
                            onClickLabel = stringResource(id = string.settings_switch_store),
                            role = Role.Button
                        ) {
                            viewModel.onSwitchSiteClick()
                        }
                )
            }

            IconButton(
                onClick = { viewModel.onSettingsClick() },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_more_screen_settings),
                    contentDescription = stringResource(id = string.settings),
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
                when (item.type) {
                    VIEW_ADMIN -> {
                        moreMenuButton(
                            text = item.text,
                            iconDrawable = item.icon,
                            onClick = { viewModel.onViewAdminButtonClick(state.adminUrl) }
                        )
                    }
                    VIEW_STORE -> {
                        moreMenuButton(
                            text = item.text,
                            iconDrawable = item.icon,
                            onClick = { viewModel.onViewStoreButtonClick(state.storeUrl) }
                        )
                    }
                    REVIEWS -> {
                        moreMenuButton(
                            text = item.text,
                            iconDrawable = item.icon,
                            onClick = { viewModel.onReviewsButtonClick() }
                        )
                    }
                    else -> {
                        moreMenuButton(
                            text = item.text,
                            iconDrawable = item.icon,
                            onClick = item.onClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun moreMenuButton(
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
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = colorResource(id = color.color_on_surface)
            )
        }
    }
}

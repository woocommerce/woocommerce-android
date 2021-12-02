package com.woocommerce.android.ui.moremenu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.GridCells.Fixed
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R.color

@ExperimentalFoundationApi
@Composable
fun MoreMenu(buttons: List<MenuButton>) {
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
                onClick = item.onClick
            )
        }
    }
}

@Composable
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
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = colorResource(id = color.color_on_surface)
            )
        }
    }
}

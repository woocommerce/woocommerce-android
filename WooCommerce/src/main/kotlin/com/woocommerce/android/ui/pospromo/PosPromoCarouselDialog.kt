package com.woocommerce.android.ui.pospromo

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun PosPromoCarouselModal(
    state: PosPromoState,
    onDismiss: () -> Unit,
    onNextClick: () -> Unit,
    onExploreClick: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        PosPromoCarouselContent(
            state = state,
            onDismiss = onDismiss,
            onNextClick = onNextClick,
            onExploreClick = onExploreClick,
        )
    }
}

@Composable
private fun PosPromoCarouselContent(
    state: PosPromoState,
    onDismiss: () -> Unit,
    onNextClick: () -> Unit,
    onExploreClick: () -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = state.currentPage,
        pageCount = { state.pages.size }
    )
    val isLastPage = state.currentPage == state.pages.size - 1

    LaunchedEffect(state.currentPage) {
        pagerState.animateScrollToPage(state.currentPage)
    }

    val scrollState = rememberScrollState()
    val spacing = 16.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 534.dp),
        shape = RoundedCornerShape(size = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box {
                Image(
                    painter = painterResource(id = state.imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(209.dp)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_close_24dp),
                        contentDescription = stringResource(R.string.close),
                        tint = colorResource(R.color.color_on_primary),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = spacing),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Spacer(modifier = Modifier.height(spacing))

                Text(
                    text = stringResource(state.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(spacing))

                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) { page ->
                    Text(
                        text = stringResource(state.pages[page].descriptionRes),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(spacing))

                PageIndicator(
                    pageCount = state.pages.size,
                    currentPage = pagerState.currentPage,
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            if (isLastPage) {
                WCColoredButton(
                    onClick = onExploreClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing)
                        .padding(bottom = spacing)
                ) {
                    Text(text = stringResource(R.string.woo_pos_promo_explore_button))
                }
            } else {
                WCOutlinedButton(
                    onClick = onNextClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing)
                        .padding(bottom = spacing)
                ) {
                    Text(text = stringResource(R.string.woo_pos_promo_next_button))
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        }
                    )
            )
        }
    }
}

@Preview(name = "Page 1 - Light")
@Preview(name = "Page 1 - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(
    name = "Page 1 - lanscape",
    device = "spec:width=674dp,height=800dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape",
)
@Composable
private fun PosPromoPage1Preview() {
    WooThemeWithBackground {
        PosPromoCarouselContent(
            state = PosPromoState(currentPage = 0),
            onDismiss = {},
            onNextClick = {},
            onExploreClick = {},
        )
    }
}

@Preview(name = "Page 2 - Light")
@Preview(name = "Page 2 - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PosPromoPage2Preview() {
    WooThemeWithBackground {
        PosPromoCarouselContent(
            state = PosPromoState(currentPage = 1),
            onDismiss = {},
            onNextClick = {},
            onExploreClick = {},
        )
    }
}

@Preview(name = "Page 3 - Light")
@Preview(name = "Page 3 - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PosPromoPage3Preview() {
    WooThemeWithBackground {
        PosPromoCarouselContent(
            state = PosPromoState(currentPage = 2),
            onDismiss = {},
            onNextClick = {},
            onExploreClick = {},
        )
    }
}

@Preview(name = "Page 4 - Light")
@Preview(name = "Page 4 - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PosPromoPage4Preview() {
    WooThemeWithBackground {
        PosPromoCarouselContent(
            state = PosPromoState(currentPage = 3),
            onDismiss = {},
            onNextClick = {},
            onExploreClick = {},
        )
    }
}

@Preview(name = "Page 5 - Light")
@Preview(name = "Page 5 - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PosPromoPage5Preview() {
    WooThemeWithBackground {
        PosPromoCarouselContent(
            state = PosPromoState(currentPage = 4),
            onDismiss = {},
            onNextClick = {},
            onExploreClick = {},
        )
    }
}

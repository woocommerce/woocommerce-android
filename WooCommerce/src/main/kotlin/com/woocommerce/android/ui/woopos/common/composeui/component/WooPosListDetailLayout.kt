package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.woopos.util.ext.isWooPosPhoneLayout

@Composable
fun WooPosListDetailLayout(
    isDetailVisible: Boolean,
    onBackFromDetail: () -> Unit,
    listPane: @Composable () -> Unit,
    detailPane: @Composable () -> Unit,
    emptyDetailPane: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    listWeight: Float = 0.3f,
    detailWeight: Float = 0.7f,
) {
    val isPhone = isWooPosPhoneLayout()

    if (isPhone) {
        BackHandler(enabled = isDetailVisible) {
            onBackFromDetail()
        }

        AnimatedContent(
            targetState = isDetailVisible,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally(initialOffsetX = { it }) + fadeIn()) togetherWith
                        (slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
                } else {
                    (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()) togetherWith
                        (slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
                }
            },
            label = "list_detail_transition",
            modifier = modifier.fillMaxSize(),
        ) { showDetail ->
            if (showDetail) {
                detailPane()
            } else {
                listPane()
            }
        }
    } else {
        Row(modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(listWeight)) {
                listPane()
            }
            Box(modifier = Modifier.weight(detailWeight)) {
                if (isDetailVisible) {
                    detailPane()
                } else {
                    emptyDetailPane()
                }
            }
        }
    }
}

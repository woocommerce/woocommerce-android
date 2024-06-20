package com.woocommerce.android.ui.compose.component

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Colors
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Reviews
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Suppress("MagicNumber")
val Colors.feedbackSurface: Color get() = if (isLight) Color(0xFF1C1C1E) else Color(0xFFBEA0F2)

@Suppress("MagicNumber")
val Colors.onFeedbackSurface: Color get() = if (isLight) Color(0xFFBEA0F2) else Color(0xFF1C1C1E)

@Composable
fun FeedbackDialog(
    title: String,
    message: String,
    action: String,
    isShown: Boolean,
    onAction: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorStops = arrayOf(
        .6f to Color.Transparent,
        1f to MaterialTheme.colors.feedbackSurface.copy(alpha = .4f),
    )

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = isShown,
            enter = fadeIn(),
            exit = fadeOut(),
            label = "background_transition",
        ) {
            Box(
                modifier = modifier
                    .background(Brush.verticalGradient(colorStops = colorStops))
                    .fillMaxSize()
            )
        }

        AnimatedVisibility(
            visible = isShown,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            label = "card_transition",
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                backgroundColor = MaterialTheme.colors.feedbackSurface,
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(corner = CornerSize(8.dp))
            ) {
                Row {
                    Column(
                        modifier = Modifier
                            .weight(2f, true)
                            .padding(bottom = 8.dp)
                    ) {
                        val fontSize = 16.sp
                        Text(
                            text = title,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.onPrimary,
                            modifier = Modifier.padding(start = 18.dp, top = 16.dp)
                        )
                        Text(
                            text = message,
                            fontSize = fontSize,
                            color = MaterialTheme.colors.onPrimary.copy(alpha = .9f),
                            modifier = Modifier.padding(start = 18.dp, top = 8.dp)
                        )
                        WCTextButton(
                            onClick = { onAction() },
                            icon = Icons.Outlined.Reviews,
                            text = action,
                            colors = ButtonDefaults
                                .textButtonColors(contentColor = MaterialTheme.colors.onFeedbackSurface),
                            modifier = Modifier.padding(start = 4.dp)

                        )
                    }
                    IconButton(onClick = { onClose() }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            tint = MaterialTheme.colors.onPrimary.copy(alpha = .60f),
                            contentDescription = stringResource(id = R.string.close),
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FeedbackDialogPreview() {
    WooThemeWithBackground {
        FeedbackDialog(
            title = "Shipping added!",
            message = "Does Woo make shipping easy?",
            action = "Share your feedback",
            isShown = true,
            onAction = {},
            onClose = {}
        )
    }
}

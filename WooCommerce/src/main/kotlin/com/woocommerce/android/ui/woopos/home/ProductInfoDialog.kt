package com.woocommerce.android.ui.woopos.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme

@Composable
fun ProductInfoDialog(
    state: WooPosHomeState.ProductsInfoDialog,
    onDismissRequest: () -> Unit,
    onCreateOrderClick: () -> Unit
) {
    if (state.shouldDisplayDialog) {
        Dialog(
            onDismissRequest = onDismissRequest,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    elevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = onDismissRequest,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        tint = MaterialTheme.colors.onSurface,
                                        contentDescription = stringResource(
                                            id = R.string.woopos_banner_simple_products_close_content_description
                                        ),
                                    )
                                }
                            }
                            Text(
                                text = stringResource(id = state.header),
                                style = MaterialTheme.typography.h5,
                                fontWeight = FontWeight.Bold,
                                color = WooPosTheme.colors.onSurfaceHigh,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = stringResource(id = state.primaryMessage),
                                style = TextStyle(
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 24.sp
                                ),
                                color = WooPosTheme.colors.onSurfaceHigh,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Box(
                                Modifier
                                    .background(color = Color(0xF6F7F7).copy(alpha = 0.8f))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(id = state.secondaryMessage),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Normal,
                                        color = WooPosTheme.colors.onSurfaceHigh,
                                    )
                                    TextButton(
                                        onClick = onCreateOrderClick,
                                    ) {
                                        Text(
                                            text = stringResource(id = state.secondaryMessageActionLabel),
                                            color = MaterialTheme.colors.primary,
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onDismissRequest,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = stringResource(id = state.primaryButton.label))
                            }
                        }
                    }
                }
            }
        }
    }
}

@WooPosPreview
@Composable
fun ProductInfoDialogPreview() {
    WooPosTheme {
        ProductInfoDialog(
            state = WooPosHomeState.ProductsInfoDialog(
                shouldDisplayDialog = true,
                header = R.string.woopos_dialog_products_info_heading,
                primaryMessage = R.string.woopos_dialog_products_info_primary_message,
                secondaryMessage = R.string.woopos_dialog_products_info_secondary_message,
                secondaryMessageActionLabel = R.string.woopos_dialog_products_info_secondary_message_action_label,
                primaryButton = WooPosHomeState.ProductsInfoDialog.PrimaryButton(
                    label = R.string.woopos_dialog_products_info_button_label
                )
            ),
            onDismissRequest = {},
            onCreateOrderClick = {}
        )
    }
}

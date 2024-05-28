package com.woocommerce.android.ui.orders.creation.shipping

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import java.math.BigDecimal

@Composable
fun UpdateShippingScreen(
    viewModel: OrderShippingViewModel,
    modifier: Modifier = Modifier
) {
    val viewState by viewModel.viewState.collectAsState()
    when (val currentState = viewState) {
        is OrderShippingViewModel.ViewState.ShippingState -> {
            UpdateShippingScreen(
                name = currentState.name,
                amount = currentState.amount,
                method = currentState.method?.title,
                isEditFlow = currentState.isEditFlow,
                isSaveChangesEnabled = currentState.isSaveChangesEnabled,
                onNameChanged = { name -> viewModel.onNameChanged(name) },
                onAmountChanged = { amount -> viewModel.onAmountChanged(amount) },
                onSelectMethod = { viewModel.onSelectMethod() },
                onRemove = { viewModel.onRemove() },
                onSaveChanges = { viewModel.onSaveChanges() },
                modifier = modifier
            )
        }

        is OrderShippingViewModel.ViewState.Loading -> {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
fun UpdateShippingScreen(
    name: String?,
    onNameChanged: (String) -> Unit,
    amount: BigDecimal,
    onAmountChanged: (BigDecimal) -> Unit,
    method: String?,
    onSelectMethod: () -> Unit,
    isSaveChangesEnabled: Boolean,
    onSaveChanges: () -> Unit,
    isEditFlow: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState())
                .padding(bottom = 200.dp)
        ) {
            FieldCaption(
                text = stringResource(id = R.string.order_creation_add_shipping_method),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            FieldSelectValue(
                text = method,
                hint = stringResource(id = R.string.na),
                onSelect = onSelectMethod,
                modifier = Modifier.fillMaxWidth()
            )
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            Box {
                FieldCaption(
                    text = stringResource(id = R.string.order_creation_add_shipping_amount),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                AmountBigDecimalTextField(
                    value = amount,
                    onValueChange = { amount -> onAmountChanged(amount) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                )
            }
            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            FieldCaption(
                text = stringResource(id = R.string.order_creation_add_shipping_name),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            FieldEditValue(
                name.orEmpty(),
                { name -> onNameChanged(name) },
                Modifier.fillMaxWidth()
            )
        }
        Column(
            modifier = Modifier
                .background(color = MaterialTheme.colors.surface)
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            WCColoredButton(
                enabled = isSaveChangesEnabled,
                onClick = { onSaveChanges() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.order_creation_shipping_add))
            }
            if (isEditFlow) {
                WCOutlinedButton(
                    onClick = { onRemove() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.order_creation_remove_shipping))
                }
            }
        }
    }
}

@Composable
fun FieldCaption(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(text = text, fontSize = 16.sp, modifier = modifier.padding(top = 16.dp))
}

@Composable
fun FieldSelectValue(
    text: String?,
    hint: String?,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val display = text ?: hint.orEmpty()
    val alpha = if (text == null) 0.38f else 1f
    Box(
        modifier = modifier
            .padding(8.dp)
            .clickable { onSelect() }
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = display,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .alpha(alpha)
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.major_250))
                .align(Alignment.CenterEnd)
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FieldEditValue(
    text: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    BasicTextField(
        value = text,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = MaterialTheme.colors.onSurface
        ),
        cursorBrush = SolidColor(colors.cursorColor(false).value),
        modifier = modifier,
        decorationBox = @Composable { innerTextField ->
            // places leading icon, text field with label and placeholder, trailing icon
            TextFieldDefaults.TextFieldDecorationBox(
                value = text,
                innerTextField = innerTextField,
                interactionSource = interactionSource,
                placeholder = @Composable {
                    Text(
                        text = stringResource(id = R.string.order_creation_add_shipping_name_hint),
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = .38f)
                    )
                },
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None
            )
        }
    )
}

@Composable
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.PIXEL_4)
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL_4)
fun UpdateShippingScreenPreview() {
    WooThemeWithBackground {
        UpdateShippingScreen(
            name = "Flat Rate",
            method = "Flat Rate",
            amount = "10.00".toBigDecimal(),
            isSaveChangesEnabled = true,
            isEditFlow = false,
            onAmountChanged = {},
            onNameChanged = {},
            onSelectMethod = {},
            onSaveChanges = {},
            onRemove = {}
        )
    }
}

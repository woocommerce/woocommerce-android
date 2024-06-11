package com.woocommerce.android.ui.orders.creation.shipping
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.textfield.TextInputLayout
import com.woocommerce.android.R
import com.woocommerce.android.extensions.filterNotNull
import com.woocommerce.android.widgets.WCMaterialOutlinedCurrencyEditTextView
import java.math.BigDecimal

@Composable
fun AmountBigDecimalTextField(
    value: BigDecimal,
    onValueChange: (BigDecimal) -> Unit,
    modifier: Modifier = Modifier
) {
    val owner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier,
        factory = { context ->
            // Creates view
            WCMaterialOutlinedCurrencyEditTextView(context).apply {
                // Sets up listeners for View -> Compose communication
                supportsEmptyState = false
                supportsNegativeValues = false
                this.value.filterNotNull().observe(owner) {
                    onValueChange(it)
                }
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_NONE
                val textSize = 28f
                editText.apply {
                    background = null
                    setTextAppearance(R.style.TextAppearance_Woo_EditText)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
                }
                prefixTextView.apply {
                    setTextAppearance(R.style.TextAppearance_Woo_EditText)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
                    setTextColor(context.getColor(R.color.color_on_surface_disabled))
                }
                suffixTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
            }
        },
        update = { view ->
            // View's been inflated or state read in this block has been updated
            // Add logic here if necessary

            // As selectedItem is read here, AndroidView will recompose
            // whenever the state changes
            // Example of Compose -> View communication
            view.setValueIfDifferent(value)
        }
    )
}

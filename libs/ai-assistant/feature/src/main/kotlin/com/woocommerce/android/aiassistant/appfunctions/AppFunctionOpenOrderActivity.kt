package com.woocommerce.android.aiassistant.appfunctions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.appfunctions.AppFunctionOpenOrderState.Open
import com.woocommerce.android.aiassistant.appfunctions.AppFunctionOpenOrderState.WrongStore
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

internal sealed interface AppFunctionOpenOrderState {
    data class Open(val orderId: Long) : AppFunctionOpenOrderState
    data class WrongStore(val storeName: String) : AppFunctionOpenOrderState
    data object Unavailable : AppFunctionOpenOrderState
}

@HiltViewModel
internal class AppFunctionOpenOrderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    storeAccess: AppFunctionsStoreAccess,
) : ViewModel() {
    val state: AppFunctionOpenOrderState = run {
        val orderId: Long? = savedStateHandle[EXTRA_ORDER_ID]
        val store = savedStateHandle.requestedStore()
        val storeName: String = savedStateHandle[EXTRA_STORE_NAME] ?: ""
        if (orderId == null || store == null) {
            AppFunctionOpenOrderState.Unavailable
        } else {
            when (storeAccess.check(store)) {
                StoreCheck.SAME_STORE -> Open(orderId)
                StoreCheck.DIFFERENT_STORE -> WrongStore(storeName)
                StoreCheck.NO_ACCESS -> AppFunctionOpenOrderState.Unavailable
            }
        }
    }
}

@AndroidEntryPoint
internal class AppFunctionOpenOrderActivity : ComponentActivity() {
    @Inject
    lateinit var hostNavigation: AppFunctionsHostNavigation

    private val viewModel: AppFunctionOpenOrderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = viewModel.state
        if (state is Open) {
            startActivity(hostNavigation.orderDetailsIntent(state.orderId))
            finish()
        } else {
            setContent {
                AppFunctionsTheme {
                    AppFunctionMessageDialog(message = state.message(), onDismiss = ::finish)
                }
            }
        }
    }
}

@Composable
private fun AppFunctionOpenOrderState.message(): String = when (this) {
    is WrongStore -> stringResource(R.string.ai_assistant_app_functions_open_order_wrong_store, storeName)
    AppFunctionOpenOrderState.Unavailable -> stringResource(R.string.ai_assistant_app_functions_open_order_unavailable)
    is Open -> ""
}

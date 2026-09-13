package com.woocommerce.android.ui.dashboard

import android.os.Bundle
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.rememberFragmentState
import com.woocommerce.android.ui.jitm.JitmFragment
import com.woocommerce.android.ui.jitm.JitmMessagePathsProvider
import com.woocommerce.android.ui.jitm.JitmViewModel

@Composable
internal fun DashboardJitmHost(
    onJitmFragmentChanged: (JitmFragment?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fragmentState = rememberFragmentState()
    val arguments = remember {
        Bundle().apply {
            putString(JitmViewModel.JITM_MESSAGE_PATH_KEY, JitmMessagePathsProvider.MY_STORE)
        }
    }
    val currentOnJitmFragmentChanged = rememberUpdatedState(onJitmFragmentChanged)

    DisposableEffect(Unit) {
        onDispose { currentOnJitmFragmentChanged.value(null) }
    }
    // Fragment Compose creates a MATCH_PARENT-height host. Intrinsics make it measure its content height.
    AndroidFragment<JitmFragment>(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        fragmentState = fragmentState,
        arguments = arguments,
        onUpdate = { currentOnJitmFragmentChanged.value(it) },
    )
}

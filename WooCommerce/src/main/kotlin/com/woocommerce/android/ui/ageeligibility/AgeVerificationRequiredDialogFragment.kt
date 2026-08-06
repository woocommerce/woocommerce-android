package com.woocommerce.android.ui.ageeligibility

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.compose.preview.FontScalePreviews
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AgeVerificationRequiredDialogFragment : DialogFragment() {
    @Inject
    lateinit var playStoreLauncher: AgeVerificationPlayStoreLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        setStyle(STYLE_NO_TITLE, R.style.Theme_Woo_Dialog_RoundedCorners_NoMinWidth)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeView {
        WooThemeWithBackground {
            AgeVerificationRequiredScreen(
                onOpenPlayStore = {
                    if (playStoreLauncher.open(requireContext())) {
                        listener.onAgeVerificationPlayStoreOpened()
                    }
                },
                onRetry = listener::onAgeVerificationRetryRequested
            )
        }
    }

    private val listener: Listener
        get() = checkNotNull(requireActivity() as? Listener) {
            "Host activity must implement AgeVerificationRequiredDialogFragment.Listener"
        }

    interface Listener {
        fun onAgeVerificationPlayStoreOpened()

        fun onAgeVerificationRetryRequested()
    }

    companion object {
        const val TAG = "AgeVerificationRequiredDialogFragment"
    }
}

class AgeVerificationPlayStoreLauncher @Inject constructor() {
    fun open(context: Context): Boolean {
        val packageName = context.packageName
        return try {
            context.startActivity(playStoreIntent("market://details?id=$packageName"))
            true
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(playStoreIntent("https://play.google.com/store/apps/details?id=$packageName"))
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }
    }

    private fun playStoreIntent(url: String) = Intent(Intent.ACTION_VIEW, Uri.parse(url))
}

fun FragmentActivity.showAgeVerificationRequiredDialog() {
    if (supportFragmentManager.findFragmentByTag(AgeVerificationRequiredDialogFragment.TAG) == null &&
        supportFragmentManager.isStateSaved.not()
    ) {
        AgeVerificationRequiredDialogFragment().show(
            supportFragmentManager,
            AgeVerificationRequiredDialogFragment.TAG
        )
    }
}

fun FragmentActivity.dismissAgeVerificationRequiredDialog() {
    val dialog = supportFragmentManager.findFragmentByTag(
        AgeVerificationRequiredDialogFragment.TAG
    ) as? AgeVerificationRequiredDialogFragment
    if (dialog != null && supportFragmentManager.isStateSaved.not()) {
        dialog.dismiss()
    }
}

@Composable
private fun AgeVerificationRequiredScreen(
    onOpenPlayStore: () -> Unit,
    onRetry: () -> Unit
) {
    Surface(color = MaterialTheme.colors.surface) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(id = R.dimen.major_100)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
        ) {
            Text(
                text = stringResource(id = R.string.age_verification_required_title),
                style = MaterialTheme.typography.h6
            )
            Text(
                text = stringResource(id = R.string.age_verification_required_message),
                style = MaterialTheme.typography.body1
            )
            WCColoredButton(
                onClick = onOpenPlayStore,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.age_verification_open_google_play))
            }
            WCOutlinedButton(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.retry))
            }
        }
    }
}

@LightDarkThemePreviews
@FontScalePreviews
@Composable
private fun AgeVerificationRequiredScreenPreview() {
    WooThemeWithBackground {
        AgeVerificationRequiredScreen(
            onOpenPlayStore = {},
            onRetry = {}
        )
    }
}

package com.woocommerce.android.ui.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.woocommerce.android.ciab.CIABAffectedFeature
import com.woocommerce.android.ciab.CIABSiteGateKeeper
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

class AppShortcutsHandler @Inject constructor(
    private val context: Context,
    private val selectedSite: SelectedSite,
    private val ciabSiteGateKeeper: CIABSiteGateKeeper,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
) {
    fun init() {
        appCoroutineScope.launch {
            selectedSite.observe()
                .distinctUntilChanged()
                .collect { site ->
                    if (site != null) {
                        ShortcutManagerCompat.setDynamicShortcuts(context, buildShortcutsList())
                    } else {
                        ShortcutManagerCompat.removeAllDynamicShortcuts(context)
                    }
                }
        }
    }

    private fun buildShortcutsList(): List<ShortcutInfoCompat> {
        return buildList {
            if (ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.InPersonPayments)) {
                add(AppShortcut.Payments.toShortcutInfo())
            }

            add(AppShortcut.CreateOrder.toShortcutInfo())
        }
    }

    private fun AppShortcut.toShortcutInfo(): ShortcutInfoCompat {
        return ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(context.getString(label))
            .setLongLabel(context.getString(label))
            .setIcon(IconCompat.createWithResource(context, icon))
            .setIntent(
                Intent(context, MainActivity::class.java).apply {
                    action = this@toShortcutInfo.action
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
            .build()
    }
}

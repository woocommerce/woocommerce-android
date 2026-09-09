package com.woocommerce.android.ui.ageeligibility

import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import com.woocommerce.android.support.help.HelpActivity
import com.woocommerce.android.support.help.HelpOrigin
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AgeRestrictionSupportLauncherTest {
    @Test
    fun `when open is called, then Help and Support is started with the age restriction origin`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()

        AgeRestrictionSupportLauncher().open(activity)

        val startedIntent = shadowOf(activity).nextStartedActivity
        assertThat(startedIntent.component?.className).isEqualTo(HelpActivity::class.java.name)
        assertThat(IntentCompat.getSerializableExtra(startedIntent, HelpActivity.ORIGIN_KEY, HelpOrigin::class.java))
            .isEqualTo(HelpOrigin.AGE_RESTRICTION)
    }
}

package com.woocommerce.android.ui.woopos.roles.lock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosLockScreenLayoutTest {
    private val tablet = WooPosLockScreenLayout.Metrics(
        horizontalPadding = 40.dp,
        verticalPadding = 40.dp,
        reducedVerticalPadding = 16.dp,
        sectionSpacing = 32.dp,
        keySpacing = 8.dp,
        illustrationSize = 88.dp,
    )
    private val phone = WooPosLockScreenLayout.Metrics(
        horizontalPadding = 20.dp,
        verticalPadding = 20.dp,
        reducedVerticalPadding = 8.dp,
        sectionSpacing = 16.dp,
        keySpacing = 4.dp,
        illustrationSize = 88.dp,
    )

    @Test
    fun `given a tall tablet, when laid out, then it keeps the illustration and design key size`() {
        // WHEN
        val layout = calculate(DpSize(800.dp, 1200.dp), tablet, preferredKeySize = 90.dp)

        // THEN
        assertThat(layout).isEqualTo(
            WooPosLockScreenLayout(90.dp, 40.dp, showsIllustration = true, statusHeight = 48.dp)
        )
    }

    @Test
    fun `given slightly short height, when laid out, then it reduces padding before hiding the illustration`() {
        // WHEN
        val layout = calculate(DpSize(600.dp, 700.dp), tablet, preferredKeySize = 90.dp)

        // THEN
        assertThat(layout).isEqualTo(
            WooPosLockScreenLayout(90.dp, 16.dp, showsIllustration = true, statusHeight = 48.dp)
        )
    }

    @Test
    fun `given large text in a short window, when laid out, then it hides the illustration and keeps full keys`() {
        // WHEN
        val layout = calculate(
            DpSize(400.dp, 600.dp),
            phone,
            preferredKeySize = 72.dp,
            titleHeight = 128.dp,
            statusHeight = 96.dp,
        )

        // THEN
        assertThat(layout).isEqualTo(
            WooPosLockScreenLayout(72.dp, 8.dp, showsIllustration = false, statusHeight = 96.dp)
        )
    }

    @Test
    fun `given a landscape phone, when laid out in one column, then it hides the illustration and fits the keys`() {
        // WHEN
        val compactPhone = phone.copy(verticalPadding = 4.dp, reducedVerticalPadding = 4.dp, sectionSpacing = 8.dp)
        val layout = calculate(DpSize(800.dp, 360.dp), compactPhone, preferredKeySize = 72.dp, isCompactHeight = true)

        // THEN
        assertThat(layout.keySize).isEqualTo(61.dp)
        assertThat(layout.showsIllustration).isFalse()
        val contentHeight = 32.dp + 48.dp + compactPhone.sectionSpacing * 2 +
            layout.keySize * 4 + compactPhone.keySpacing * 3 + layout.verticalPadding * 2
        assertThat(contentHeight.value).isLessThanOrEqualTo(360f)
    }

    @Test
    fun `given a tiny window, when laid out, then keys stop at the minimum size and content scrolls`() {
        // WHEN
        val layout = calculate(DpSize(320.dp, 320.dp), phone, preferredKeySize = 72.dp)

        // THEN
        assertThat(layout.keySize).isEqualTo(60.dp)
        assertThat(layout.showsIllustration).isFalse()
    }

    @Suppress("LongParameterList")
    private fun calculate(
        viewport: DpSize,
        metrics: WooPosLockScreenLayout.Metrics,
        preferredKeySize: Dp,
        titleHeight: Dp = 32.dp,
        statusHeight: Dp = 48.dp,
        isCompactHeight: Boolean = false,
    ) = WooPosLockScreenLayout.calculate(
        viewport,
        preferredKeySize,
        titleHeight,
        statusHeight,
        isCompactHeight,
        metrics,
    )
}

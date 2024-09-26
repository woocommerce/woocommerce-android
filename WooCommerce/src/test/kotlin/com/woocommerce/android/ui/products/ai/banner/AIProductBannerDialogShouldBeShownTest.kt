import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ai.banner.AIProductBannerDialogShouldBeShown
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

class AIProductBannerDialogShouldBeShownTest {
    private val selectedSite: SelectedSite = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private lateinit var aiProductBannerDialogShouldBeShown: AIProductBannerDialogShouldBeShown

    @Before
    fun setup() {
        aiProductBannerDialogShouldBeShown = AIProductBannerDialogShouldBeShown(
            selectedSite,
            appPrefsWrapper
        )
    }

    @Test
    fun `given site is WPComAtomic, when AI promo dialog was not shown, then should return true`() {
        // given
        val site = mock<SiteModel>()
        whenever(site.isWPComAtomic).thenReturn(true)
        whenever(site.planActiveFeatures).thenReturn("")
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(appPrefsWrapper.wasAIProductDescriptionPromoDialogShown).thenReturn(false)

        // when
        val result = aiProductBannerDialogShouldBeShown()

        // then
        assertThat(result).isTrue
    }

    @Test
    fun `given site has AI assistant in plan features, when AI promo dialog was not shown, then should return true`() {
        // given
        val site = mock<SiteModel>()
        whenever(site.isWPComAtomic).thenReturn(false)
        whenever(site.planActiveFeatures).thenReturn("ai-assistant")
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(appPrefsWrapper.wasAIProductDescriptionPromoDialogShown).thenReturn(false)

        // when
        val result = aiProductBannerDialogShouldBeShown()

        // then
        assertThat(result).isTrue
    }

    @Test
    fun `given site is not eligible for AI, when checking if dialog should be shown, then should return false`() {
        // given
        val site = mock<SiteModel>()
        whenever(site.isWPComAtomic).thenReturn(false)
        whenever(site.planActiveFeatures).thenReturn("")
        whenever(selectedSite.getOrNull()).thenReturn(site)

        // when
        val result = aiProductBannerDialogShouldBeShown()

        // then
        assertThat(result).isFalse
    }

    @Test
    fun `given AI promo dialog was already shown, when site is eligible for AI, then should return false`() {
        // given
        val site = mock<SiteModel>()
        whenever(site.isWPComAtomic).thenReturn(true)
        whenever(site.planActiveFeatures).thenReturn("")
        whenever(selectedSite.getOrNull()).thenReturn(site)
        whenever(appPrefsWrapper.wasAIProductDescriptionPromoDialogShown).thenReturn(true)

        // when
        val result = aiProductBannerDialogShouldBeShown()

        // then
        assertThat(result).isFalse
    }

    @Test
    fun `given no site is selected, when checking if dialog should be shown, then should return false`() {
        // given
        whenever(selectedSite.getOrNull()).thenReturn(null)

        // when
        val result = aiProductBannerDialogShouldBeShown()

        // then
        assertThat(result).isFalse
    }
}

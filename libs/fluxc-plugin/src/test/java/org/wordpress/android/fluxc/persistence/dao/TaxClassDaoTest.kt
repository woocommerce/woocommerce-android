package org.wordpress.android.fluxc.persistence.dao

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.taxes.WCTaxClassModel
import org.wordpress.android.fluxc.persistence.DatabaseTestRule

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class TaxClassDaoTest {

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    private lateinit var sut: TaxClassDao

    private companion object {
        val site = SiteModel().apply {
            email = "test@example.org"
            name = "Test Site"
            id = 24
        }

        val sampleTaxClass1 = WCTaxClassModel(
            localSiteId = site.localId(), name = "Standard", slug = "standard"
        )

        val sampleTaxClass2 = WCTaxClassModel(
            localSiteId = site.localId(), name = "Reduced Rate", slug = "reduced-rate"
        )

        val differentSiteTaxClass = WCTaxClassModel(
            localSiteId = LocalId(999), name = "Standard", slug = "standard"
        )
    }

    @Before
    fun setUp() {
        sut = databaseRule.db.taxClassDao
    }

    @Test
    fun `getTaxClasses returns empty list when no tax classes are stored`() = runTest {
        // when
        val result = sut.getTaxClasses(site.localId())

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `getTaxClasses returns tax classes for the given site`() = runTest {
        // given
        val taxClasses = listOf(sampleTaxClass1, sampleTaxClass2)
        sut.replaceAll(site.localId(), taxClasses)

        // when
        val result = sut.getTaxClasses(site.localId())

        // then
        assertThat(result).containsExactlyInAnyOrderElementsOf(taxClasses)
    }

    @Test
    fun `getTaxClasses does not return tax classes for different site`() = runTest {
        // given
        sut.replaceAll(LocalId(999), listOf(differentSiteTaxClass))

        // when
        val result = sut.getTaxClasses(site.localId())

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `replaceAll replaces existing tax classes`() = runTest {
        // given
        sut.replaceAll(site.localId(), listOf(sampleTaxClass1))

        // when
        val newTaxClasses = listOf(sampleTaxClass2)
        sut.replaceAll(site.localId(), newTaxClasses)

        // then
        val result = sut.getTaxClasses(site.localId())
        assertThat(result).containsExactlyInAnyOrderElementsOf(newTaxClasses)
    }

    @Test
    fun `replaceAll only affects tax classes for the given site`() = runTest {
        // given
        sut.replaceAll(LocalId(999), listOf(differentSiteTaxClass))
        sut.replaceAll(site.localId(), listOf(sampleTaxClass1))

        // when
        val newTaxClasses = listOf(sampleTaxClass2)
        sut.replaceAll(site.localId(), newTaxClasses)

        // then
        val result = sut.getTaxClasses(site.localId())
        assertThat(result).containsExactlyInAnyOrderElementsOf(newTaxClasses)

        val otherSiteResult = sut.getTaxClasses(LocalId(999))
        assertThat(otherSiteResult).containsExactly(differentSiteTaxClass)
    }

    @Test
    fun `when there are duplicate tax classes, replaceAll keeps the last one`() = runTest {
        // given
        val taxClasses = listOf(sampleTaxClass1, sampleTaxClass2, sampleTaxClass1)
        sut.replaceAll(site.localId(), taxClasses)

        // when
        val result = sut.getTaxClasses(site.localId())

        // then
        assertThat(result).containsExactlyInAnyOrderElementsOf(listOf(sampleTaxClass1, sampleTaxClass2))
    }
}

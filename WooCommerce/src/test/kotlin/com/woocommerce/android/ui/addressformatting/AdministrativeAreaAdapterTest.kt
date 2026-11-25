package com.woocommerce.android.ui.addressformatting

import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AdministrativeAreaAdapterTest {
    @Test
    fun `given a Chinese state, when getting its presentation name, then the state name is returned`() {
        val state = AmbiguousLocation.Defined(Location(code = "CN24", name = "Sichuan / 四川", parentCode = "CN"))

        assertThat(state.presentationName("CN")).isEqualTo("Sichuan / 四川")
    }

    @Test
    fun `given a US state, when getting its presentation name, then the state code is returned`() {
        val state = AmbiguousLocation.Defined(Location(code = "CA", name = "California", parentCode = "US"))

        assertThat(state.presentationName("US")).isEqualTo("CA")
    }

    @Test
    fun `given an unresolved state, when getting its presentation name, then the raw code is returned`() {
        val state = AmbiguousLocation.Raw("CN24")

        assertThat(state.presentationName("CN")).isEqualTo("CN24")
    }
}

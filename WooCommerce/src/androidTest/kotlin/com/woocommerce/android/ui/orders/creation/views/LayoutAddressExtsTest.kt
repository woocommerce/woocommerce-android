package com.woocommerce.android.ui.orders.creation.views

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.woocommerce.android.R
import com.woocommerce.android.databinding.LayoutAddressFormBinding
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.AddressSelectionState
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.StateSpinnerStatus.HAVING_LOCATIONS
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LayoutAddressExtsTest {
    @Test
    @UiThreadTest
    fun givenSelectedRegionWhenCountryChangesThenRegionSpinnerIsEmpty() {
        // GIVEN
        val context = ContextThemeWrapper(
            ApplicationProvider.getApplicationContext<Context>(),
            R.style.Theme_Woo_DayNight
        )
        val binding = LayoutAddressFormBinding.inflate(LayoutInflater.from(context))
        binding.update(
            AddressSelectionState(
                address = Address.EMPTY.copy(
                    country = Location("US", "United States"),
                    state = AmbiguousLocation.Defined(Location("CA", "California", "US"))
                ),
                stateSpinnerStatus = HAVING_LOCATIONS
            )
        )
        assertThat(binding.stateSpinner.getText()).isEqualTo("California")

        // WHEN
        binding.update(
            AddressSelectionState(
                address = Address.EMPTY.copy(
                    country = Location("DE", "Germany"),
                    state = AmbiguousLocation.EMPTY
                ),
                stateSpinnerStatus = HAVING_LOCATIONS
            )
        )

        // THEN
        assertThat(binding.countrySpinner.getText()).isEqualTo("Germany")
        assertThat(binding.stateSpinner.isVisible).isTrue()
        assertThat(binding.stateSpinner.getText()).isEmpty()
    }
}

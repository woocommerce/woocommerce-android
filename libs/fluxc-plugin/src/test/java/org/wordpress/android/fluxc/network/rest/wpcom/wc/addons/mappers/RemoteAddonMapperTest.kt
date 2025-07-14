package org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.mappers

import org.junit.Test
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto

class RemoteAddonMapperTest {
    val sut = RemoteAddonMapper

    @Test
    fun `should not throw exception for a valid dto`() {
        sut.toDomain(VALID_ADDON_DTO)
    }

    @Test(expected = MappingRemoteException::class)
    fun `should throw exception when type of addon dto is null`() {
        sut.toDomain(
            VALID_ADDON_DTO.copy(type = null)
        )
    }

    @Test(expected = MappingRemoteException::class)
    fun `should throw exception when title format of addon dto is null`() {
        sut.toDomain(
            VALID_ADDON_DTO.copy(titleFormat = null)
        )
    }

    private companion object {
        val VALID_ADDON_DTO = RemoteAddonDto(
            titleFormat = RemoteAddonDto.RemoteTitleFormat.Hide,
            descriptionEnabled = 1,
            restrictionsType = null,
            adjustPrice = 0,
            priceType = null,
            type = RemoteAddonDto.RemoteType.MultipleChoice,
            display = RemoteAddonDto.RemoteDisplay.RadioButton,
            name = "Add-on",
            description = "This is a test add-on",
            required = 1,
            position = 0,
            price = null,
            min = 0,
            max = 0,
            options = listOf(
                RemoteAddonDto.RemoteOption(
                    priceType = RemoteAddonDto.RemotePriceType.FlatFee,
                    label = "First checkbox option",
                    price = "10"
                ),
                RemoteAddonDto.RemoteOption(
                    priceType = RemoteAddonDto.RemotePriceType.FlatFee,
                    label = "Second checkbox option",
                    price = "20"
                )
            )
        )
    }
}

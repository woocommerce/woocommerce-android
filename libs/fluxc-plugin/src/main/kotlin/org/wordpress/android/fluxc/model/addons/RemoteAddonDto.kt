package org.wordpress.android.fluxc.model.addons

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.metadata.WCMetaDataValue

data class RemoteAddonDto(
    @SerializedName("title_format")
    val titleFormat: RemoteTitleFormat?,
    @SerializedName("description_enable")
    val descriptionEnabled: Int,
    @SerializedName("restrictions_type")
    val restrictionsType: RemoteRestrictionsType? = null,
    @SerializedName("adjust_price")
    val adjustPrice: Int,
    @SerializedName("price_type")
    val priceType: RemotePriceType? = null,

    val type: RemoteType?,
    val display: RemoteDisplay? = null,
    val name: String,
    val description: String,
    val required: Int,
    val position: Int,
    val price: String? = null,
    val min: Long,
    val max: Long,
    val options: List<RemoteOption>? = null
) {
    enum class RemoteType {
        @SerializedName("multiple_choice") MultipleChoice,
        @SerializedName("checkbox") Checkbox,
        @SerializedName("custom_text") CustomText,
        @SerializedName("custom_textarea") CustomTextArea,
        @SerializedName("file_upload") FileUpload,
        @SerializedName("custom_price") CustomPrice,
        @SerializedName("input_multiplier") InputMultiplier,
        @SerializedName("heading") Heading
    }

    enum class RemoteDisplay {
        @SerializedName("select") Select,
        @SerializedName("radiobutton") RadioButton,
        @SerializedName("images") Images
    }

    enum class RemoteTitleFormat {
        @SerializedName("label") Label,
        @SerializedName("heading") Heading,
        @SerializedName("hide") Hide
    }

    enum class RemoteRestrictionsType {
        @SerializedName("any_text") AnyText,
        @SerializedName("only_letters") OnlyLetters,
        @SerializedName("only_numbers") OnlyNumbers,
        @SerializedName("only_letters_numbers") OnlyLettersNumbers,
        @SerializedName("email") Email
    }

    enum class RemotePriceType {
        @SerializedName("flat_fee") FlatFee,
        @SerializedName("quantity_based") QuantityBased,
        @SerializedName("percentage_based") PercentageBased
    }

    data class RemoteOption(
        @SerializedName("price_type")
        val priceType: RemotePriceType,

        val label: String? = null,
        val price: String? = null,
        val image: String? = null
    )

    companion object {
        @Suppress("SwallowedException", "TooGenericExceptionCaught")
        fun fromMetaDataValue(value: WCMetaDataValue): List<RemoteAddonDto>? {
            return try {
                Gson().run {
                    fromJson(value.jsonValue, Array<RemoteAddonDto>::class.java)
                }.toList()
            } catch (ex: Exception) {
                null
            }
        }
    }
}

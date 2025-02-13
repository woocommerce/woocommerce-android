package org.wordpress.android.fluxc.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import androidx.room.Relation
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

data class AddonWithOptions(
    @Embedded val addon: AddonEntity,
    @Relation(
            parentColumn = "addonLocalId",
            entityColumn = "addonLocalId"
    )
    val options: List<AddonOptionEntity>
)

@Entity(
        foreignKeys = [ForeignKey(
                entity = GlobalAddonGroupEntity::class,
                parentColumns = ["globalGroupLocalId"],
                childColumns = ["globalGroupLocalId"],
                onDelete = CASCADE
        )]
)
data class AddonEntity(
    @PrimaryKey(autoGenerate = true) val addonLocalId: Long = 0,
    @ColumnInfo(index = true) val globalGroupLocalId: Long? = null,
    val productRemoteId: RemoteId? = null,
    val localSiteId: LocalId? = null,
    val type: LocalType,
    val display: LocalDisplay? = null,
    val name: String,
    val titleFormat: LocalTitleFormat,
    val description: String?,
    val required: Boolean,
    val position: Int,
    val restrictions: LocalRestrictions? = null,
    val priceType: LocalPriceType? = null,
    val price: String? = null,
    val min: Long? = null,
    val max: Long? = null
) {
    enum class LocalType {
        MultipleChoice,
        Checkbox,
        CustomText,
        CustomTextArea,
        FileUpload,
        CustomPrice,
        InputMultiplier,
        Heading
    }

    enum class LocalDisplay {
        Select,
        RadioButton,
        Images
    }

    enum class LocalTitleFormat {
        Label,
        Heading,
        Hide
    }

    enum class LocalRestrictions {
        AnyText,
        OnlyLetters,
        OnlyNumbers,
        OnlyLettersNumbers,
        Email
    }

    enum class LocalPriceType {
        FlatFee,
        QuantityBased,
        PercentageBased
    }
}

package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.RawConstraints
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
@RawConstraints(
        "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE",
        "UNIQUE (REMOTE_SHIPPING_CLASS_ID, LOCAL_SITE_ID) ON CONFLICT REPLACE"
)
class WCProductShippingClassModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var remoteShippingClassId = 0L // The unique identifier for this shipping class on the server
    @Column var name = ""
    @Column var slug = ""
    @Column var description = ""

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }
}

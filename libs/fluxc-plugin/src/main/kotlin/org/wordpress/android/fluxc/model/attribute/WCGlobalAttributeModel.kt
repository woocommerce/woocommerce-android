package org.wordpress.android.fluxc.model.attribute

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.attribute.terms.WCAttributeTermModel
import org.wordpress.android.fluxc.persistence.WCGlobalAttributeSqlUtils.getTerm
import org.wordpress.android.fluxc.persistence.WellSqlConfig

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCGlobalAttributeModel(
    @PrimaryKey @Column private var id: Int = 0,
    @Column var localSiteId: Int = 0,
    @Column var name: String = "",
    @Column var slug: String = "",
    @Column var type: String = "",
    @Column var orderBy: String = "",
    @Column var hasArchives: Boolean = false,
    @Column var termsId: String = "",
    @Column var remoteId: Int = 0
) : Identifiable {
    override fun setId(id: Int) {
        this.id = id
    }

    override fun getId() = id

    val terms by lazy {
        termsId.split(";")
                .mapNotNull { it.toIntOrNull() }
                .takeIf { it.isNotEmpty() }
                ?.map { getTerm(localSiteId, it) }
    }

    fun asProductAttributeModel(vararg includedTermId: Int) =
            WCProductModel.ProductAttribute(
                    id = remoteId.toLong(),
                    name = name,
                    visible = true,
                    variation = true,
                    options = includedTermId.takeIf { it.isNotEmpty() }
                            ?.let { terms?.filterFromIdList(it) }
                            ?.map { it.name }
                            ?.toMutableList()
                            ?: mutableListOf()
            )

    fun asProductAttributeModel(includedTerms: List<String>) =
            WCProductModel.ProductAttribute(
                    id = remoteId.toLong(),
                    name = name,
                    visible = true,
                    variation = true,
                    options = includedTerms.takeIf { it.isNotEmpty() }
                            ?.let { terms?.filterFromTermNameList(it) }
                            ?.map { it.name }
                            ?.toMutableList()
                            ?: mutableListOf()
            )

    private fun List<WCAttributeTermModel?>.filterFromIdList(ids: IntArray) =
                    filterNotNull().filter { term ->
                        ids.contains(term.remoteId)
                    }

    private fun List<WCAttributeTermModel?>.filterFromTermNameList(termNames: List<String>) =
                    filterNotNull().filter { term ->
                        termNames.contains(term.name)
                    }
}

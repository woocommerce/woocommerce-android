package org.wordpress.android.fluxc.model.metadata

data class UpdateMetadataRequest(
    val parentItemId: Long,
    val parentItemType: MetaDataParentItemType,
    val metadataChanges: MetadataChanges,
) {
    val insertedMetadata: List<WCMetaData> get() = metadataChanges.insertedMetadata
    val updatedMetadata: List<WCMetaData> get() = metadataChanges.updatedMetadata
    val deletedMetadataIds: List<Long> get() = metadataChanges.deletedMetadataIds

    constructor(
        parentItemId: Long,
        parentItemType: MetaDataParentItemType,
        insertedMetadata: List<WCMetaData> = emptyList(),
        updatedMetadata: List<WCMetaData> = emptyList(),
        deletedMetadataIds: List<Long> = emptyList(),
    ) : this(
        parentItemId,
        parentItemType,
        MetadataChanges(
            insertedMetadata,
            updatedMetadata,
            deletedMetadataIds,
        )
    )
}


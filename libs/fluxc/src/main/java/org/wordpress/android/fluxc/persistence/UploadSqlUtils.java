package org.wordpress.android.fluxc.persistence;

import android.content.ContentValues;

import androidx.annotation.Nullable;

import com.wellsql.generated.MediaModelTable;
import com.wellsql.generated.MediaUploadModelTable;
import com.yarolegovich.wellsql.WellCursor;
import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.mapper.InsertMapper;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.MediaUploadModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UploadSqlUtils {
    public static int insertOrUpdateMedia(MediaUploadModel media) {
        if (media == null) return 0;

        List<MediaUploadModel> existingMedia;
        existingMedia = WellSql.select(MediaUploadModel.class)
                .where()
                .equals(MediaUploadModelTable.ID, media.getId())
                .endWhere().getAsModel();

        if (existingMedia.isEmpty()) {
            // Insert, media item does not exist
            WellSql.insert(media).asSingleTransaction(true).execute();
            return 1;
        } else {
            // Update, media item already exists
            int oldId = existingMedia.get(0).getId();
            return WellSql.update(MediaUploadModel.class).whereId(oldId)
                    .put(media, new UpdateAllExceptId<>(MediaUploadModel.class)).execute();
        }
    }

    public static int updateMediaProgressOnly(MediaUploadModel media) {
        if (media == null) return 0;

        List<MediaUploadModel> existingMedia;
        existingMedia = WellSql.select(MediaUploadModel.class)
                .where()
                .equals(MediaUploadModelTable.ID, media.getId())
                .endWhere().getAsModel();

        if (existingMedia.isEmpty()) {
            // We're only interested in updating the progress for existing MediaUploadModels
            return 0;
        } else {
            // Update, media item already exists
            int oldId = existingMedia.get(0).getId();
            return WellSql.update(MediaUploadModel.class).whereId(oldId)
                    .put(media, new InsertMapper<MediaUploadModel>() {
                        @Override
                        public ContentValues toCv(MediaUploadModel item) {
                            ContentValues cv = new ContentValues();
                            cv.put(MediaUploadModelTable.PROGRESS, item.getProgress());
                            return cv;
                        }
                    }).execute();
        }
    }

    public static @Nullable MediaUploadModel getMediaUploadModelForLocalId(int localMediaId) {
        List<MediaUploadModel> result = WellSql.select(MediaUploadModel.class).where()
                .equals(MediaUploadModelTable.ID, localMediaId)
                .endWhere()
                .getAsModel();
        if (result.isEmpty()) {
            return null;
        } else {
            return result.get(0);
        }
    }

    public static Set<MediaUploadModel> getMediaUploadModelsForPostId(int localPostId) {
        WellCursor<MediaModel> mediaModelCursor = WellSql.select(MediaModel.class)
                .columns(MediaModelTable.ID)
                .where().beginGroup()
                .equals(MediaModelTable.LOCAL_POST_ID, localPostId)
                .endGroup().endWhere()
                .getAsCursor();

        Set<MediaUploadModel> mediaUploadModels = new HashSet<>();
        while (mediaModelCursor.moveToNext()) {
            MediaUploadModel mediaUploadModel = getMediaUploadModelForLocalId(mediaModelCursor.getInt(0));
            if (mediaUploadModel != null) {
                mediaUploadModels.add(mediaUploadModel);
            }
        }
        mediaModelCursor.close();

        return mediaUploadModels;
    }

    public static int deleteMediaUploadModelWithLocalId(int localMediaId) {
        return WellSql.delete(MediaUploadModel.class)
                .where()
                .equals(MediaUploadModelTable.ID, localMediaId)
                .endWhere()
                .execute();
    }

    public static int deleteMediaUploadModelsWithLocalIds(Set<Integer> localMediaIds) {
        if (localMediaIds.size() > 0) {
            return WellSql.delete(MediaUploadModel.class)
                    .where()
                    .isIn(MediaUploadModelTable.ID, localMediaIds)
                    .endWhere()
                    .execute();
        }
        return 0;
    }
}

package org.wordpress.android.fluxc.media;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.utils.MediaUtils;

public class MediaTestUtils {
    public static MediaModel generateMediaFromPath(int localSiteId, long mediaId, String filePath) {
        return generateMediaFromPath(localSiteId, mediaId, filePath, null, null, null);
    }

    //Temporary hack
    private static int sNextId = 1;

    public static MediaModel generateMediaFromPath(int localSiteId, long mediaId, String filePath,
                                                   String title, String description, String caption) {
        MediaModel media = new MediaModel(localSiteId, mediaId);
        media.setId(sNextId++);  // Assign unique ID for cache
        media.setFilePath(filePath);
        media.setFileName(MediaUtils.getFileName(filePath));
        String extension = MediaUtils.getExtension(filePath);
        media.setMimeType(MediaUtils.getMimeTypeForExtension(extension));
        media.setTitle(title != null ? title : media.getFileName());
        if (description != null) {
            media.setDescription(description);
        }
        if (caption != null) {
            media.setCaption(caption);
        }
        return media;
    }
}

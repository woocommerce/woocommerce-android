package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload;

@ActionEnum
public enum UploadAction implements IAction {
    // Remote responses
    @Action(payloadType = ProgressPayload.class)
    UPLOADED_MEDIA, // Proxy for MediaAction.UPLOADED_MEDIA
}

package org.wordpress.android.fluxc.model

enum class AvatarSize(val size: Int) {
    SMALL(24), MEDIUM(48), LARGE(96);

    companion object {
        fun getAvatarSizeForValue(size: Int): AvatarSize {
            return when (size) {
                SMALL.size -> SMALL
                MEDIUM.size -> MEDIUM
                LARGE.size -> LARGE
                else -> MEDIUM
            }
        }
    }
}

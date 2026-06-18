package com.undy.startrobot3.data.model

data class AnnouncementChain(
    val id: Long = 0,
    val sortOrder: Int = 0,
    val announcements: List<Announcement> = emptyList()
) {
    fun anchorLabel(): String {
        val anchor = announcements.firstOrNull { it.isAnchor } ?: return "No anchor"
        return "${anchor.anchorOffsetSeconds}s from start"
    }
}

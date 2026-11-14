package com.professorx.launcher.launcher.workspace

import java.util.UUID

enum class ItemType { CLOCK, SYSTEM, SCREENTIME, CALENDAR, PHOTO, FOLDER, APP }

/** One element on the home workspace. Coordinates and sizes are in dp, relative to the page area. */
data class WorkItem(
    val id: String = UUID.randomUUID().toString(),
    val type: ItemType,
    val page: Int = 0,
    val x: Float = 16f,
    val y: Float = 16f,
    val w: Float = 160f,
    val h: Float = 160f,
    val alpha: Float = 1f,
    val corner: Float = 24f,
    val locked: Boolean = false,
    val rotation: Float = 0f,
    val border: Boolean = false,
    /** CLOCK: "digital|24"; PHOTO: file path; FOLDER: name; APP: package name */
    val payload: String = "",
    /** FOLDER: contained package names */
    val apps: List<String> = emptyList(),
)

data class LauncherSettings(
    val themeId: Int = 0,
    val glassAlpha: Float = 0.18f,
    val snap: Boolean = true,
    val layoutLocked: Boolean = false,
    val showLabels: Boolean = true,
    val iconShape: Int = 1,
    val wallpaper: String = "",
    val dim: Float = 0.35f,
    val showDock: Boolean = true,
    val drawerColumns: Int = 4,
    val pages: Int = 2,
)

data class LauncherState(
    val settings: LauncherSettings = LauncherSettings(),
    val items: List<WorkItem> = emptyList(),
    val dock: List<String> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val hidden: Set<String> = emptySet(),
    val recents: List<String> = emptyList(),
)

fun defaultState() = LauncherState(
    items = listOf(
        WorkItem(id = "clock0", type = ItemType.CLOCK, page = 0, x = 16f, y = 16f, w = 328f, h = 130f, payload = "digital|24"),
        WorkItem(id = "sys0", type = ItemType.SYSTEM, page = 0, x = 16f, y = 160f, w = 160f, h = 180f),
        WorkItem(id = "st0", type = ItemType.SCREENTIME, page = 0, x = 184f, y = 160f, w = 160f, h = 180f),
        WorkItem(id = "cal0", type = ItemType.CALENDAR, page = 1, x = 16f, y = 16f, w = 328f, h = 330f),
    ),
)

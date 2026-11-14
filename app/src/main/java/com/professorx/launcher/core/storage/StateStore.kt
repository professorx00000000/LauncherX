package com.professorx.launcher.core.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.professorx.launcher.launcher.workspace.ItemType
import com.professorx.launcher.launcher.workspace.LauncherSettings
import com.professorx.launcher.launcher.workspace.LauncherState
import com.professorx.launcher.launcher.workspace.WorkItem
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.stateDataStore by preferencesDataStore(name = "professorx_state")

class StateStore(private val ctx: Context) {
    private val key = stringPreferencesKey("state_v1")
    private val corruptKey = stringPreferencesKey("state_v1_corrupt_backup")

    suspend fun load(): LauncherState? {
        val raw = ctx.stateDataStore.data.first()[key] ?: return null
        return runCatching { Codec.decode(raw) }.getOrElse {
            // Never silently destroy a config we could not read: keep the raw text.
            ctx.stateDataStore.edit { p -> p[corruptKey] = raw }
            null
        }
    }

    suspend fun save(s: LauncherState) {
        ctx.stateDataStore.edit { it[key] = Codec.encode(s) }
    }
}

/** JSON codec with a schema number so future versions can migrate old data instead of dropping it. */
object Codec {
    const val SCHEMA = 1

    fun encode(s: LauncherState): String {
        val st = s.settings
        val so = JSONObject()
            .put("themeId", st.themeId)
            .put("glassAlpha", st.glassAlpha.toDouble())
            .put("snap", st.snap)
            .put("layoutLocked", st.layoutLocked)
            .put("showLabels", st.showLabels)
            .put("iconShape", st.iconShape)
            .put("wallpaper", st.wallpaper)
            .put("dim", st.dim.toDouble())
            .put("showDock", st.showDock)
            .put("drawerColumns", st.drawerColumns)
            .put("pages", st.pages)
        val items = JSONArray()
        s.items.forEach { items.put(itemToJson(it)) }
        return JSONObject()
            .put("schema", SCHEMA)
            .put("settings", so)
            .put("items", items)
            .put("dock", JSONArray(s.dock))
            .put("favorites", JSONArray(s.favorites.toList()))
            .put("hidden", JSONArray(s.hidden.toList()))
            .put("recents", JSONArray(s.recents))
            .toString()
    }

    fun decode(str: String): LauncherState {
        val o = JSONObject(str)
        require(o.optInt("schema", 1) <= SCHEMA) { "Backup is from a newer version" }
        val so = o.optJSONObject("settings") ?: JSONObject()
        val d = LauncherSettings()
        val settings = LauncherSettings(
            themeId = so.optInt("themeId", d.themeId),
            glassAlpha = so.optDouble("glassAlpha", d.glassAlpha.toDouble()).toFloat(),
            snap = so.optBoolean("snap", d.snap),
            layoutLocked = so.optBoolean("layoutLocked", d.layoutLocked),
            showLabels = so.optBoolean("showLabels", d.showLabels),
            iconShape = so.optInt("iconShape", d.iconShape),
            wallpaper = so.optString("wallpaper", d.wallpaper),
            dim = so.optDouble("dim", d.dim.toDouble()).toFloat(),
            showDock = so.optBoolean("showDock", d.showDock),
            drawerColumns = so.optInt("drawerColumns", d.drawerColumns),
            pages = so.optInt("pages", d.pages).coerceIn(1, 8),
        )
        val arr = o.optJSONArray("items")
        val items = if (arr == null) emptyList() else (0 until arr.length()).mapNotNull {
            runCatching { itemFromJson(arr.getJSONObject(it)) }.getOrNull()
        }
        return LauncherState(
            settings = settings,
            items = items,
            dock = strings(o.optJSONArray("dock")),
            favorites = strings(o.optJSONArray("favorites")).toSet(),
            hidden = strings(o.optJSONArray("hidden")).toSet(),
            recents = strings(o.optJSONArray("recents")),
        )
    }

    private fun strings(a: JSONArray?): List<String> =
        if (a == null) emptyList() else (0 until a.length()).map { a.getString(it) }

    private fun itemToJson(i: WorkItem): JSONObject = JSONObject()
        .put("id", i.id).put("type", i.type.name).put("page", i.page)
        .put("x", i.x.toDouble()).put("y", i.y.toDouble()).put("w", i.w.toDouble()).put("h", i.h.toDouble())
        .put("alpha", i.alpha.toDouble()).put("corner", i.corner.toDouble())
        .put("locked", i.locked).put("rotation", i.rotation.toDouble()).put("border", i.border)
        .put("payload", i.payload).put("apps", JSONArray(i.apps))

    private fun itemFromJson(o: JSONObject): WorkItem = WorkItem(
        id = o.getString("id"),
        type = ItemType.valueOf(o.getString("type")),
        page = o.optInt("page", 0),
        x = o.optDouble("x", 16.0).toFloat(),
        y = o.optDouble("y", 16.0).toFloat(),
        w = o.optDouble("w", 160.0).toFloat(),
        h = o.optDouble("h", 160.0).toFloat(),
        alpha = o.optDouble("alpha", 1.0).toFloat(),
        corner = o.optDouble("corner", 24.0).toFloat(),
        locked = o.optBoolean("locked", false),
        rotation = o.optDouble("rotation", 0.0).toFloat(),
        border = o.optBoolean("border", false),
        payload = o.optString("payload", ""),
        apps = strings(o.optJSONArray("apps")),
    )
}

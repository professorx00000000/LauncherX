package com.professorx.launcher.launcher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.professorx.launcher.core.storage.Codec
import com.professorx.launcher.core.storage.StateStore
import com.professorx.launcher.launcher.appdrawer.AppInfo
import com.professorx.launcher.launcher.appdrawer.AppRepository
import com.professorx.launcher.launcher.workspace.ItemType
import com.professorx.launcher.launcher.workspace.LauncherSettings
import com.professorx.launcher.launcher.workspace.LauncherState
import com.professorx.launcher.launcher.workspace.WorkItem
import com.professorx.launcher.launcher.workspace.defaultState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context = app.applicationContext
    private val store = StateStore(ctx)
    private val repo = AppRepository(ctx)

    private val _state = MutableStateFlow(LauncherState())
    val state: StateFlow<LauncherState> = _state.asStateFlow()
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    var homeSignal by mutableIntStateOf(0)
        private set

    private var saveJob: Job? = null

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) = refreshApps()
    }

    init {
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(ctx, pkgReceiver, f, ContextCompat.RECEIVER_NOT_EXPORTED)
        viewModelScope.launch {
            val saved = store.load()
            if (saved != null) {
                _state.value = saved
            } else {
                val dock = withContext(Dispatchers.IO) { repo.defaultDock() }
                _state.value = defaultState().copy(dock = dock)
            }
            _loaded.value = true
            if (saved == null) store.save(_state.value)
            refreshApps()
        }
    }

    fun refreshApps() {
        viewModelScope.launch(Dispatchers.IO) { _apps.value = repo.loadApps() }
    }

    fun goHome() { homeSignal++ }

    // ---------- persistence ----------
    private fun mutate(f: (LauncherState) -> LauncherState) {
        _state.update(f)
        if (_loaded.value) schedule()
    }

    private fun schedule() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(300)
            store.save(_state.value)
        }
    }

    fun flush() {
        if (!_loaded.value) return
        saveJob?.cancel()
        viewModelScope.launch { store.save(_state.value) }
    }

    // ---------- items ----------
    fun updateItem(id: String, f: (WorkItem) -> WorkItem) =
        mutate { s -> s.copy(items = s.items.map { if (it.id == id) f(it) else it }) }

    fun addItem(item: WorkItem) = mutate { it.copy(items = it.items + item) }

    fun removeItem(id: String) {
        val removed = _state.value.items.firstOrNull { it.id == id } ?: return
        mutate { s -> s.copy(items = s.items.filter { it.id != id }) }
        if (removed.type == ItemType.PHOTO && _state.value.items.none { it.payload == removed.payload }) {
            viewModelScope.launch(Dispatchers.IO) { runCatching { File(removed.payload).delete() } }
        }
    }

    fun duplicate(id: String) = mutate { s ->
        val src = s.items.firstOrNull { it.id == id } ?: return@mutate s
        s.copy(items = s.items + src.copy(id = UUID.randomUUID().toString(), x = src.x + 16f, y = src.y + 16f, locked = false))
    }

    // ---------- pages ----------
    fun addPage() = mutate { s -> s.copy(settings = s.settings.copy(pages = (s.settings.pages + 1).coerceAtMost(8))) }

    /** Removes the last page only if it holds no items (never deletes user content). */
    fun removeLastPage(): Boolean {
        val s = _state.value
        val last = s.settings.pages - 1
        if (s.settings.pages <= 1 || s.items.any { it.page == last }) return false
        mutate { it.copy(settings = it.settings.copy(pages = last)) }
        return true
    }

    // ---------- apps ----------
    fun launchApp(pkg: String) {
        if (repo.launch(pkg)) {
            mutate { s -> s.copy(recents = (listOf(pkg) + s.recents.filter { it != pkg }).take(12)) }
        }
    }

    fun addToDock(pkg: String) = mutate { s ->
        if (pkg in s.dock || s.dock.size >= 5) s else s.copy(dock = s.dock + pkg)
    }

    fun removeFromDock(pkg: String) = mutate { s -> s.copy(dock = s.dock - pkg) }

    fun toggleFavorite(pkg: String) = mutate { s ->
        s.copy(favorites = if (pkg in s.favorites) s.favorites - pkg else s.favorites + pkg)
    }

    fun hideApp(pkg: String) = mutate { s -> s.copy(hidden = s.hidden + pkg) }
    fun unhideAll() = mutate { s -> s.copy(hidden = emptySet()) }

    fun setSettings(f: (LauncherSettings) -> LauncherSettings) =
        mutate { s -> s.copy(settings = f(s.settings)) }

    // ---------- images ----------
    private suspend fun importImage(uri: Uri, dirName: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(ctx.filesDir, dirName).apply { mkdirs() }
            val f = File(dir, UUID.randomUUID().toString() + ".img")
            ctx.contentResolver.openInputStream(uri)!!.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
            f.absolutePath
        }.getOrNull()
    }

    fun addPhotos(uris: List<Uri>, page: Int) {
        viewModelScope.launch {
            uris.forEachIndexed { idx, u ->
                val path = importImage(u, "photos") ?: return@forEachIndexed
                addItem(
                    WorkItem(
                        type = ItemType.PHOTO, page = page,
                        x = 24f + idx * 16f, y = 24f + idx * 16f, w = 180f, h = 180f,
                        corner = 16f, payload = path,
                    ),
                )
            }
        }
    }

    fun setWallpaper(uri: Uri) {
        viewModelScope.launch {
            val path = importImage(uri, "wallpaper") ?: return@launch
            val old = _state.value.settings.wallpaper
            setSettings { it.copy(wallpaper = path) }
            if (old.isNotBlank()) withContext(Dispatchers.IO) { runCatching { File(old).delete() } }
        }
    }

    fun clearWallpaper() {
        val old = _state.value.settings.wallpaper
        setSettings { it.copy(wallpaper = "") }
        if (old.isNotBlank()) viewModelScope.launch(Dispatchers.IO) { runCatching { File(old).delete() } }
    }

    // ---------- backup ----------
    fun exportJson(): String = Codec.encode(_state.value)

    fun importJson(text: String): Boolean {
        val restored = runCatching { Codec.decode(text) }.getOrNull() ?: return false
        mutate { restored }
        return true
    }

    override fun onCleared() {
        runCatching { ctx.unregisterReceiver(pkgReceiver) }
        super.onCleared()
    }
}

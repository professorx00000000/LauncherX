package com.professorx.launcher.app

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.professorx.launcher.core.design.GlassButton
import com.professorx.launcher.core.design.GlassSearchBar
import com.professorx.launcher.core.design.LauncherTheme
import com.professorx.launcher.core.design.ProfessorXTheme
import com.professorx.launcher.core.design.Themes
import com.professorx.launcher.core.storage.decodeSampled
import com.professorx.launcher.core.ui.IconStyle
import com.professorx.launcher.core.ui.LocalApps
import com.professorx.launcher.core.ui.LocalIconStyle
import com.professorx.launcher.core.ui.LocalLaunch
import com.professorx.launcher.launcher.LauncherViewModel
import com.professorx.launcher.launcher.appdrawer.AppDrawer
import com.professorx.launcher.launcher.dock.Dock
import com.professorx.launcher.launcher.workspace.EditDialog
import com.professorx.launcher.launcher.workspace.ItemType
import com.professorx.launcher.launcher.workspace.LauncherSettings
import com.professorx.launcher.launcher.workspace.WorkItem
import com.professorx.launcher.launcher.workspace.WorkspacePage
import com.professorx.launcher.settings.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LauncherApp(vm: LauncherViewModel) {
    val state by vm.state.collectAsState()
    val apps by vm.apps.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val ctx = LocalContext.current
    val theme = Themes.all.getOrElse(state.settings.themeId) { Themes.all[0] }

    ProfessorXTheme(theme, state.settings.glassAlpha) {
        if (!loaded) {
            Box(Modifier.fillMaxSize().background(Color.Black))
        } else {
            val appMap = remember(apps) { apps.associateBy { it.pkg } }
            val scope = rememberCoroutineScope()
            val pagerState = androidx.compose.foundation.pager.rememberPagerState { state.settings.pages }

            var drawerOpen by remember { mutableStateOf(false) }
            var drawerSearch by remember { mutableStateOf(false) }
            var settingsOpen by remember { mutableStateOf(false) }
            var addMenu by remember { mutableStateOf(false) }
            var selectedId by remember { mutableStateOf<String?>(null) }
            var editId by remember { mutableStateOf<String?>(null) }
            var confirmRemove by remember { mutableStateOf<String?>(null) }

            val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(5)) { uris ->
                if (uris.isNotEmpty()) vm.addPhotos(uris, pagerState.currentPage)
            }
            val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) vm.setWallpaper(uri)
            }

            LaunchedEffect(vm.homeSignal) {
                if (vm.homeSignal > 0) {
                    drawerOpen = false; settingsOpen = false; addMenu = false
                    selectedId = null; editId = null
                    pagerState.animateScrollToPage(0)
                }
            }

            // The launcher must never exit on Back.
            BackHandler {
                when {
                    editId != null -> editId = null
                    addMenu -> addMenu = false
                    settingsOpen -> settingsOpen = false
                    drawerOpen -> drawerOpen = false
                    selectedId != null -> selectedId = null
                    else -> scope.launch { pagerState.animateScrollToPage(0) }
                }
            }

            fun addWidget(type: ItemType) {
                val page = pagerState.currentPage
                val n = state.items.count { it.page == page }
                val (w, h) = when (type) {
                    ItemType.CLOCK -> 300f to 120f
                    ItemType.SYSTEM -> 160f to 180f
                    ItemType.SCREENTIME -> 180f to 190f
                    ItemType.CALENDAR -> 300f to 300f
                    ItemType.FOLDER -> 170f to 170f
                    else -> 160f to 160f
                }
                val item = WorkItem(
                    type = type, page = page,
                    x = 16f + (n % 6) * 12f, y = 16f + (n % 6) * 12f, w = w, h = h,
                    payload = when (type) {
                        ItemType.CLOCK -> "digital|24"
                        ItemType.FOLDER -> "Folder"
                        else -> ""
                    },
                )
                vm.addItem(item)
                addMenu = false
                selectedId = item.id
                if (type == ItemType.FOLDER) editId = item.id
            }

            CompositionLocalProvider(
                LocalApps provides appMap,
                LocalLaunch provides { pkg: String -> vm.launchApp(pkg); drawerOpen = false },
                LocalIconStyle provides IconStyle(state.settings.iconShape, state.settings.showLabels),
            ) {
                Box(Modifier.fillMaxSize().background(theme.background)) {
                    Wallpaper(state.settings, theme)

                    Column(Modifier.fillMaxSize().systemBarsPadding()) {
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                userScrollEnabled = selectedId == null,
                            ) { page ->
                                WorkspacePage(
                                    page = page,
                                    state = state,
                                    selectedId = selectedId,
                                    onSelect = { selectedId = it },
                                    onOpenDrawer = { search -> drawerSearch = search; drawerOpen = true },
                                    onLongPressEmpty = { addMenu = true },
                                    vm = vm,
                                )
                            }
                            val sel = state.items.firstOrNull { it.id == selectedId }
                            if (sel != null) {
                                Column(
                                    Modifier.align(Alignment.BottomCenter).padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        if (state.settings.layoutLocked || sel.locked) "Locked: unlock to move or resize"
                                        else "Drag to move · drag the dots to resize",
                                        color = theme.subText, fontSize = 11.sp,
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        GlassButton("Edit") { editId = sel.id }
                                        GlassButton("Copy") { vm.duplicate(sel.id) }
                                        GlassButton(if (sel.locked) "Unlock" else "Lock") {
                                            vm.updateItem(sel.id) { it.copy(locked = !it.locked) }
                                        }
                                        GlassButton("Remove") { confirmRemove = sel.id }
                                        GlassButton("Done") { selectedId = null }
                                    }
                                }
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            repeat(state.settings.pages) { i ->
                                Box(
                                    Modifier.padding(horizontal = 3.dp).size(7.dp).clip(CircleShape)
                                        .background(if (i == pagerState.currentPage) theme.primary else Color.White.copy(alpha = 0.3f)),
                                )
                            }
                        }
                        GlassSearchBar(
                            "Search apps",
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        ) { drawerSearch = true; drawerOpen = true }
                        if (state.settings.showDock) Dock(state.dock) { vm.removeFromDock(it) }
                        else Box(Modifier.size(8.dp))
                    }

                    AnimatedVisibility(
                        visible = drawerOpen,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                    ) {
                        AppDrawer(
                            apps = apps,
                            state = state,
                            focusSearch = drawerSearch,
                            onLaunch = { vm.launchApp(it); drawerOpen = false },
                            onAddHome = { pkg ->
                                vm.addItem(
                                    WorkItem(
                                        type = ItemType.APP, page = pagerState.currentPage,
                                        x = 24f, y = 24f, w = 72f, h = 88f, payload = pkg,
                                    ),
                                )
                                drawerOpen = false
                                Toast.makeText(ctx, "Added to home", Toast.LENGTH_SHORT).show()
                            },
                            onAddDock = { pkg ->
                                if (state.dock.size >= 5 && pkg !in state.dock) {
                                    Toast.makeText(ctx, "Dock is full (5)", Toast.LENGTH_SHORT).show()
                                } else vm.addToDock(pkg)
                            },
                            onFav = { vm.toggleFavorite(it) },
                            onHide = { vm.hideApp(it) },
                            onClose = { drawerOpen = false },
                        )
                    }

                    AnimatedVisibility(
                        visible = settingsOpen,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                    ) {
                        SettingsScreen(
                            vm = vm,
                            state = state,
                            onPickWallpaper = {
                                wallpaperPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onClose = { settingsOpen = false },
                        )
                    }
                }

                if (addMenu) {
                    AlertDialog(
                        onDismissRequest = { addMenu = false },
                        title = { Text("Add to home") },
                        text = {
                            Column {
                                MenuRow("Clock") { addWidget(ItemType.CLOCK) }
                                MenuRow("System monitor") { addWidget(ItemType.SYSTEM) }
                                MenuRow("Screen time") { addWidget(ItemType.SCREENTIME) }
                                MenuRow("Calendar") { addWidget(ItemType.CALENDAR) }
                                MenuRow("Photos…") {
                                    addMenu = false
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                                MenuRow("Folder") { addWidget(ItemType.FOLDER) }
                                MenuRow("Settings & wallpaper") { addMenu = false; settingsOpen = true }
                            }
                        },
                        confirmButton = { TextButton({ addMenu = false }) { Text("Cancel") } },
                    )
                }

                val editing = state.items.firstOrNull { it.id == editId }
                if (editing != null) {
                    val cfg = LocalConfiguration.current
                    EditDialog(
                        item = editing,
                        maxW = cfg.screenWidthDp.toFloat(),
                        maxH = cfg.screenHeightDp * 0.7f,
                        apps = apps,
                        vm = vm,
                        onDismiss = { editId = null },
                    )
                }

                confirmRemove?.let { id ->
                    AlertDialog(
                        onDismissRequest = { confirmRemove = null },
                        title = { Text("Remove this item?") },
                        text = { Text("It is removed from the home screen only. Apps are not uninstalled.") },
                        confirmButton = {
                            TextButton({ vm.removeItem(id); selectedId = null; confirmRemove = null }) { Text("Remove") }
                        },
                        dismissButton = { TextButton({ confirmRemove = null }) { Text("Cancel") } },
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuRow(text: String, onClick: () -> Unit) {
    TextButton(onClick, Modifier.fillMaxWidth()) { Text(text) }
}

@Composable
private fun Wallpaper(settings: LauncherSettings, theme: LauncherTheme) {
    val bmp by produceState<ImageBitmap?>(null, settings.wallpaper) {
        value = if (settings.wallpaper.isBlank()) null
        else withContext(Dispatchers.IO) { decodeSampled(settings.wallpaper, 1600) }
    }
    val b = bmp
    Box(Modifier.fillMaxSize()) {
        if (b != null) {
            Image(b, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(theme.background)
                val c = Offset(size.width * 0.5f, size.height * 0.3f)
                drawCircle(
                    Brush.radialGradient(listOf(theme.primary.copy(alpha = 0.40f), Color.Transparent), center = c, radius = size.width),
                    radius = size.width, center = c,
                )
            }
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = settings.dim)))
    }
}

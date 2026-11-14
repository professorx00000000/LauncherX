package com.professorx.launcher.launcher.appdrawer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import android.content.pm.PackageManager

data class AppInfo(val pkg: String, val label: String)

class AppRepository(private val ctx: Context) {
    private val pm: PackageManager = ctx.packageManager

    fun loadApps(): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { AppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.pkg != ctx.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }

    fun launch(pkg: String): Boolean {
        val i = pm.getLaunchIntentForPackage(pkg) ?: return false
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return runCatching { ctx.startActivity(i) }.isSuccess
    }

    /** Phone, messages, browser, camera - whatever the device resolves them to. */
    fun defaultDock(): List<String> {
        val intents = listOf(
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:")),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
        )
        return intents
            .mapNotNull { runCatching { pm.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY) }.getOrNull() }
            .map { it.activityInfo.packageName }
            .filter { it != "android" && it != ctx.packageName }
            .distinct()
    }
}

object IconCache {
    private val cache = LruCache<String, ImageBitmap>(120)

    fun peek(pkg: String): ImageBitmap? = cache.get(pkg)

    fun get(ctx: Context, pkg: String): ImageBitmap? {
        cache.get(pkg)?.let { return it }
        val d = runCatching { ctx.packageManager.getApplicationIcon(pkg) }.getOrNull() ?: return null
        val bmp = runCatching { d.toBitmap(128, 128).asImageBitmap() }.getOrNull() ?: return null
        cache.put(pkg, bmp)
        return bmp
    }
}

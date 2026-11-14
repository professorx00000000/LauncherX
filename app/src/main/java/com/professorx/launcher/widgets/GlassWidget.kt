package com.professorx.launcher.widgets

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.professorx.launcher.core.design.GlassCard
import com.professorx.launcher.launcher.workspace.WorkItem

@Composable
fun GlassWidget(item: WorkItem, content: @Composable BoxScope.() -> Unit) {
    GlassCard(Modifier.fillMaxSize(), corner = item.corner.dp, content = content)
}

package com.example.sshclient.ui.screen.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuickKeyBar(
    onTab: () -> Unit,
    onCtrlC: () -> Unit,
    onCtrlD: () -> Unit,
    onCtrlZ: () -> Unit,
    onCtrlL: () -> Unit,
    onArrowUp: () -> Unit,
    onArrowDown: () -> Unit,
    onEscape: () -> Unit,
    onPaste: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        "TAB" to onTab,
        "↑" to onArrowUp,
        "↓" to onArrowDown,
        "ESC" to onEscape,
        "^C" to onCtrlC,
        "^D" to onCtrlD,
        "^Z" to onCtrlZ,
        "^L" to onCtrlL,
        "Paste" to onPaste
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(keys) { (label, action) ->
                OutlinedButton(
                    onClick = action,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

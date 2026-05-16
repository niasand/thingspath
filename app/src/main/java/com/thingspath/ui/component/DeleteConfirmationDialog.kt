package com.thingspath.ui.component

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "确认删除")
        },
        text = {
            Text(text = "确定要删除吗？此操作不可撤销。")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        },
        modifier = modifier
    )
}

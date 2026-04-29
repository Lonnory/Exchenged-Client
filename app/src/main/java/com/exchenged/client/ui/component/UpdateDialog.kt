package com.exchenged.client.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.exchenged.client.R

@Composable
fun UpdateDialog(
    versionName: String,
    changelog: String,
    isCritical: Boolean = false,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = { if (!isCritical) onDismiss() },
        title = { 
            Text(
                if (isCritical) stringResource(R.string.critical_update_required, versionName) 
                else stringResource(R.string.update_available, versionName)
            ) 
        },
        text = { 
            Column {
                if (isCritical) {
                    Text(stringResource(R.string.mandatory_update_message))
                }
                Text(changelog)
            }
        },
        confirmButton = {
            Button(onClick = onUpdate) {
                Text(stringResource(R.string.update_now))
            }
        },
        dismissButton = if (!isCritical) {
            {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.not_now))
                }
            }
        } else null
    )
}

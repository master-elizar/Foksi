package com.foksi.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.foksi.app.R
import com.foksi.app.notifications.ReminderStatus
import com.foksi.app.notifications.ReminderStatusLevel
import com.foksi.app.ui.theme.LocalFoksiColors

/** Shows the honest state of Android's notification restrictions, with a way to fix it. */
@Composable
fun ReminderStatusBanner(status: ReminderStatus, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val extras = LocalFoksiColors.current
    val (tint, icon) = when (status.level) {
        ReminderStatusLevel.OK -> extras.success to Icons.Outlined.CheckCircle
        ReminderStatusLevel.WARNING -> extras.warning to Icons.Outlined.WarningAmber
        ReminderStatusLevel.ERROR -> extras.danger to Icons.Outlined.ErrorOutline
    }
    val clickable = status.systemIntent != null

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
        onClick = {
            status.systemIntent?.let { intent -> runCatching { context.startActivity(intent) } }
        },
        enabled = clickable,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(status.messageRes),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            if (clickable) {
                Text(
                    text = stringResource(R.string.status_fix),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

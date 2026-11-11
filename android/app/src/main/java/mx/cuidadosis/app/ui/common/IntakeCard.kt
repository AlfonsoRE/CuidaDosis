package mx.cuidadosis.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.cuidadosis.app.data.remote.IntakeDto

/**
 * Tarjeta de una toma del día. [action] es el botón "Tomado" del paciente;
 * el cuidador la usa sin acción (solo lectura).
 */
@Composable
fun IntakeCard(
    intake: IntakeDto,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val container = if (intake.isTaken) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                intake.time,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(72.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(intake.name, style = MaterialTheme.typography.titleMedium)
                Text(intake.dose, style = MaterialTheme.typography.bodyMedium)
                Text(
                    statusText(intake),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!intake.isTaken && isTimePast(intake.time)) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            action?.invoke()
        }
    }
}

private fun statusText(intake: IntakeDto): String = when {
    intake.isTaken -> "Tomada a las ${formatApiTime(intake.confirmedAt)}"
    isTimePast(intake.time) -> "Pendiente, la hora ya pasó"
    else -> "Pendiente"
}

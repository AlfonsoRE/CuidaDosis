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
import mx.cuidadosis.app.data.IntakeState
import mx.cuidadosis.app.data.intakeState
import mx.cuidadosis.app.data.nowMexico
import mx.cuidadosis.app.data.parseApiDateTime
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
    val state = stateOf(intake)
    val container = when (state) {
        IntakeState.CONFIRMED -> MaterialTheme.colorScheme.primaryContainer
        IntakeState.UNCONFIRMED -> MaterialTheme.colorScheme.errorContainer
        IntakeState.PENDING -> MaterialTheme.colorScheme.surfaceVariant
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
                intake.dose?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Text(
                    statusText(intake, state),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when (state) {
                        IntakeState.UNCONFIRMED -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            action?.invoke()
        }
    }
}

// Se recalcula con la hora actual de México: una toma pendiente pasa sola a "Sin confirmar" a T + 30
fun stateOf(intake: IntakeDto): IntakeState {
    val scheduled = parseApiDateTime(intake.scheduledFor) ?: return if (intake.isTaken) IntakeState.CONFIRMED else IntakeState.PENDING
    return intakeState(scheduled, intake.isTaken, nowMexico())
}

// Nunca "Tomada": el sistema no verifica la ingesta (RF-07, ADR-006)
private fun statusText(intake: IntakeDto, state: IntakeState): String = when (state) {
    IntakeState.CONFIRMED -> intake.confirmedAt?.let { "${state.label} a las ${formatApiTime(it)}" } ?: state.label
    else -> state.label
}

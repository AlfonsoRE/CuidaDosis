package mx.cuidadosis.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mx.cuidadosis.app.data.IntakeState
import mx.cuidadosis.app.data.TodayItem
import mx.cuidadosis.app.data.intakeState
import mx.cuidadosis.app.data.nowMexico
import mx.cuidadosis.app.data.parseApiDateTime
import mx.cuidadosis.app.data.remote.IntakeDto
import java.time.LocalDateTime

// Datos que muestra una tarjeta de toma, vengan de la API o de la copia local
data class IntakeView(
    val time: String,
    val name: String,
    // Null para el cuidador cuando el paciente no comparte la dosis (G-07)
    val dose: String?,
    val scheduledFor: LocalDateTime?,
    val confirmedAt: String?,
    val note: String? = null,
) {
    val confirmed: Boolean get() = confirmedAt != null
}

fun IntakeDto.toView() = IntakeView(
    time = time,
    name = name,
    dose = dose,
    scheduledFor = parseApiDateTime(scheduledFor),
    confirmedAt = if (isTaken) confirmedAt ?: "" else null,
)

fun TodayItem.toView() = IntakeView(
    time = time,
    name = name,
    dose = dose,
    scheduledFor = scheduledFor,
    confirmedAt = confirmedAt,
    note = if (waitingSync) "Guardada en el teléfono; se enviará al haber conexión." else null,
)

/**
 * Tarjeta de una toma. [action] es el botón "Tomado" del paciente;
 * el cuidador la usa sin acción (solo lectura).
 */
@Composable
fun IntakeCard(
    intake: IntakeView,
    modifier: Modifier = Modifier,
    now: LocalDateTime = nowMexico(),
    action: (@Composable () -> Unit)? = null,
) {
    val state = stateOf(intake, now)
    val container = when (state) {
        IntakeState.CONFIRMED -> MaterialTheme.colorScheme.primaryContainer
        IntakeState.UNCONFIRMED -> MaterialTheme.colorScheme.errorContainer
        IntakeState.PENDING -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = modifier
            .fillMaxWidth()
            .semantics { stateDescription = state.label },
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
                modifier = Modifier.widthIn(min = 64.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(intake.name, style = MaterialTheme.typography.titleMedium)
                intake.dose?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                Text(
                    statusText(intake, state),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = when (state) {
                        IntakeState.UNCONFIRMED -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                intake.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            action?.invoke()
        }
    }
}

// Se recalcula con la hora actual de México: una toma pendiente pasa sola a "Sin confirmar" a T + 30
fun stateOf(intake: IntakeView, now: LocalDateTime = nowMexico()): IntakeState {
    val scheduled = intake.scheduledFor ?: return if (intake.confirmed) IntakeState.CONFIRMED else IntakeState.PENDING
    return intakeState(scheduled, intake.confirmed, now)
}

// Nunca "Tomada": el sistema no verifica la ingesta (RF-07, ADR-006)
private fun statusText(intake: IntakeView, state: IntakeState): String = when (state) {
    IntakeState.CONFIRMED -> intake.confirmedAt?.takeIf { it.isNotEmpty() }
        ?.let { "${state.label} a las ${formatApiTime(it)}" } ?: state.label
    else -> state.label
}

// Texto "hh:mm" de la última sincronización, o aviso si nunca
fun lastSyncText(lastSyncAt: String?): String =
    lastSyncAt?.takeIf { parseApiDateTime(it) != null }
        ?.let { "Última sincronización: ${formatApiDateTime(it)}" }
        ?: "Todavía no hay sincronización con el servidor"

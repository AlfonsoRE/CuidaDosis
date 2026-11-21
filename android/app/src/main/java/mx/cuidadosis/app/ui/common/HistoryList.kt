package mx.cuidadosis.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mx.cuidadosis.app.data.CONFIRMATION_DISCLAIMER
import mx.cuidadosis.app.data.nowMexico
import mx.cuidadosis.app.data.remote.ApiResult
import mx.cuidadosis.app.data.remote.HistoryResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Historial de los últimos 7 días (RF-15), para paciente y cuidador. Necesita conexión.

data class HistoryUiState(
    val loading: Boolean = true,
    val data: HistoryResponse? = null,
    val error: String? = null,
)

class HistoryViewModel(private val load: suspend () -> ApiResult<HistoryResponse>) : ViewModel() {
    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val r = load()
            _state.update {
                when (r) {
                    is ApiResult.Success -> it.copy(loading = false, data = r.data)
                    is ApiResult.Error -> it.copy(loading = false, error = r.message)
                }
            }
        }
    }
}

private val DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy")

// "Hoy", "Ayer" o la fecha; la fecha de la API es de México
fun historyDayLabel(date: String, today: LocalDate): String {
    val d = runCatching { LocalDate.parse(date) }.getOrNull() ?: return date
    return when (d) {
        today -> "Hoy, ${d.format(DAY)}"
        today.minusDays(1) -> "Ayer, ${d.format(DAY)}"
        else -> d.format(DAY)
    }
}

@Composable
fun HistoryList(viewModel: HistoryViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val now = nowMexico()
    val data = state.data

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Últimos 7 días", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(
                    onClick = viewModel::refresh,
                    enabled = !state.loading,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text("Actualizar") }
            }
            Text(CONFIRMATION_DISCLAIMER, style = MaterialTheme.typography.bodyMedium)
        }
        when {
            state.loading && data == null -> item { LoadingBox(Modifier.fillMaxWidth().padding(32.dp)) }
            data == null -> item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ErrorText(state.error ?: "No se pudo cargar el historial.")
                    Text("El historial necesita conexión con el servidor.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = viewModel::refresh) { Text("Reintentar") }
                }
            }
            data.days.isEmpty() -> item {
                Text("No hay tomas en los últimos 7 días.", style = MaterialTheme.typography.bodyLarge)
            }
            else -> {
                state.error?.let { item { ErrorText(it) } }
                data.days.forEach { day ->
                    item(key = "d" + day.date) {
                        Column(Modifier.padding(top = 8.dp)) {
                            Text(historyDayLabel(day.date, now.toLocalDate()), style = MaterialTheme.typography.titleMedium)
                            Text(
                                summaryText(day.summary.confirmed, day.summary.total) +
                                    if (day.summary.unconfirmed > 0) ", ${day.summary.unconfirmed} sin confirmar" else "",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                    // Del más temprano al más tardío dentro de cada día
                    items(day.intakes.sortedBy { it.scheduledFor }, key = { "i" + it.id }) { intake ->
                        IntakeCard(intake.toView(), now = now)
                    }
                }
            }
        }
    }
}

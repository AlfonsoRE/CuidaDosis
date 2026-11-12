package mx.cuidadosis.app

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import mx.cuidadosis.app.reminders.ReminderNotifier

class CuidaDosisApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderNotifier.createChannel(this)
        watchSessionForReminders()
    }

    // Avisos solo para el paciente con sesión activa. Al iniciar sesión (o abrir la app) se
    // descargan sus medicamentos para programarlos; al cerrar sesión, expirar el token o
    // entrar como cuidador se cancelan y se borra la copia local.
    private fun watchSessionForReminders() {
        appScope.launch {
            container.sessionStore.session
                .distinctUntilChangedBy { it?.token }
                .collect { session ->
                    val scheduler = container.reminderScheduler
                    if (session == null || !session.isPatient) {
                        scheduler.cancelAll()
                        return@collect
                    }
                    if (scheduler.cachedUserId() == session.userId) {
                        scheduler.reschedule() // sin red, con la copia local
                    } else {
                        scheduler.cancelAll()
                    }
                    launch { container.medicationRepository.list() }
                }
        }
    }
}

// Acceso al contenedor desde las fábricas de ViewModel
val CreationExtras.container: AppContainer
    get() = (this[APPLICATION_KEY] as CuidaDosisApp).container

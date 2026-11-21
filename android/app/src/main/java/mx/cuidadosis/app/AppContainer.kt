package mx.cuidadosis.app

import android.content.Context
import mx.cuidadosis.app.data.AuthRepository
import mx.cuidadosis.app.data.IntakeRepository
import mx.cuidadosis.app.data.LinkRepository
import mx.cuidadosis.app.data.MedicationRepository
import mx.cuidadosis.app.data.local.PatientStore
import mx.cuidadosis.app.data.local.SessionStore
import mx.cuidadosis.app.data.local.SettingsStore
import mx.cuidadosis.app.data.remote.ApiClient
import mx.cuidadosis.app.reminders.CaregiverAlarmScheduler
import mx.cuidadosis.app.reminders.ReminderScheduler

// Dependencias de la app (inyección manual, sin frameworks)
class AppContainer(context: Context) {
    val settingsStore = SettingsStore(context, BuildConfig.DEFAULT_API_BASE_URL)
    val sessionStore = SessionStore(context)
    private val apiClient = ApiClient(settingsStore, sessionStore)
    val authRepository = AuthRepository(apiClient, sessionStore)
    val patientStore = PatientStore(context)
    val reminderScheduler = ReminderScheduler(context, patientStore, settingsStore)
    val medicationRepository = MedicationRepository(apiClient, sessionStore, patientStore, reminderScheduler)
    val intakeRepository = IntakeRepository(apiClient, sessionStore, patientStore, reminderScheduler)
    val linkRepository = LinkRepository(apiClient)
    val caregiverAlarms = CaregiverAlarmScheduler(context)
}

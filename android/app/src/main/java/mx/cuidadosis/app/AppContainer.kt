package mx.cuidadosis.app

import android.content.Context
import mx.cuidadosis.app.data.AuthRepository
import mx.cuidadosis.app.data.MedicationRepository
import mx.cuidadosis.app.data.local.SessionStore
import mx.cuidadosis.app.data.local.SettingsStore
import mx.cuidadosis.app.data.remote.ApiClient

// Dependencias de la app (inyección manual, sin frameworks)
class AppContainer(context: Context) {
    val settingsStore = SettingsStore(context, BuildConfig.DEFAULT_API_BASE_URL)
    val sessionStore = SessionStore(context)
    private val apiClient = ApiClient(settingsStore, sessionStore)
    val authRepository = AuthRepository(apiClient, sessionStore)
    val medicationRepository = MedicationRepository(apiClient)
}

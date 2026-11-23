package mx.cuidadosis.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import mx.cuidadosis.app.data.local.LocalNetwork
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import mx.cuidadosis.app.AppContainer
import mx.cuidadosis.app.data.canLogout
import mx.cuidadosis.app.data.pendingLogoutMessage
import mx.cuidadosis.app.data.local.Session
import mx.cuidadosis.app.ui.auth.LoginScreen
import mx.cuidadosis.app.ui.auth.RegisterScreen
import mx.cuidadosis.app.ui.caregiver.CaregiverHistoryScreen
import mx.cuidadosis.app.ui.caregiver.CaregiverHomeScreen
import mx.cuidadosis.app.ui.medications.MedicationFormScreen
import mx.cuidadosis.app.ui.caregiver.CaregiverPatientScreen
import mx.cuidadosis.app.ui.patient.PatientHomeScreen
import androidx.navigation.toRoute
import mx.cuidadosis.app.ui.settings.ReminderSettingsScreen
import mx.cuidadosis.app.ui.settings.ServerScreen

// Rutas de navegación
@Serializable object LoginRoute
@Serializable object RegisterRoute
@Serializable object ServerRoute
@Serializable object ReminderSettingsRoute
@Serializable object PatientHomeRoute
@Serializable data class MedicationFormRoute(val id: Int? = null)
@Serializable object CaregiverHomeRoute
@Serializable data class CaregiverPatientRoute(val id: Int, val name: String)
@Serializable data class CaregiverHistoryRoute(val id: Int, val name: String)

private fun homeFor(session: Session?): Any = when {
    session == null -> LoginRoute
    session.isPatient -> PatientHomeRoute
    else -> CaregiverHomeRoute
}

@Composable
fun AppNavHost(container: AppContainer) {
    val session by container.sessionStore.session.collectAsStateWithLifecycle()
    val serverUrl by container.settingsStore.baseUrl.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val logout: () -> Unit = { scope.launch { container.authRepository.logout() } }

    // Paciente: antes de salir se envían las confirmaciones pendientes; si no se puede, no sale
    var pendingAtLogout by remember { mutableStateOf(0) }
    var sendingBeforeLogout by remember { mutableStateOf(false) }
    val patientLogout: () -> Unit = {
        scope.launch {
            val intakes = container.intakeRepository
            sendingBeforeLogout = true
            if (intakes.hasPendingSync()) intakes.syncQueue()
            sendingBeforeLogout = false
            val pending = container.patientStore.current?.queue?.size ?: 0
            if (canLogout(pending)) {
                pendingAtLogout = 0
                container.authRepository.logout()
            } else {
                pendingAtLogout = pending
            }
        }
    }
    if (pendingAtLogout > 0) {
        AlertDialog(
            onDismissRequest = { pendingAtLogout = 0 },
            title = { Text("Confirmaciones sin enviar") },
            text = { Text(pendingLogoutMessage(pendingAtLogout)) },
            confirmButton = {
                TextButton(onClick = patientLogout, enabled = !sendingBeforeLogout) { Text("Reintentar envío") }
            },
            dismissButton = { TextButton(onClick = { pendingAtLogout = 0 }) { Text("Quedarme") } },
        )
    }

    // Android 17+: pide permiso de red local si la API está en la LAN (XAMPP)
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(serverUrl) {
        if (LocalNetwork.needsPermission(context, serverUrl)) {
            permissionLauncher.launch(LocalNetwork.PERMISSION)
        }
    }

    // Cuando la sesión empieza o termina (login, registro, logout o token vencido)
    // se limpia la pila y se va a la pantalla que corresponde al rol
    var lastToken by remember { mutableStateOf(session?.token) }
    LaunchedEffect(session?.token) {
        if (session?.token != lastToken) {
            lastToken = session?.token
            navController.navigate(homeFor(session)) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = remember { homeFor(session) }) {
        composable<LoginRoute> {
            LoginScreen(
                serverUrl = serverUrl,
                onRegister = { navController.navigate(RegisterRoute) },
                onServer = if (container.settingsStore.allowServerChange) {
                    { navController.navigate(ServerRoute) }
                } else {
                    null
                },
            )
        }
        composable<RegisterRoute> {
            RegisterScreen(onBack = { navController.popBackStack() })
        }
        composable<ServerRoute> {
            ServerScreen(onBack = { navController.popBackStack() })
        }
        composable<PatientHomeRoute> {
            PatientHomeScreen(
                userName = session?.fullName.orEmpty(),
                onAddMedication = { navController.navigate(MedicationFormRoute()) },
                onEditMedication = { id -> navController.navigate(MedicationFormRoute(id)) },
                onSettings = { navController.navigate(ReminderSettingsRoute) },
                onLogout = patientLogout,
            )
        }
        composable<ReminderSettingsRoute> {
            ReminderSettingsScreen(container, onBack = { navController.popBackStack() })
        }
        composable<MedicationFormRoute> {
            MedicationFormScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable<CaregiverHomeRoute> {
            CaregiverHomeScreen(
                userName = session?.fullName.orEmpty(),
                onOpenPatient = { id, name -> navController.navigate(CaregiverPatientRoute(id, name)) },
                onLogout = logout,
            )
        }
        composable<CaregiverPatientRoute> { entry ->
            val route = entry.toRoute<CaregiverPatientRoute>()
            CaregiverPatientScreen(
                patientName = route.name,
                onBack = { navController.popBackStack() },
                onHistory = { navController.navigate(CaregiverHistoryRoute(route.id, route.name)) },
            )
        }
        composable<CaregiverHistoryRoute> { entry ->
            val route = entry.toRoute<CaregiverHistoryRoute>()
            CaregiverHistoryScreen(route.id, route.name, onBack = { navController.popBackStack() })
        }
    }
}

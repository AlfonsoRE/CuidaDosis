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
import mx.cuidadosis.app.AppContainer
import mx.cuidadosis.app.data.local.Session
import mx.cuidadosis.app.ui.auth.LoginScreen
import mx.cuidadosis.app.ui.auth.RegisterScreen
import mx.cuidadosis.app.ui.caregiver.CaregiverHomeScreen
import mx.cuidadosis.app.ui.medications.MedicationFormScreen
import mx.cuidadosis.app.ui.medications.MedicationListScreen
import mx.cuidadosis.app.ui.settings.ServerScreen

// Rutas de navegación
@Serializable object LoginRoute
@Serializable object RegisterRoute
@Serializable object ServerRoute
@Serializable object MedicationListRoute
@Serializable data class MedicationFormRoute(val id: Int? = null)
@Serializable object CaregiverHomeRoute

private fun homeFor(session: Session?): Any = when {
    session == null -> LoginRoute
    session.isPatient -> MedicationListRoute
    else -> CaregiverHomeRoute
}

@Composable
fun AppNavHost(container: AppContainer) {
    val session by container.sessionStore.session.collectAsStateWithLifecycle()
    val serverUrl by container.settingsStore.baseUrl.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val logout: () -> Unit = { scope.launch { container.authRepository.logout() } }

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
                onServer = { navController.navigate(ServerRoute) },
            )
        }
        composable<RegisterRoute> {
            RegisterScreen(onBack = { navController.popBackStack() })
        }
        composable<ServerRoute> {
            ServerScreen(onBack = { navController.popBackStack() })
        }
        composable<MedicationListRoute> {
            MedicationListScreen(
                userName = session?.fullName.orEmpty(),
                onAdd = { navController.navigate(MedicationFormRoute()) },
                onEdit = { id -> navController.navigate(MedicationFormRoute(id)) },
                onLogout = logout,
            )
        }
        composable<MedicationFormRoute> {
            MedicationFormScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable<CaregiverHomeRoute> {
            CaregiverHomeScreen(userName = session?.fullName.orEmpty(), onLogout = logout)
        }
    }
}

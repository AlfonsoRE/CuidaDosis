package mx.cuidadosis.app

import android.app.Application
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY

class CuidaDosisApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

// Acceso al contenedor desde las fábricas de ViewModel
val CreationExtras.container: AppContainer
    get() = (this[APPLICATION_KEY] as CuidaDosisApp).container

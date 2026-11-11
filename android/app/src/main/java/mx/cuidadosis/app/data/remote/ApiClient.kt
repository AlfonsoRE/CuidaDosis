package mx.cuidadosis.app.data.remote

import mx.cuidadosis.app.data.local.SessionStore
import mx.cuidadosis.app.data.local.SettingsStore
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

// Crea el cliente Retrofit con la URL base actual y agrega el token de sesión
class ApiClient(
    private val settings: SettingsStore,
    private val session: SessionStore,
) {
    private val authInterceptor = Interceptor { chain ->
        val token = session.current?.token
        val request = if (token != null) {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        val response = chain.proceed(request)
        // Token vencido o revocado: se borra la sesión y la app vuelve al login
        if (response.code == 401 && token != null) {
            session.clear()
        }
        response
    }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .build()

    private var cachedUrl: String? = null
    private var cachedService: ApiService? = null

    @Synchronized
    fun service(): ApiService {
        val url = settings.baseUrl.value
        cachedService?.takeIf { url == cachedUrl }?.let { return it }
        val service = Retrofit.Builder()
            .baseUrl(url)
            .client(okHttp)
            .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
        cachedUrl = url
        cachedService = service
        return service
    }
}

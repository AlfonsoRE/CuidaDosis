package mx.cuidadosis.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Url

// Endpoints de la API. Las rutas son relativas a la URL base, que termina en "/".
interface ApiService {

    @GET
    suspend fun health(@Url url: String): Response<HealthResponse>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<MessageResponse>

    @GET("medications")
    suspend fun medications(): Response<MedicationListResponse>

    @GET("medications/{id}")
    suspend fun medication(@Path("id") id: Int): Response<MedicationResponse>

    @POST("medications")
    suspend fun createMedication(@Body body: MedicationRequest): Response<MedicationResponse>

    @PUT("medications/{id}")
    suspend fun updateMedication(
        @Path("id") id: Int,
        @Body body: MedicationRequest,
    ): Response<MedicationResponse>

    @DELETE("medications/{id}")
    suspend fun deactivateMedication(@Path("id") id: Int): Response<MessageResponse>

    @GET("me")
    suspend fun me(): Response<MeResponse>

    // Tomas del paciente
    @GET("intakes/today")
    suspend fun todayIntakes(): Response<TodayResponse>

    @POST("intakes/{id}/take")
    suspend fun takeIntake(@Path("id") id: Int): Response<IntakeResponse>

    // Vinculación
    @POST("links/invite")
    suspend fun invite(): Response<InviteDto>

    @POST("links/accept")
    suspend fun accept(@Body body: AcceptRequest): Response<AcceptResponse>

    @DELETE("links/active")
    suspend fun unlinkCaregiver(): Response<MessageResponse>

    // Vista del cuidador
    @GET("caregiver/patients")
    suspend fun caregiverPatients(): Response<CaregiverPatientsResponse>

    @GET("caregiver/patients/{id}/intakes/today")
    suspend fun caregiverPatientToday(@Path("id") id: Int): Response<TodayResponse>

    @DELETE("caregiver/patients/{id}")
    suspend fun unlinkPatient(@Path("id") id: Int): Response<MessageResponse>
}

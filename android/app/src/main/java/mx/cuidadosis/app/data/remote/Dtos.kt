package mx.cuidadosis.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Modelos JSON de la API (ver docs/api.md)

@Serializable
data class UserDto(
    val id: Int,
    @SerialName("full_name") val fullName: String,
    val email: String,
    val role: String,
)

@Serializable
data class AuthResponse(
    val user: UserDto,
    val token: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(
    @SerialName("full_name") val fullName: String,
    val email: String,
    val password: String,
    val role: String,
)

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class HealthResponse(val status: String, val service: String? = null)

@Serializable
data class MedicationDto(
    val id: Int,
    val name: String,
    val dose: String,
    val times: List<String>,
)

@Serializable
data class MedicationListResponse(val medications: List<MedicationDto>)

@Serializable
data class MedicationResponse(val medication: MedicationDto)

@Serializable
data class MedicationRequest(val name: String, val dose: String, val times: List<String>)

@Serializable
data class ErrorResponse(val error: String, val details: Map<String, String>? = null)

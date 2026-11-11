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

// Tomas del día

@Serializable
data class IntakeDto(
    val id: Int,
    @SerialName("medication_id") val medicationId: Int,
    val name: String,
    val dose: String,
    val time: String,
    @SerialName("scheduled_for") val scheduledFor: String,
    val status: String,
    @SerialName("confirmed_at") val confirmedAt: String? = null,
) {
    val isTaken: Boolean get() = status == "taken"
}

@Serializable
data class IntakeSummary(val total: Int, val taken: Int, val pending: Int)

@Serializable
data class PersonRef(
    val id: Int,
    @SerialName("full_name") val fullName: String,
)

@Serializable
data class TodayResponse(
    val date: String,
    val intakes: List<IntakeDto>,
    val summary: IntakeSummary,
    val patient: PersonRef? = null,
)

@Serializable
data class IntakeResponse(val intake: IntakeDto)

// Vinculación

@Serializable
data class InviteDto(
    @SerialName("invite_code") val inviteCode: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class AcceptRequest(@SerialName("invite_code") val inviteCode: String)

@Serializable
data class AcceptResponse(
    val patient: PersonRef,
    @SerialName("linked_at") val linkedAt: String,
)

@Serializable
data class LinkDto(
    val caregiver: PersonRef? = null,
    val patient: PersonRef? = null,
    @SerialName("linked_at") val linkedAt: String,
)

@Serializable
data class LinkedPatientDto(
    val id: Int,
    @SerialName("full_name") val fullName: String,
    @SerialName("linked_at") val linkedAt: String,
)

@Serializable
data class MeResponse(
    val user: UserDto,
    val link: LinkDto? = null,
    @SerialName("pending_invite") val pendingInvite: InviteDto? = null,
    val patients: List<LinkedPatientDto>? = null,
)

// Vista del cuidador

@Serializable
data class CaregiverPatientDto(
    val id: Int,
    @SerialName("full_name") val fullName: String,
    @SerialName("linked_at") val linkedAt: String,
    val summary: IntakeSummary,
)

@Serializable
data class CaregiverPatientsResponse(val date: String, val patients: List<CaregiverPatientDto>)

@Serializable
data class ErrorResponse(val error: String, val details: Map<String, String>? = null)

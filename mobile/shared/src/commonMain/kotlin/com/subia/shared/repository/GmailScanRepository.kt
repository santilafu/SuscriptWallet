package com.subia.shared.repository

import com.subia.shared.model.EmptyBody
import com.subia.shared.model.GmailAddRequest
import com.subia.shared.model.GmailAddResult
import com.subia.shared.model.GmailDetected
import com.subia.shared.model.GmailScanTicketResponse
import com.subia.shared.network.ApiClient
import com.subia.shared.network.ApiRoutes

/** Acceso a los endpoints REST de detección de suscripciones por Gmail. */
interface GmailScanRepository {
    /** Pide el ticket y devuelve la URL de consentimiento a abrir en Custom Tab. */
    suspend fun requestTicket(months: Int): Result<GmailScanTicketResponse>
    suspend fun getResults(): Result<List<GmailDetected>>
    suspend fun add(ids: List<Long>): Result<GmailAddResult>
}

class GmailScanRepositoryImpl(private val apiClient: ApiClient) : GmailScanRepository {

    override suspend fun requestTicket(months: Int): Result<GmailScanTicketResponse> =
        apiClient.post(ApiRoutes.gmailScanTicket(months), EmptyBody)

    override suspend fun getResults(): Result<List<GmailDetected>> =
        apiClient.get(ApiRoutes.GMAIL_SCAN_RESULTS)

    override suspend fun add(ids: List<Long>): Result<GmailAddResult> =
        apiClient.post(ApiRoutes.GMAIL_SCAN_ADD, GmailAddRequest(ids))
}

package com.subia.android.auth

import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.subia.android.BuildConfig
import kotlinx.coroutines.CancellationException

sealed class GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult()
    object UserCancelled : GoogleSignInResult()
    object NoGoogleAccounts : GoogleSignInResult()
    object NotConfigured : GoogleSignInResult()
    /**
     * Fallo no previsto. [detalle] es el mensaje técnico de la excepción (si lo hay), que la UI
     * ya mostraba; sin él, la UI usa un texto genérico localizado.
     */
    data class Unknown(val detalle: String?) : GoogleSignInResult()
}

object GoogleSignInHelper {
    suspend fun signIn(activity: android.app.Activity): GoogleSignInResult {
        if (BuildConfig.SUBIA_GOOGLE_WEB_CLIENT_ID.isEmpty()) {
            return GoogleSignInResult.NotConfigured
        }
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.SUBIA_GOOGLE_WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val credentialManager = CredentialManager.create(activity)
            val response = credentialManager.getCredential(activity, request)
            val googleCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
            GoogleSignInResult.Success(googleCredential.idToken)
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult.UserCancelled
        } catch (e: NoCredentialException) {
            GoogleSignInResult.NoGoogleAccounts
        } catch (e: GetCredentialException) {
            GoogleSignInResult.Unknown(e.message)
        } catch (e: CancellationException) {
            // La corrutina se canceló (p. ej. se salió de la pantalla): no es un error que mostrar.
            throw e
        } catch (e: Throwable) {
            GoogleSignInResult.Unknown(e.message)
        }
    }
}

package com.subia.android.auth

import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.subia.android.BuildConfig

sealed class GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult()
    object UserCancelled : GoogleSignInResult()
    object NoGoogleAccounts : GoogleSignInResult()
    object NotConfigured : GoogleSignInResult()
    data class Unknown(val message: String) : GoogleSignInResult()
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
            GoogleSignInResult.Unknown(e.message ?: "Error de Google Sign-In")
        } catch (e: Throwable) {
            GoogleSignInResult.Unknown(e.message ?: "Error inesperado")
        }
    }
}

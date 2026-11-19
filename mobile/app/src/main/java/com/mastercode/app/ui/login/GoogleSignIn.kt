package com.mastercode.app.ui.login

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

sealed interface ResultadoGoogle {
    // El token de identidad que firma Google; admin-api lo verifica antes de abrir la sesión.
    data class Token(val idToken: String) : ResultadoGoogle

    // La persona cerró el selector de cuentas: no es un error y no se avisa.
    data object Cancelado : ResultadoGoogle

    // No se pudo mostrar el selector (sin cuentas de Google, sin Google Play, sin permiso...).
    data object NoDisponible : ResultadoGoogle
}

// Abre el selector de cuentas de Google del teléfono. Hay que pasar el ID de cliente de tipo «Aplicación web» (el mismo que
// usa admin-api): el token sale emitido para ese cliente, y por eso admin-api lo acepta. El de Android solo se configura en
// Google Cloud con el paquete y el SHA-1, no va en el código.
suspend fun pedirTokenDeGoogle(contexto: Context, idDeClienteWeb: String): ResultadoGoogle {
    val opcion = GetSignInWithGoogleOption.Builder(idDeClienteWeb).build()
    val peticion = GetCredentialRequest.Builder().addCredentialOption(opcion).build()
    return try {
        val credencial = CredentialManager.create(contexto).getCredential(contexto, peticion).credential
        if (credencial is CustomCredential && credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            ResultadoGoogle.Token(GoogleIdTokenCredential.createFrom(credencial.data).idToken)
        } else {
            ResultadoGoogle.NoDisponible
        }
    } catch (e: GetCredentialCancellationException) {
        ResultadoGoogle.Cancelado
    } catch (e: GetCredentialException) {
        ResultadoGoogle.NoDisponible
    } catch (e: GoogleIdTokenParsingException) {
        ResultadoGoogle.NoDisponible
    }
}

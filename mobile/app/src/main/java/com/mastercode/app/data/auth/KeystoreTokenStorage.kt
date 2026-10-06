package com.mastercode.app.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// Cifra el refresh con una clave AES que vive en Android Keystore: la clave nunca sale
// del Keystore, así que quien copie el archivo de preferencias solo ve texto cifrado.
class KeystoreTokenStorage(context: Context) : TokenStorage {

    private val preferencias = context.getSharedPreferences("sesion", Context.MODE_PRIVATE)

    override fun guardarRefresh(refresh: String) {
        val cipher = Cipher.getInstance(TRANSFORMACION)
        // Al cifrar, Keystore genera un IV aleatorio nuevo; se guarda junto al texto cifrado.
        cipher.init(Cipher.ENCRYPT_MODE, clave())
        val cifrado = cipher.doFinal(refresh.toByteArray())
        // commit() y no apply(): con la rotación el refresh anterior ya no sirve en el servidor,
        // así que el nuevo debe quedar escrito antes de seguir.
        preferencias.edit(commit = true) {
            putString(CLAVE_IV, Base64.getEncoder().encodeToString(cipher.iv))
            putString(CLAVE_REFRESH, Base64.getEncoder().encodeToString(cifrado))
        }
    }

    override fun leerRefresh(): String? {
        val iv = preferencias.getString(CLAVE_IV, null) ?: return null
        val cifrado = preferencias.getString(CLAVE_REFRESH, null) ?: return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMACION)
            cipher.init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(128, Base64.getDecoder().decode(iv)))
            String(cipher.doFinal(Base64.getDecoder().decode(cifrado)))
        } catch (e: GeneralSecurityException) {
            // La clave cambió o el dato fue alterado (GCM lo detecta): no se puede recuperar.
            borrar()
            null
        }
    }

    override fun borrar() {
        preferencias.edit(commit = true) { clear() }
    }

    private fun clave(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as SecretKey?)?.let { return it }

        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generador.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generador.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "refresh_token"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val CLAVE_IV = "iv"
        const val CLAVE_REFRESH = "refresh"
    }
}

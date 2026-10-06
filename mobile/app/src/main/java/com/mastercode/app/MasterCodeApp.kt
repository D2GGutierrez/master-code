package com.mastercode.app

import android.app.Application
import com.mastercode.app.data.auth.AuthRepository
import com.mastercode.app.data.auth.HttpAuthApi
import com.mastercode.app.data.auth.KeystoreTokenStorage

// Crea las dependencias a mano, una sola vez por proceso: así el access token en memoria
// no se pierde cuando la Activity se recrea (por ejemplo, al girar el teléfono).
class MasterCodeApp : Application() {
    val authRepository by lazy {
        AuthRepository(HttpAuthApi(BuildConfig.ADMIN_API_URL), KeystoreTokenStorage(this))
    }
}

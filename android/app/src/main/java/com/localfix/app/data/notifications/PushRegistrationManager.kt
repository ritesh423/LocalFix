package com.localfix.app.data.notifications

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.installations.FirebaseInstallations
import com.localfix.app.data.remote.PushRegistrationApi
import com.localfix.app.data.remote.PushRegistrationPayload
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class PushRole {
    RESIDENT,
    MANAGER,
    WORKER,
}

fun interface FirebaseInstallationProvider {
    fun requestInstallationId(onResult: (String?) -> Unit)
}

class DefaultFirebaseInstallationProvider : FirebaseInstallationProvider {
    override fun requestInstallationId(onResult: (String?) -> Unit) {
        runCatching { FirebaseInstallations.getInstance().id }
            .onSuccess { task ->
                task.addOnSuccessListener { installationId -> onResult(installationId) }
                    .addOnFailureListener { onResult(null) }
            }
            .onFailure { onResult(null) }
    }
}

interface PushRegistrationStore {
    var activeRole: PushRole?
    var latestFirebaseInstallationId: String?
    val installationId: String
}

class SharedPreferencesPushRegistrationStore(
    private val preferences: SharedPreferences,
) : PushRegistrationStore {
    override var activeRole: PushRole?
        get() = preferences.getString(ACTIVE_ROLE_KEY, null)
            ?.let { runCatching { PushRole.valueOf(it) }.getOrNull() }
        set(value) {
            preferences.edit { putString(ACTIVE_ROLE_KEY, value?.name) }
        }

    override var latestFirebaseInstallationId: String?
        get() = preferences.getString(FIREBASE_INSTALLATION_ID_KEY, null)
        set(value) {
            preferences.edit { putString(FIREBASE_INSTALLATION_ID_KEY, value) }
        }

    override val installationId: String
        get() = preferences.getString(INSTALLATION_ID_KEY, null)
            ?: UUID.randomUUID().toString().also { generated ->
                preferences.edit { putString(INSTALLATION_ID_KEY, generated) }
            }

    private companion object {
        const val ACTIVE_ROLE_KEY = "active_push_role"
        const val INSTALLATION_ID_KEY = "push_installation_id"
        const val FIREBASE_INSTALLATION_ID_KEY = "latest_firebase_installation_id"
    }
}

class PushRegistrationManager(
    private val api: PushRegistrationApi,
    private val store: PushRegistrationStore,
    private val firebaseInstallationProvider: FirebaseInstallationProvider,
    private val applicationScope: CoroutineScope,
) {
    fun activate(role: PushRole) {
        store.activeRole = role
        firebaseInstallationProvider.requestInstallationId { installationId ->
            if (installationId != null) refreshRegistration(installationId)
        }
    }

    fun refreshRegistration(firebaseInstallationId: String) {
        store.latestFirebaseInstallationId = firebaseInstallationId
        val role = store.activeRole ?: return
        applicationScope.launch {
            runCatching {
                api.registerPushDevice(
                    role = role.name.lowercase(),
                    request = PushRegistrationPayload(
                        installationId = store.installationId,
                        firebaseInstallationId = firebaseInstallationId,
                    ),
                )
            }
        }
    }
}

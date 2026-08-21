package com.localfix.app.data.notifications

import com.localfix.app.data.remote.PushRegistrationApi
import com.localfix.app.data.remote.PushRegistrationPayload
import com.localfix.app.data.remote.PushRegistrationResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PushRegistrationManagerTest {
    @Test
    fun selectedRoleAndChangedFirebaseIdsReuseTheSameInstallation() = runTest {
        val api = FakePushRegistrationApi()
        val store = FakePushRegistrationStore()
        val manager = PushRegistrationManager(
            api = api,
            store = store,
            firebaseInstallationProvider = FirebaseInstallationProvider { callback ->
                callback("first-firebase-installation")
            },
            applicationScope = backgroundScope,
        )

        manager.activate(PushRole.WORKER)
        runCurrent()
        manager.refreshRegistration("changed-firebase-installation")
        runCurrent()

        assertEquals(listOf("worker", "worker"), api.roles)
        assertEquals(
            listOf("first-firebase-installation", "changed-firebase-installation"),
            api.payloads.map(PushRegistrationPayload::firebaseInstallationId),
        )
        assertEquals(
            listOf("fixed-installation", "fixed-installation"),
            api.payloads.map(PushRegistrationPayload::installationId),
        )
    }

    private class FakePushRegistrationApi : PushRegistrationApi {
        val roles = mutableListOf<String>()
        val payloads = mutableListOf<PushRegistrationPayload>()

        override suspend fun registerPushDevice(
            role: String,
            request: PushRegistrationPayload,
        ): PushRegistrationResponse {
            roles += role
            payloads += request
            return PushRegistrationResponse(
                installationId = request.installationId,
                platform = request.platform,
                role = role,
            )
        }
    }

    private class FakePushRegistrationStore : PushRegistrationStore {
        override var activeRole: PushRole? = null
        override var latestFirebaseInstallationId: String? = null
        override val installationId = "fixed-installation"
    }
}

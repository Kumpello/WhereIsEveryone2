package com.kumpello.whereiseveryone.data.di

import com.kumpello.whereiseveryone.data.network.api.AuthApi
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.kumpello.whereiseveryone.data.repository.AuthenticationRepository
import com.kumpello.whereiseveryone.data.network.api.FriendsApi
import com.kumpello.whereiseveryone.data.repository.FriendsRepository
import com.kumpello.whereiseveryone.data.repository.LocationRepository
import com.kumpello.whereiseveryone.data.network.api.FriendApi
import com.kumpello.whereiseveryone.data.network.api.SharingApi
import com.kumpello.whereiseveryone.data.repository.FriendRepository
import com.kumpello.whereiseveryone.data.repository.SharingRepository
import com.kumpello.whereiseveryone.data.network.api.LocationApi
import com.kumpello.whereiseveryone.data.network.api.StatusApi
import com.kumpello.whereiseveryone.data.repository.StatusRepository
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import retrofit2.Retrofit

class NetworkModuleTest {
    @Test
    fun `app supplied server URL is used and all network bindings resolve`() {
        val baseUrl = "https://example.test/"
        val container = koinApplication {
            modules(
                networkModule(baseUrl),
                module { single { mockk<PreferencesManager>() } }
            )
        }
        try {
            val koin = container.koin
            assertEquals(baseUrl, koin.get<Retrofit>().baseUrl().toString())
            listOf(
                koin.get<AuthApi>(),
                koin.get<FriendsApi>(),
                koin.get<FriendApi>(),
                koin.get<SharingApi>(),
                koin.get<LocationApi>(),
                koin.get<StatusApi>(),
                koin.get<AuthenticationRepository>(),
                koin.get<FriendsRepository>(),
                koin.get<FriendRepository>(),
                koin.get<SharingRepository>(),
                koin.get<LocationRepository>(),
                koin.get<StatusRepository>()
            ).forEach(::assertNotNull)
        } finally {
            container.close()
        }
    }
}

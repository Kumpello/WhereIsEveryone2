package com.kumpello.whereiseveryone.common.di

import com.kumpello.whereiseveryone.common.domain.model.AuthApi
import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationRepository
import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationRepositoryImpl
import com.kumpello.whereiseveryone.common.domain.services.AuthInterceptor
import com.kumpello.whereiseveryone.common.domain.services.InstantAdapter
import com.kumpello.whereiseveryone.common.domain.services.RequestInterceptor
import com.kumpello.whereiseveryone.main.common.domain.model.FriendsApi
import com.kumpello.whereiseveryone.main.common.domain.repository.FriendsRepository
import com.kumpello.whereiseveryone.main.common.domain.repository.FriendsRepositoryImpl
import com.kumpello.whereiseveryone.main.common.domain.repository.LocationRepository
import com.kumpello.whereiseveryone.main.common.domain.repository.LocationRepositoryImpl
import com.kumpello.whereiseveryone.main.friends.domain.api.FriendApi
import com.kumpello.whereiseveryone.main.friends.domain.api.SharingApi
import com.kumpello.whereiseveryone.main.friends.domain.repository.FriendRepository
import com.kumpello.whereiseveryone.main.friends.domain.repository.FriendRepositoryImpl
import com.kumpello.whereiseveryone.main.friends.domain.repository.SharingRepository
import com.kumpello.whereiseveryone.main.friends.domain.repository.SharingRepositoryImpl
import com.kumpello.whereiseveryone.main.map.domain.api.LocationApi
import com.kumpello.whereiseveryone.main.map.domain.api.StatusApi
import com.kumpello.whereiseveryone.main.map.domain.repository.StatusRepository
import com.kumpello.whereiseveryone.main.map.domain.repository.StatusRepositoryImpl
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

private const val TEMP_BASE_URL = "http://192.168.1.216:8080/api/"

val networkModule = module {
    single {
        Moshi.Builder()
            .add(InstantAdapter)
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    single {
        AuthInterceptor(get())
    }

    single {
        OkHttpClient.Builder()
            .addInterceptor(RequestInterceptor)
            .addInterceptor(get<AuthInterceptor>())
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(TEMP_BASE_URL)
            .client(get())
            .addConverterFactory(MoshiConverterFactory.create(get()))
            .build()
    }

    single { get<Retrofit>().create(AuthApi::class.java) }
    single { get<Retrofit>().create(LocationApi::class.java) }
    single { get<Retrofit>().create(FriendsApi::class.java) }
    single { get<Retrofit>().create(FriendApi::class.java) }
    single { get<Retrofit>().create(StatusApi::class.java) }
    single { get<Retrofit>().create(SharingApi::class.java) }

    single { AuthenticationRepositoryImpl(get()) } bind AuthenticationRepository::class
    single { LocationRepositoryImpl(get()) } bind LocationRepository::class
    single { FriendsRepositoryImpl(get()) } bind FriendsRepository::class
    single { FriendRepositoryImpl(get()) } bind FriendRepository::class
    single { SharingRepositoryImpl(get()) } bind SharingRepository::class
    single { StatusRepositoryImpl(get()) } bind StatusRepository::class
}

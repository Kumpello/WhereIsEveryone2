package com.kumpello.whereiseveryone.data.di

import com.kumpello.whereiseveryone.data.network.api.AuthApi
import com.kumpello.whereiseveryone.data.repository.AuthenticationRepository
import com.kumpello.whereiseveryone.data.repository.AuthenticationRepositoryImpl
import com.kumpello.whereiseveryone.data.network.interceptor.AuthInterceptor
import com.kumpello.whereiseveryone.data.network.interceptor.RequestInterceptor
import com.kumpello.whereiseveryone.data.network.api.FriendsApi
import com.kumpello.whereiseveryone.data.repository.FriendsRepository
import com.kumpello.whereiseveryone.data.repository.FriendsRepositoryImpl
import com.kumpello.whereiseveryone.data.repository.LocationRepository
import com.kumpello.whereiseveryone.data.repository.LocationRepositoryImpl
import com.kumpello.whereiseveryone.data.network.api.FriendApi
import com.kumpello.whereiseveryone.data.network.api.SharingApi
import com.kumpello.whereiseveryone.data.repository.FriendRepository
import com.kumpello.whereiseveryone.data.repository.FriendRepositoryImpl
import com.kumpello.whereiseveryone.data.repository.SharingRepository
import com.kumpello.whereiseveryone.data.repository.SharingRepositoryImpl
import com.kumpello.whereiseveryone.data.network.api.LocationApi
import com.kumpello.whereiseveryone.data.network.api.StatusApi
import com.kumpello.whereiseveryone.data.repository.StatusRepository
import com.kumpello.whereiseveryone.data.repository.StatusRepositoryImpl
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

fun networkModule(baseUrl: String) = module {
    single {
        Moshi.Builder()
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
            .baseUrl(baseUrl)
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

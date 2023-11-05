package com.kumpello.whereiseveryone.common.domain.model

import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationService
import com.kumpello.whereiseveryone.map.domain.repository.PositionsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    fun provideAuthentication(): AuthenticationService {
        return AuthenticationService()
    }

    @Provides
    fun providesPositions(): PositionsService {
        return PositionsService()
    }
}
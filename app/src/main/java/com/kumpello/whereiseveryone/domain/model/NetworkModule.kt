package com.kumpello.whereiseveryone.domain.model

import com.kumpello.whereiseveryone.domain.usecase.AuthenticationService
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

/*    @Provides
    fun providesOrganizations(): OrganizationsService {
        return OrganizationsService()
    }*/
}
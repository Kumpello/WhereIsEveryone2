package com.kumpello.whereiseveryone.common.domain.usecase

import android.Manifest
import org.junit.Assert.assertTrue
import org.junit.Test

class GetNeededPermissionsUseCaseTest {

    private val useCase = GetNeededPermissionsUseCase()

    @Test
    fun `execute returns required location permissions`() {
        val permissions = useCase.execute()

        assertTrue(permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
        assertTrue(permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
    }
}

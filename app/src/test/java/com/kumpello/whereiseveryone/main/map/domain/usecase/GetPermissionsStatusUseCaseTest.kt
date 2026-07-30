package com.kumpello.whereiseveryone.main.map.domain.usecase

import android.content.Context
import android.content.pm.PackageManager
import com.kumpello.whereiseveryone.common.domain.usecase.GetNeededPermissionsUseCase
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class GetPermissionsStatusUseCaseTest {

    private val getNeededPermissionsUseCase: GetNeededPermissionsUseCase = mockk()
    private val context: Context = mockk()
    private val useCase = GetPermissionsStatusUseCase(getNeededPermissionsUseCase)

    @Test
    fun `execute returns map with permission statuses`() {
        val permissions = listOf("perm1", "perm2")
        every { getNeededPermissionsUseCase.execute() } returns permissions
        every { context.checkPermission("perm1", any(), any()) } returns PackageManager.PERMISSION_GRANTED
        every { context.checkPermission("perm2", any(), any()) } returns PackageManager.PERMISSION_DENIED

        val result = useCase.execute(context)

        assertEquals(2, result.size)
        assertEquals(true, result["perm1"])
        assertEquals(false, result["perm2"])
    }
}

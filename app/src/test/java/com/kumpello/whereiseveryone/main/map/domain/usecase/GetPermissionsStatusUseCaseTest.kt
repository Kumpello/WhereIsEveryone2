package com.kumpello.whereiseveryone.main.map.domain.usecase

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.kumpello.whereiseveryone.common.domain.usecase.GetNeededPermissionsUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertEquals
import org.junit.Test

class GetPermissionsStatusUseCaseTest {

    private val getNeededPermissionsUseCase: GetNeededPermissionsUseCase = mockk()
    private val context: Context = mockk()
    private val useCase = GetPermissionsStatusUseCase(getNeededPermissionsUseCase)

    @Test
    fun `execute returns map with permission statuses`() {
        mockkStatic(ContextCompat::class)
        val permissions = listOf("perm1", "perm2")
        every { getNeededPermissionsUseCase.execute() } returns permissions
        every { ContextCompat.checkSelfPermission(context, "perm1") } returns PackageManager.PERMISSION_GRANTED
        every { ContextCompat.checkSelfPermission(context, "perm2") } returns PackageManager.PERMISSION_DENIED

        val result = useCase.execute(context)

        assertEquals(2, result.size)
        assertEquals(true, result["perm1"])
        assertEquals(false, result["perm2"])
        unmockkStatic(ContextCompat::class)
    }
}

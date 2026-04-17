package com.kumpello.whereiseveryone.main.map.domain.usecase

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.kumpello.whereiseveryone.common.domain.ucecase.GetNeededPermissionsUseCase

class GetPermissionsStatusUseCase(val getNeededPermissionsUseCase: GetNeededPermissionsUseCase) {

    fun execute(context: Context): Map<String, Boolean> {
        val permissions: MutableMap<String, Boolean> = mutableMapOf()
        getNeededPermissionsUseCase.execute().forEach { permission ->
            permissions[permission] = checkPermission(context, permission)
        }
        return permissions.toMap()
    }

    private fun checkPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}
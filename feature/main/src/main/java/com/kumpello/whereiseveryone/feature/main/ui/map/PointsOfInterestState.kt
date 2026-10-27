package com.kumpello.whereiseveryone.feature.main.ui.map

import androidx.annotation.StringRes
import com.kumpello.whereiseveryone.data.model.PointOfInterest
import com.kumpello.whereiseveryone.data.model.isValidPointText
import com.kumpello.whereiseveryone.data.network.model.PointOfInterestRequest

internal fun PointDraft.request() = PointOfInterestRequest(name.trim(), description.trim(), latitude, longitude)

internal fun PointDraft.isValid() = isValidPointText(name, description) &&
    latitude.isFinite() && latitude in -90.0..90.0 && longitude.isFinite() && longitude in -180.0..180.0

data class PointDraft(
    val latitude: Double,
    val longitude: Double,
    val name: String = "",
    val description: String = "",
    val editingId: String? = null,
)

data class PointsOfInterestState(
    val ownPoints: List<PointOfInterest> = emptyList(),
    val isLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val showManager: Boolean = false,
    val showLayer: Boolean = true,
    val selectedId: String? = null,
    val navigatingId: String? = null,
    val draft: PointDraft? = null,
    val deleteConfirmationId: String? = null,
    @param:StringRes val error: Int? = null,
)

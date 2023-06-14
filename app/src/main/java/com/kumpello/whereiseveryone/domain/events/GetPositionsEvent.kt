package com.kumpello.whereiseveryone.domain.events

import com.kumpello.whereiseveryone.data.model.ErrorData
import com.kumpello.whereiseveryone.data.model.map.PositionsResponse

sealed class GetPositionsEvent {
    data class GetSuccess(val organizationsData: PositionsResponse): GetPositionsEvent()
    data class GetError(val error: ErrorData) : GetPositionsEvent()
}

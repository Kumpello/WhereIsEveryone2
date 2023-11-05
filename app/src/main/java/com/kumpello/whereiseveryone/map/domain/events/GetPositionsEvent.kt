package com.kumpello.whereiseveryone.map.domain.events

import com.kumpello.whereiseveryone.common.data.model.ErrorData
import com.kumpello.whereiseveryone.map.data.model.PositionsResponse

sealed class GetPositionsEvent {
    data class GetSuccess(val organizationsData: PositionsResponse): GetPositionsEvent()
    data class GetError(val error: ErrorData) : GetPositionsEvent()
}

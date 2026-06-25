package com.kumpello.whereiseveryone.main.common.domain.usecase

import com.kumpello.whereiseveryone.main.common.entity.AccuracyLevel
import com.kumpello.whereiseveryone.main.common.entity.AltDifference
import com.kumpello.whereiseveryone.main.common.entity.FriendLocalData
import com.kumpello.whereiseveryone.main.common.entity.FriendState
import com.kumpello.whereiseveryone.main.common.entity.LastUpdateAge
import com.kumpello.whereiseveryone.main.common.entity.LocationData
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Clock

class MapFriendUseCaseTest {

    private val calculateDistanceUseCase: CalculateDistanceUseCase = mockk()
    private val convertAltUseCase: ConvertAltUseCase = mockk()
    private val convertAccuracyUseCase: ConvertAccuracyUseCase = mockk()
    private val convertLastUpdateUseCase: ConvertLastUpdateUseCase = mockk()
    private val formatLastUpdateUseCase: FormatLastUpdateUseCase = mockk()
    
    private val useCase = MapFriendUseCase(
        calculateDistanceUseCase,
        convertAltUseCase,
        convertAccuracyUseCase,
        convertLastUpdateUseCase,
        formatLastUpdateUseCase
    )

    @Test
    fun `execute maps FriendLocalData to Friend correctly`() {
        val lastUpdate = Clock.System.now()
        val friendLocalData = FriendLocalData(
            username = "friend1",
            status = "status",
            state = FriendState.ACCEPTED,
            location = LocationData(1.0, 2.0, 0f, 3.0, 4f, lastUpdate)
        )
        val userLocation = LocationData(0.0, 0.0, 0f, 0.0, 0f, lastUpdate)

        every { calculateDistanceUseCase.execute(any(), any(), any(), any(), any(), any()) } returns 100.0
        every { convertAltUseCase.execute(any(), any()) } returns AltDifference.SOMEWHAT_SAME
        every { convertAccuracyUseCase.execute(any()) } returns AccuracyLevel.HIGH
        every { convertLastUpdateUseCase.execute(any(), any()) } returns LastUpdateAge.FRESH
        every { formatLastUpdateUseCase.execute(any()) } returns "1 min ago"

        val result = useCase.execute(friendLocalData, userLocation)

        assertEquals("friend1", result.username)
        assertEquals(100.0, result.distance!!, 0.001)
        assertEquals("1 min ago", result.location.lastUpdateTime)
    }
}

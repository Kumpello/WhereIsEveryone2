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
import org.junit.Assert.assertNull
import org.junit.Test

class MapFriendUseCaseTest {

    private val calculateDistanceUseCase: CalculateDistanceUseCase = mockk()
    private val convertAltUseCase: ConvertAltUseCase = mockk()
    private val convertAccuracyUseCase: ConvertAccuracyUseCase = mockk()
    private val convertLastUpdateUseCase: ConvertLastUpdateUseCase = mockk()
    private val formatLastUpdateUseCase: FormatLastUpdateUseCase = mockk()
    private val formatDateUseCase: FormatDateUseCase = mockk()
    
    private val useCase = MapFriendUseCase(
        calculateDistanceUseCase,
        convertAltUseCase,
        convertAccuracyUseCase,
        convertLastUpdateUseCase,
        formatLastUpdateUseCase,
        formatDateUseCase
    )

    @Test
    fun `execute maps FriendLocalData to Friend correctly`() {
        val lastUpdate = System.currentTimeMillis()
        val friendLocalData = FriendLocalData(
            username = "friend1",
            status = "status",
            state = FriendState.ACCEPTED,
            location = LocationData(1.0, 2.0, 0f, 3.0, 4f, lastUpdate),
            friendSince = lastUpdate
        )
        val userLocation = LocationData(0.0, 0.0, 0f, 0.0, 0f, lastUpdate)

        every { calculateDistanceUseCase.execute(any(), any(), any(), any(), any(), any()) } returns 100.0
        every { convertAltUseCase.execute(any(), any()) } returns AltDifference.SOMEWHAT_SAME
        every { convertAccuracyUseCase.execute(any()) } returns AccuracyLevel.HIGH
        every { convertLastUpdateUseCase.execute(any(), any()) } returns LastUpdateAge.FRESH
        every { formatLastUpdateUseCase.execute(any()) } returns "1 min ago"
        every { formatDateUseCase.execute(any()) } returns "2023-01-01"

        val result = useCase.execute(friendLocalData, userLocation)

        assertEquals("friend1", result.username)
        assertEquals(100.0, result.distance!!, 0.001)
        assertEquals("1 min ago", result.location?.lastUpdateTime)
        assertEquals("2023-01-01", result.friendSince)
    }

    @Test
    fun `execute handles null friend location`() {
        val lastUpdate = System.currentTimeMillis()
        val friendLocalData = FriendLocalData(
            username = "friend1",
            status = "status",
            state = FriendState.ACCEPTED,
            location = null,
            friendSince = null
        )
        val userLocation = LocationData(0.0, 0.0, 0f, 0.0, 0f, lastUpdate)

        val result = useCase.execute(friendLocalData, userLocation)

        assertEquals("friend1", result.username)
        assertNull(result.location)
        assertNull(result.distance)
    }
}

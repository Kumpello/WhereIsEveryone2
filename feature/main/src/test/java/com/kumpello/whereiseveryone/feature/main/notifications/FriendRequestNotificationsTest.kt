package com.kumpello.whereiseveryone.feature.main.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class FriendRequestNotificationsTest {
    @Test
    fun `new incoming names alert including a replacement with the same count`() {
        assertEquals(
            FriendRequestNotificationChange.Alert,
            friendRequestNotificationChange(emptySet(), setOf("alice"), false),
        )
        assertEquals(
            FriendRequestNotificationChange.Alert,
            friendRequestNotificationChange(setOf("alice"), setOf("bob"), true),
        )
    }

    @Test
    fun `unchanged requests do not notify again even after dismissal`() {
        for (isShowing in listOf(true, false)) {
            assertEquals(
                FriendRequestNotificationChange.None,
                friendRequestNotificationChange(setOf("alice", "bob"), setOf("bob", "alice"), isShowing),
            )
        }
    }

    @Test
    fun `accepting or rejecting updates remaining names quietly`() {
        assertEquals(
            FriendRequestNotificationChange.Update,
            friendRequestNotificationChange(setOf("alice", "bob"), setOf("bob"), true),
        )
    }

    @Test
    fun `removing names does not restore a dismissed notification`() {
        assertEquals(
            FriendRequestNotificationChange.None,
            friendRequestNotificationChange(setOf("alice", "bob"), setOf("bob"), false),
        )
    }

    @Test
    fun `no pending requests cancels the notification including one from a previous process`() {
        assertEquals(
            FriendRequestNotificationChange.Cancel,
            friendRequestNotificationChange(setOf("alice"), emptySet(), true),
        )
        assertEquals(
            FriendRequestNotificationChange.Cancel,
            friendRequestNotificationChange(emptySet(), emptySet(), true),
        )
        assertEquals(
            FriendRequestNotificationChange.None,
            friendRequestNotificationChange(emptySet(), emptySet(), false),
        )
    }
}

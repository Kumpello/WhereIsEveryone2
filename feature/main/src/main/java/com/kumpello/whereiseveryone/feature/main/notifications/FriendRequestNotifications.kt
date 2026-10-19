package com.kumpello.whereiseveryone.feature.main.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.kumpello.whereiseveryone.data.repository.FriendsStateRepository
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesKey
import com.kumpello.whereiseveryone.data.repository.preferences.PreferencesManager
import com.kumpello.whereiseveryone.feature.main.R
import com.kumpello.whereiseveryone.feature.main.navigation.MainActivityIntentFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

internal fun friendRequestNotificationChange(
    previous: Set<String>,
    current: Set<String>,
    isShowing: Boolean,
): FriendRequestNotificationChange = when {
    current.isEmpty() -> if (previous.isNotEmpty() || isShowing) FriendRequestNotificationChange.Cancel else FriendRequestNotificationChange.None
    current == previous -> FriendRequestNotificationChange.None
    (current - previous).isNotEmpty() -> FriendRequestNotificationChange.Alert
    isShowing -> FriendRequestNotificationChange.Update
    else -> FriendRequestNotificationChange.None
}

class FriendRequestNotifications(
    private val context: Context,
    private val friendsStateRepository: FriendsStateRepository,
    private val preferencesManager: PreferencesManager,
    private val mainActivityIntentFactory: MainActivityIntentFactory,
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private var previousRequests = emptySet<String>()

    suspend fun observe() = withContext(Dispatchers.Main.immediate) {
        combine(
            friendsStateRepository.incomingFriendRequests,
            preferencesManager.observe(PreferencesKey.AuthToken),
        ) { requests, token ->
            if (token.isNullOrBlank()) emptyList() else requests
        }.collect { requests -> update(requests) }
    }

    fun clear() {
        notificationManager.cancel(NOTIFICATION_ID)
        previousRequests = emptySet()
    }

    private fun update(requests: List<String>) {
        val current = requests.toSet()
        val isShowing = notificationManager.activeNotifications.any { it.id == NOTIFICATION_ID }
        val change = friendRequestNotificationChange(previousRequests, current, isShowing)
        when (change) {
            FriendRequestNotificationChange.None -> {
                previousRequests = current
                return
            }
            FriendRequestNotificationChange.Cancel -> {
                clear()
                return
            }
            FriendRequestNotificationChange.Alert, FriendRequestNotificationChange.Update -> Unit
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        if (!notificationManager.areNotificationsEnabled()) return

        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.friend_requests_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
        )
        if (notificationManager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE) return

        val names = current.sorted()
        val title = context.resources.getQuantityString(R.plurals.friend_requests_notification_title, names.size, names.size)
        val text = context.getString(R.string.friend_requests_notification_names, names.joinToString(", "))
        val intent = mainActivityIntentFactory.create(context)
            .setAction(MainActivityIntentFactory.ACTION_OPEN_FRIEND_REQUESTS)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_friend_request)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setNumber(names.size)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(change == FriendRequestNotificationChange.Update)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
        previousRequests = current
    }

    companion object {
        private const val CHANNEL_ID = "WhereIsEveryone_FriendRequests"
        private const val NOTIFICATION_ID = 423
    }
}

internal enum class FriendRequestNotificationChange {
    None,
    Alert,
    Update,
    Cancel,
}

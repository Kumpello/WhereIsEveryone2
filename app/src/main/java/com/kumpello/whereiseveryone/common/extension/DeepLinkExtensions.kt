package com.kumpello.whereiseveryone.common.extension

import android.net.Uri

const val APP_DOMAIN = "where-is-everyone.com"
const val ADD_FRIEND_PATH_PREFIX = "/addfriend"

fun Uri?.isAddFriendDeepLink(): Boolean {
    if (this == null) return false
    return scheme == "https" && host == APP_DOMAIN && path?.startsWith(ADD_FRIEND_PATH_PREFIX) == true
}

fun createAddFriendDeepLink(username: String): String {
    return "https://$APP_DOMAIN$ADD_FRIEND_PATH_PREFIX/$username"
}

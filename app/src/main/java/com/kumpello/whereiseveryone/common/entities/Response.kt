package com.kumpello.whereiseveryone.common.entities

sealed class Response {
    data object Success : Response()
    data object Error : Response()
}

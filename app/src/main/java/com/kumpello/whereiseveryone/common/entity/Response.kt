package com.kumpello.whereiseveryone.common.entity

sealed class Response {
    data object Success : Response()
    data object Error : Response()
}

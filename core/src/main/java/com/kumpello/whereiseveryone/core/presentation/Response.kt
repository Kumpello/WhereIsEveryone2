package com.kumpello.whereiseveryone.core.presentation

sealed class Response {
    data object Success : Response()
    data object Error : Response()
}

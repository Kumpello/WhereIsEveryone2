package com.kumpello.whereiseveryone.data.model

import com.kumpello.whereiseveryone.data.model.authorization.AuthResponse
import okhttp3.ResponseBody

class ErrorData(val errorBody: ResponseBody) : AuthResponse

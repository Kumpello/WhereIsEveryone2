package com.kumpello.whereiseveryone.data.model

import com.kumpello.whereiseveryone.data.model.authorization.AuthResponse
import okhttp3.ResponseBody

class ErrorData(val code : Int, val error : String, val message : String) : AuthResponse, com.kumpello.whereiseveryone.data.model.map.Response

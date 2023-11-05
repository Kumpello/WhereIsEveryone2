package com.kumpello.whereiseveryone.common.data.model

import com.kumpello.whereiseveryone.common.data.model.authorization.AuthResponse
import com.kumpello.whereiseveryone.map.data.model.Response

class ErrorData(val code : Int, val error : String, val message : String) : AuthResponse, Response

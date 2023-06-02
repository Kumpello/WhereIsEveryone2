package com.kumpello.whereiseveryone.data.model.authorization

import com.kumpello.poker.data.model.organizations.OrganizationsResponse
import okhttp3.ResponseBody

class ErrorData(val errorBody: ResponseBody) : AuthResponse

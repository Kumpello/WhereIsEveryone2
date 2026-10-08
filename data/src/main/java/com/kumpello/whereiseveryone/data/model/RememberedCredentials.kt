package com.kumpello.whereiseveryone.data.model

import kotlinx.serialization.Serializable

// A regular class deliberately avoids a generated toString containing the password.
@Serializable
class RememberedCredentials(val username: String, val password: String)

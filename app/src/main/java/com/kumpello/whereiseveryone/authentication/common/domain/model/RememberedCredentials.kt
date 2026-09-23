package com.kumpello.whereiseveryone.authentication.common.domain.model

import kotlinx.serialization.Serializable

// A regular class deliberately avoids a generated toString containing the password.
@Serializable
class RememberedCredentials(val username: String, val password: String)

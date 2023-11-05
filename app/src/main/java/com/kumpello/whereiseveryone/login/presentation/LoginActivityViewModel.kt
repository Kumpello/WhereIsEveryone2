package com.kumpello.whereiseveryone.login.presentation

import androidx.lifecycle.ViewModel
import com.kumpello.whereiseveryone.common.domain.repository.AuthenticationService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginActivityViewModel @Inject constructor(val authenticationService: AuthenticationService) : ViewModel() {
}
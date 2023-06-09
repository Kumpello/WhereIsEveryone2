package com.kumpello.whereiseveryone.ui.login

import androidx.lifecycle.ViewModel
import com.kumpello.whereiseveryone.domain.usecase.AuthenticationService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginActivityViewModel @Inject constructor(val authenticationService: AuthenticationService) : ViewModel() {
}
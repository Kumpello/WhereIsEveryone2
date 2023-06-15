package com.kumpello.whereiseveryone.ui.main.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.domain.usecase.AuthenticationService
import com.kumpello.whereiseveryone.ui.login.LoginActivity
import com.kumpello.whereiseveryone.ui.main.MainActivityViewModel
import com.kumpello.whereiseveryone.ui.theme.WhereIsEveryoneTheme

@Composable
fun Map(navController: NavHostController, viewModel: MainActivityViewModel) {
    val mContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val application = mContext.applicationContext as WhereIsEveryoneApplication

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

    }
}

@Preview(showBackground = true)
@Composable
fun MapPreview() {
    WhereIsEveryoneTheme {
        Map(rememberNavController(), AuthenticationService(), LoginActivity())
    }
}
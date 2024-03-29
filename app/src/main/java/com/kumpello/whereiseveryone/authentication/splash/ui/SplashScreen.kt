package com.kumpello.whereiseveryone.authentication.splash.ui

import android.content.Intent
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumpello.whereiseveryone.authentication.AuthenticationNavGraph
import com.kumpello.whereiseveryone.authentication.splash.presentation.SplashViewModel
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.destinations.SignUpScreenDestination
import com.kumpello.whereiseveryone.main.MainActivity
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay

@AuthenticationNavGraph(start = true)
@Destination
@Composable
fun SplashScreen(
    navigator: DestinationsNavigator,
    viewModel: SplashViewModel
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        animateLogo(state)
        delay(2000)
        viewModel.trigger(SplashViewModel.Command.NavigateToNextDestination)
    }

    LaunchedEffect(viewModel.action) {
        viewModel.action.collect { action ->
            when (action) {
                SplashViewModel.Action.NavigateMain -> context.startActivity(Intent(context, MainActivity::class.java))
                SplashViewModel.Action.NavigateSignUp -> navigator.navigate(SignUpScreenDestination)
            }
        }
    }

    SplashScreen(
        viewState = state
    )
}

@Composable
fun SplashScreen(
    viewState: SplashViewModel.ViewState,
) {
    Column(
        modifier = Modifier.padding(vertical = 100.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .padding(15.dp)
                .size(330.dp)
                .scale(viewState.scale.value),
            shape = CircleShape,
            border = BorderStroke(width = 2.dp, color = Color.LightGray)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppLogo()
                Text(
                    text = "Find your friends!",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Text(
        text = "Where is everyone!?",
        modifier.padding(bottom = 16.dp),
        style = MaterialTheme.typography.headlineMedium,
        color = Color.Red.copy(0.5f),
        textAlign = TextAlign.Center,
        fontSize = 40.sp
    )
}

private suspend fun animateLogo(state: SplashViewModel.ViewState) {
    //TODO: This needs to be changed, maybe scale should be moved here?
    state.scale.animateTo(
        targetValue = 0.9f,
        animationSpec = tween(
            durationMillis = 1200,
            easing = {
                OvershootInterpolator(2f).getInterpolation(it)
            })
    )
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    val scale by remember { mutableStateOf(Animatable(0.7f)) }

    WhereIsEveryoneTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            SplashScreen(
                viewState = SplashViewModel.ViewState(
                    scale = scale
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreviewDark() {
    val scale by remember { mutableStateOf(Animatable(0.7f)) }

    WhereIsEveryoneTheme(true) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            SplashScreen(
                viewState = SplashViewModel.ViewState(
                    scale = scale
                )
            )
        }
    }
}
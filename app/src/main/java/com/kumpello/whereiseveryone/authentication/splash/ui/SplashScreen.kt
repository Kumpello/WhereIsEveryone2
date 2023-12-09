package com.kumpello.whereiseveryone.authentication.splash.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.kumpello.whereiseveryone.authentication.splash.presentation.SplashViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay

@RootNavGraph(start = true)
@Destination
@Composable
fun SplashScreen(
    navigator: DestinationsNavigator,
    viewModel: SplashViewModel
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(key1 = true) {
        viewModel.trigger(SplashViewModel.Command.AnimateLogo)
        delay(2000)
        //navigator
    }

    SplashScreen(
        viewState = state,
        trigger = viewModel::trigger
    )
}

@Composable
fun SplashScreen(
    viewState: SplashViewModel.ViewState,
    trigger: (SplashViewModel.Command) -> Unit,
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
            modifier = Modifier.padding(1.dp),
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

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Text(
        text = "Where is everyone!?",
        modifier.padding(bottom = 16.dp),
        style = MaterialTheme.typography.headlineMedium,
        color = Color.Red.copy(0.5f),
        fontSize = 40.sp
    )
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    var scale by remember { mutableStateOf(Animatable(0f)) }

    WhereIsEveryoneTheme {
        SplashScreen(
            viewState = SplashViewModel.ViewState(
                scale = scale
            )
        ) {}
    }
}
package com.kumpello.whereiseveryone.login.ui.screens

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.kumpello.whereiseveryone.ui.navigation.LoginRoutes
import com.kumpello.whereiseveryone.common.ui.theme.WhereIsEveryoneTheme
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.delay

@Destination
@Composable
fun Splash(navController: NavHostController) {
    val scale = remember {
        Animatable(0f)
    }

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 0.9f,
            animationSpec = tween(
                durationMillis = 800,
                easing = {
                    OvershootInterpolator(2f).getInterpolation(it)
                })
        )
        delay(2000)
        navController.navigate(LoginRoutes.Login.route)
    }

    Surface(
        modifier = Modifier
            .padding(15.dp)
            .size(330.dp)
            .scale(scale.value),
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
                text = "Read, Learn, Repeat",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.LightGray
            )
        }
    }
}

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Text(
        text = "Nav With Enum",
        modifier.padding(bottom = 16.dp),
        style = MaterialTheme.typography.headlineMedium,
        color = Color.Red.copy(0.5f),
        fontSize = 40.sp
    )
}

@Preview(showBackground = true)
@Composable
fun SplashPreview() {
    WhereIsEveryoneTheme {
        Splash(rememberNavController())
    }
}
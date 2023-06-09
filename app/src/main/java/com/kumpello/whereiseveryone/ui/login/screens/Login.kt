package com.kumpello.whereiseveryone.ui.login.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.kumpello.whereiseveryone.app.WhereIsEveryoneApplication
import com.kumpello.whereiseveryone.data.model.authorization.AuthData
import com.kumpello.whereiseveryone.domain.usecase.AuthenticationService
import com.kumpello.whereiseveryone.ui.login.LoginActivity
import com.kumpello.whereiseveryone.ui.main.MainActivity
import com.kumpello.whereiseveryone.ui.navigation.LoginRoutes
import com.kumpello.whereiseveryone.ui.theme.WhereIsEveryoneTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

@Composable
fun Login(navController: NavHostController, authService: AuthenticationService, activity: LoginActivity) {
    val mContext = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val application = mContext.applicationContext as WhereIsEveryoneApplication

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        //ToDo add validation!
        val username = remember { mutableStateOf(TextFieldValue()) }
        val password = remember { mutableStateOf(TextFieldValue()) }

        Text(text = "Login", style = TextStyle(fontSize = 40.sp, fontFamily = FontFamily.Default))

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            label = { Text(text = "Username") },
            value = username.value,
            onValueChange = { username.value = it })

        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            label = { Text(text = "Password") },
            value = password.value,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            onValueChange = { password.value = it })

        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.padding(40.dp, 0.dp, 40.dp, 0.dp)) {
            Button(
                onClick = {
                    var response: Optional<AuthData>?
                    coroutineScope.launch(Dispatchers.IO){
                        response = authService.logIn(username.value.text, password.value.text)
                        withContext(Dispatchers.Main){
                            if (response != null) {
                                application.saveUserID(response!!.get().id)
                                application.saveUserName(username.value.text)
                                application.saveAuthToken(response!!.get().token)
                                application.saveAuthRefreshToken(response!!.get().refresh_token)

                                activity.makeToast(mContext, "Login succeeded!")
                                mContext.startActivity(Intent(mContext, MainActivity::class.java))
                            } else {
                                activity.makeToast(mContext, "Login failed!")
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(50.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "Login")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            ClickableText(
                text = AnnotatedString("Sign up here"),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(20.dp),
                onClick = { navController.navigate(LoginRoutes.SignUp.route) },
                style = TextStyle(
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Default,
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colors.primaryVariant
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    WhereIsEveryoneTheme {
        Login(rememberNavController(), AuthenticationService(), LoginActivity())
    }
}
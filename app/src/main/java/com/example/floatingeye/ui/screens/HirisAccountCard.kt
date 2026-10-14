package com.example.floatingeye.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.floatingeye.auth.HirisAuth
import com.example.floatingeye.auth.HirisUser
import com.example.ui.theme.CyanNeonPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.launch

@Composable
fun HirisAccountCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var user by remember { mutableStateOf<HirisUser?>(null) }
    var isSignup by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isCheckingSession by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(context) {
        val token = HirisAuth.getToken(context)
        if (token != null) {
            try {
                user = HirisAuth.getMe(token)
                if (user == null) HirisAuth.logout(context)
            } catch (_: Exception) {
                errorMessage = "We couldn't verify your saved session. Check your connection and log in again."
            }
        }
        isCheckingSession = false
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1524)),
        border = BorderStroke(1.dp, Color(0xFF1A263D))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("HIRIS Account", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(
                "Konekte ak username ou oswa kreye yon kont.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (isCheckingSession || isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = CyanNeonPrimary,
                        strokeWidth = 2.dp
                    )
                    Text(
                        if (isCheckingSession) "Checking account…" else "Connecting…",
                        color = TextSecondary
                    )
                }
            } else if (user != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.AccountCircle,
                        contentDescription = null,
                        tint = CyanNeonPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user!!.username, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = user!!.email,
                            color = CyanNeonPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text("Connected account", color = TextTertiary, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            HirisAuth.logout(context)
                            user = null
                            username = ""
                            password = ""
                            errorMessage = null
                        }
                    ) { Text("Log out") }
                }
            } else {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.filterNot(Char::isWhitespace) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Username") },
                    placeholder = { Text("e.g. pierrejean") },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    colors = accountFieldColors()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = accountFieldColors()
                )
                Text(
                    "Antre username sèlman; pa ajoute @fbox.com.",
                    color = TextTertiary,
                    style = MaterialTheme.typography.bodySmall
                )
                errorMessage?.let {
                    Text(it, color = Color(0xFFFF8585), style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = {
                        val enteredUsername = username.trim()
                        if (enteredUsername.isBlank() || password.isBlank()) {
                            errorMessage = "Antre username ak password ou."
                        } else {
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    val result = if (isSignup) {
                                        HirisAuth.signup(enteredUsername, password)
                                    } else {
                                        HirisAuth.login(enteredUsername, password)
                                    }
                                    HirisAuth.saveToken(context, result.token)
                                    user = result.user
                                    password = ""
                                } catch (error: Exception) {
                                    errorMessage = error.message ?: "Connection failed. Try again."
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanNeonPrimary,
                        contentColor = Color(0xFF06111A)
                    )
                ) {
                    Text(if (isSignup) "Create account" else "Log in", fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = {
                        isSignup = !isSignup
                        errorMessage = null
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(if (isSignup) "Already have an account? Log in" else "New to HIRIS? Create an account")
                }
            }
        }
    }
}

@Composable
private fun accountFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanNeonPrimary,
    unfocusedBorderColor = Color(0xFF26364E),
    focusedLabelColor = CyanNeonPrimary,
    cursorColor = CyanNeonPrimary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)

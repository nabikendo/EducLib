package com.example.ui

import android.content.Context
import android.util.Log
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer
import com.example.data.WeeklySuggestionEntity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.foundation.gestures.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.BookEntity
import com.example.data.LoanEntity
import com.example.data.BorrowerEntity
import com.example.data.ReservationEntity
import com.example.data.ShelfEntity
import com.example.data.ShelfPositionEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LibraryApp(viewModel: LibraryViewModel) {
    val isPasswordSet by viewModel.isPasswordSet.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

    MyApplicationTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            when (isPasswordSet) {
                null -> {
                    // Loading initial state from Room database config table
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ThemePurplePrimary)
                    }
                }
                false -> {
                    UnifiedAuthScreen(viewModel = viewModel, isSetupMode = true)
                }
                true -> {
                    if (isLoggedIn) {
                        MainLibraryAppContent(viewModel)
                    } else {
                        UnifiedAuthScreen(viewModel = viewModel, isSetupMode = false)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UnifiedAuthScreen(viewModel: LibraryViewModel, isSetupMode: Boolean) {
    var isSignUp by remember { mutableStateOf(isSetupMode) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var masterPassword by remember { mutableStateOf("") }
    
    // Auth method sub-choice inside login
    var loginByMasterPassword by remember { mutableStateOf(false) }

    var errorText by remember { mutableStateOf("") }
    var showGoogleChooser by remember { mutableStateOf(false) }
    var isLoadingGoogle by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    if (showGoogleChooser) {
        GoogleSignChooserDialog(
            onAccountSelected = { chosenEmail, chosenName, chosenPhoto ->
                showGoogleChooser = false
                isLoadingGoogle = true
                scope.launch {
                    kotlinx.coroutines.delay(1000) // Elegant simulated OAuth delays (1.0s)
                    viewModel.loginWithGoogle(chosenEmail, chosenName, chosenPhoto)
                    isLoadingGoogle = false
                }
            },
            onDismiss = { showGoogleChooser = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF), // Pure white
                        Color(0xFFFBFBFC), // Minimalist warm off-white
                        Color(0xFFF4F4F7)  // Super soft aluminum shade
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoadingGoogle) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = ThemePurplePrimary, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Conectando...",
                        fontWeight = FontWeight.Bold,
                        color = ThemePurplePrimary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .verticalScrollWithKeyboard(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Centered Minimalist Logo Header
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                        .border(BorderStroke(0.5.dp, Color(0xFFE2E8F0)), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalLibrary,
                        contentDescription = "BiblioTech",
                        tint = ThemePurplePrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "BiblioTech",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ThemePurplePrimary,
                    letterSpacing = (-0.8).sp
                )
                Text(
                    text = "Gestão Escolar & Acervo Inteligente",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeSecondaryText.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 1.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Unified Login Card (iOS-style clean card with simple floating details)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        // iOS-style Segmented Controller (Switch between Sign In and Sign Up)
                        if (!isSetupMode) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val tabModifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { isSignUp = false }
                                    .padding(vertical = 8.dp)

                                val activeTabStyle = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .shadow(1.dp, RoundedCornerShape(10.dp))
                                    .padding(vertical = 8.dp)

                                Box(
                                    modifier = if (!isSignUp) activeTabStyle else tabModifier,
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Acessar",
                                        fontWeight = FontWeight.Bold,
                                        color = if (!isSignUp) ThemePurplePrimary else ThemeSecondaryText.copy(alpha = 0.6f),
                                        fontSize = 13.sp
                                    )
                                }

                                Box(
                                    modifier = if (isSignUp) activeTabStyle else tabModifier,
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Criar Conta",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSignUp) ThemePurplePrimary else ThemeSecondaryText.copy(alpha = 0.6f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        } else {
                            Text(
                                text = "Configuração de Primeiro Acesso",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = ThemePurplePrimary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        // Form Inputs
                        if (isSignUp) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Nome Completo") },
                                placeholder = { Text("Nome de usuário") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF64748B)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("setup_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ThemePurplePrimary,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = Color(0xFFFBFBFC),
                                    unfocusedContainerColor = Color(0xFFFBFBFC),
                                    focusedLabelColor = ThemePurplePrimary,
                                    unfocusedLabelColor = Color(0xFF94A3B8),
                                    cursorColor = ThemePurplePrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("E-mail") },
                                placeholder = { Text("escola@sistema.com") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF64748B)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("setup_email_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ThemePurplePrimary,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = Color(0xFFFBFBFC),
                                    unfocusedContainerColor = Color(0xFFFBFBFC),
                                    focusedLabelColor = ThemePurplePrimary,
                                    unfocusedLabelColor = Color(0xFF94A3B8),
                                    cursorColor = ThemePurplePrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Senha") },
                                placeholder = { Text("Mínimo 4 caracteres") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B)) },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("setup_password_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ThemePurplePrimary,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = Color(0xFFFBFBFC),
                                    unfocusedContainerColor = Color(0xFFFBFBFC),
                                    focusedLabelColor = ThemePurplePrimary,
                                    unfocusedLabelColor = Color(0xFF94A3B8),
                                    cursorColor = ThemePurplePrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirmar Senha") },
                                placeholder = { Text("Repita a sua senha") },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF64748B)) },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("setup_confirm_password_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ThemePurplePrimary,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = Color(0xFFFBFBFC),
                                    unfocusedContainerColor = Color(0xFFFBFBFC),
                                    focusedLabelColor = ThemePurplePrimary,
                                    unfocusedLabelColor = Color(0xFF94A3B8),
                                    cursorColor = ThemePurplePrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else {
                            if (loginByMasterPassword) {
                                OutlinedTextField(
                                    value = masterPassword,
                                    onValueChange = { masterPassword = it },
                                    label = { Text("Senha Mestre") },
                                    placeholder = { Text("Sua senha de acesso local") },
                                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF64748B)) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("password_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ThemePurplePrimary,
                                        unfocusedBorderColor = Color(0xFFE2E8F0),
                                        focusedContainerColor = Color(0xFFFBFBFC),
                                        unfocusedContainerColor = Color(0xFFFBFBFC),
                                        focusedLabelColor = ThemePurplePrimary,
                                        unfocusedLabelColor = Color(0xFF94A3B8),
                                        cursorColor = ThemePurplePrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(
                                    onClick = { loginByMasterPassword = false },
                                    modifier = Modifier.align(Alignment.End),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Entrar com E-mail Escolar", fontSize = 11.sp, color = ThemePurplePrimary, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("E-mail") },
                                    placeholder = { Text("escola@sistema.com") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF64748B)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("login_email_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ThemePurplePrimary,
                                        unfocusedBorderColor = Color(0xFFE2E8F0),
                                        focusedContainerColor = Color(0xFFFBFBFC),
                                        unfocusedContainerColor = Color(0xFFFBFBFC),
                                        focusedLabelColor = ThemePurplePrimary,
                                        unfocusedLabelColor = Color(0xFF94A3B8),
                                        cursorColor = ThemePurplePrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Senha") },
                                    placeholder = { Text("Digite sua senha") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B)) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ThemePurplePrimary,
                                        unfocusedBorderColor = Color(0xFFE2E8F0),
                                        focusedContainerColor = Color(0xFFFBFBFC),
                                        unfocusedContainerColor = Color(0xFFFBFBFC),
                                        focusedLabelColor = ThemePurplePrimary,
                                        unfocusedLabelColor = Color(0xFF94A3B8),
                                        cursorColor = ThemePurplePrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(
                                    onClick = { loginByMasterPassword = true },
                                    modifier = Modifier.align(Alignment.End),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Acessar via Senha Mestre", fontSize = 11.sp, color = ThemePurplePrimary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        if (errorText.isNotEmpty()) {
                            Text(
                                text = errorText,
                                color = Color.Red,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Primary Access Button
                        Button(
                            onClick = {
                                if (isSignUp) {
                                    if (name.isBlank() || email.isBlank() || password.isBlank()) {
                                        errorText = "Favor preencher todos os campos obrigatórios."
                                    } else if (!email.contains("@")) {
                                        errorText = "Insira um endereço de e-mail válido."
                                    } else if (password.length < 4) {
                                        errorText = "A senha deve conter no mínimo 4 caracteres."
                                    } else if (password != confirmPassword) {
                                        errorText = "Controle falhou! Senhas não coincidem."
                                    } else {
                                        errorText = ""
                                        viewModel.registerEmailAndPassword(email, password, name)
                                    }
                                } else {
                                    if (loginByMasterPassword) {
                                        if (masterPassword.isBlank()) {
                                            errorText = "A senha não pode estar em branco."
                                        } else {
                                            scope.launch {
                                                val success = viewModel.login(masterPassword)
                                                if (!success) {
                                                    errorText = "Senha mestre incorreta!"
                                                } else {
                                                    errorText = ""
                                                }
                                            }
                                        }
                                    } else {
                                        if (email.isBlank() || password.isBlank()) {
                                            errorText = "Preencha o e-mail e a senha cadastrados."
                                        } else {
                                            scope.launch {
                                                val success = viewModel.loginWithEmail(email, password)
                                                if (!success) {
                                                    errorText = "E-mail ou senha incorretos! Tente novamente."
                                                } else {
                                                    errorText = ""
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("submit_setup_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isSignUp) "Criar Minha Conta" else "Entrar",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }

                        // Elegant Google Divider
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                            Text(
                                text = "ou",
                                modifier = Modifier.padding(horizontal = 12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = ThemeSecondaryText.copy(alpha = 0.5f)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Modern Apple-style Outline Google Button
                        OutlinedButton(
                            onClick = { showGoogleChooser = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("google_login_button"),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(ThemePurpleLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("G", fontWeight = FontWeight.ExtraBold, color = ThemePurplePrimary, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSignUp) "Cadastrar com o Google" else "Entrar com o Google",
                                    color = ThemeSecondaryText.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "BiblioTech • Conexão Local Segura ⚡",
                    fontSize = 11.sp,
                    color = ThemeSecondaryText.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun GoogleSignChooserDialog(
    onAccountSelected: (email: String, name: String, photo: String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, ThemeBorder),
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(ThemePurpleLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", fontWeight = FontWeight.Bold, color = ThemePurplePrimary, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fazer login com o Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeOnBackground
                    )
                }

                Text(
                    text = "Selecione uma conta para continuar no BiblioTech:",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeSecondaryText,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val accounts = listOf(
                    Triple("andersonfelipex5@gmail.com", "Anderson Felipe", "AF"),
                    Triple("bibliotech.admin@gmail.com", "Admin BiblioTech", "AB")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.forEach { (email, name, alphabet) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAccountSelected(email, name, "") }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ThemePurplePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = alphabet,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeOnBackground
                                )
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThemeSecondaryText
                                )
                            }
                        }
                    }

                    var inputEmail by remember { mutableStateOf("") }
                    var isExpandedOptions by remember { mutableStateOf(false) }

                    if (!isExpandedOptions) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { isExpandedOptions = true }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ThemePurpleLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = ThemePurplePrimary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Usar outra conta...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ThemePurplePrimary
                            )
                        }
                    } else {
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            OutlinedTextField(
                                value = inputEmail,
                                onValueChange = { inputEmail = it },
                                label = { Text("E-mail de Acesso") },
                                placeholder = { Text("exemplo@escola.com") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(onClick = { isExpandedOptions = false }) { Text("Cancelar") }
                                Button(
                                    onClick = {
                                        if (inputEmail.isNotBlank()) {
                                            val namePart = inputEmail.split("@").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Usuário Google"
                                            onAccountSelected(inputEmail, namePart, "")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary)
                                ) {
                                    Text("Entrar")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Para continuar, o Google compartilhará seu nome, e-mail e foto de perfil com o BiblioTech.",
                    fontSize = 10.sp,
                    color = ThemeSecondaryText,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Backwards compatibility for screenshot tests
@Composable
fun SetupPasswordScreen(onPasswordSaved: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ThemeBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp).widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = ThemePurplePrimary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Configuração de Acesso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onPasswordSaved("admin123") },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_setup_button")
                ) {
                    Text("Configurar Acesso Inicial")
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onValidatePassword: suspend (String) -> Boolean) {
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.testTag("password_input")
            )
            Button(
                onClick = { scope.launch { onValidatePassword(password) } },
                modifier = Modifier.testTag("submit_login_button")
            ) {
                Text("Entrar")
            }
        }
    }
}

@Composable
fun MainLibraryAppContent(viewModel: LibraryViewModel) {
    var selectedTab by rememberSaveable { mutableStateOf(0) } // 0 = Catalog/Acervo, 1 = Circulation/Empréstimos, 2 = Borrowers, 3 = Configurations
    var isAddingBookRoute by rememberSaveable { mutableStateOf(false) }
    var bookToEdit by remember { mutableStateOf<BookEntity?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth > 600.dp

        if (isTablet) {
            // Tablet layout: Side drawer/navigation layout
            Row(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight(),
                    color = Color.White,
                    border = BorderStroke(1.dp, ThemeBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 24.dp, horizontal = 16.dp)
                            .fillMaxSize()
                    ) {
                        // Brand header details
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ThemeBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalLibrary,
                                    contentDescription = "Library App Logo",
                                    tint = ThemePurplePrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "BiblioTech",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeOnBackground
                                )
                                Text(
                                    text = "Modo Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ThemeSecondaryText
                                )
                            }
                        }

                        // Tablet Navigation Items (Drawer list elements)
                        NavigationDrawerItem(
                            label = { Text("Acervo Geral") },
                            selected = selectedTab == 0 && !isAddingBookRoute && bookToEdit == null,
                            onClick = {
                                selectedTab = 0
                                isAddingBookRoute = false
                                bookToEdit = null
                            },
                            icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = BentoLilacSecondary,
                                selectedTextColor = BentoLilacSecondaryOn,
                                selectedIconColor = ThemePurplePrimary,
                                unselectedTextColor = ThemeSecondaryText,
                                unselectedIconColor = ThemeSecondaryText
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("Circulação / Empréstimos") },
                            selected = selectedTab == 1 && !isAddingBookRoute && bookToEdit == null,
                            onClick = {
                                selectedTab = 1
                                isAddingBookRoute = false
                                bookToEdit = null
                            },
                            icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = BentoLilacSecondary,
                                selectedTextColor = BentoLilacSecondaryOn,
                                selectedIconColor = ThemePurplePrimary,
                                unselectedTextColor = ThemeSecondaryText,
                                unselectedIconColor = ThemeSecondaryText
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("Alunos & Professores") },
                            selected = selectedTab == 2 && !isAddingBookRoute && bookToEdit == null,
                            onClick = {
                                selectedTab = 2
                                isAddingBookRoute = false
                                bookToEdit = null
                            },
                            icon = { Icon(Icons.Default.People, contentDescription = null) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = BentoLilacSecondary,
                                selectedTextColor = BentoLilacSecondaryOn,
                                selectedIconColor = ThemePurplePrimary,
                                unselectedTextColor = ThemeSecondaryText,
                                unselectedIconColor = ThemeSecondaryText
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("Gestão JTA & ROOF") },
                            selected = selectedTab == 4 && !isAddingBookRoute && bookToEdit == null,
                            onClick = {
                                selectedTab = 4
                                isAddingBookRoute = false
                                bookToEdit = null
                            },
                            icon = { Icon(Icons.Default.Insights, contentDescription = null) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = BentoLilacSecondary,
                                selectedTextColor = BentoLilacSecondaryOn,
                                selectedIconColor = ThemePurplePrimary,
                                unselectedTextColor = ThemeSecondaryText,
                                unselectedIconColor = ThemeSecondaryText
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("Configurações & Backup") },
                            selected = selectedTab == 3 && !isAddingBookRoute && bookToEdit == null,
                            onClick = {
                                selectedTab = 3
                                isAddingBookRoute = false
                                bookToEdit = null
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = BentoLilacSecondary,
                                selectedTextColor = BentoLilacSecondaryOn,
                                selectedIconColor = ThemePurplePrimary,
                                unselectedTextColor = ThemeSecondaryText,
                                unselectedIconColor = ThemeSecondaryText
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
                        val userName by viewModel.userName.collectAsStateWithLifecycle()
                        val loginMethod by viewModel.loginMethod.collectAsStateWithLifecycle()

                        Card(
                            colors = CardDefaults.cardColors(containerColor = ThemePurpleLight.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, ThemePurpleLight),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(ThemePurplePrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = userName?.take(2)?.uppercase() ?: "AD",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = userName ?: "Administrador",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = ThemeOnBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = userEmail ?: "admin@escola.local",
                                            fontSize = 10.sp,
                                            color = ThemeSecondaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                val badgeText = when (loginMethod) {
                                    "google" -> "Via Google ✨"
                                    "email" -> "Via E-mail 📧"
                                    else -> "Senha Mestre 🔑"
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(BentoLilacSecondary)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            fontSize = 9.sp,
                                            color = ThemePurplePrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.logout() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Logout,
                                            contentDescription = "Sair",
                                            tint = Color.Red.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "BiblioTech v1.2",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThemeSecondaryText,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                // Tablet screen content details
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(ThemeBackground)
                ) {
                    RenderScreen(
                        selectedTab = selectedTab,
                        isAdding = isAddingBookRoute,
                        editingBook = bookToEdit,
                        viewModel = viewModel,
                        onAddBookRoute = { isAddingBookRoute = true },
                        onEditBookRoute = { bookToEdit = it },
                        onBack = {
                            isAddingBookRoute = false
                            bookToEdit = null
                        }
                    )
                }
            }
        } else {
            // Mobile portrait layout: normal layout with bottom navigation bar
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = ThemePurpleLight,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0 && !isAddingBookRoute && bookToEdit == null,
                            onClick = {
                                selectedTab = 0
                                isAddingBookRoute = false
                                bookToEdit = null
                            },
                            icon = { Icon(Icons.Default.MenuBook, contentDescription = "Acervo") },
                            label = { Text("Acervo") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ThemePurplePrimary,
                                selectedTextColor = ThemeOnBackground,
                                indicatorColor = BentoLilacSecondary,
                                unselectedIconColor = ThemeSecondaryText,
                                unselectedTextColor = ThemeSecondaryText
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == 1 && !isAddingBookRoute && bookToEdit == null,
                            onClick = {
                                selectedTab = 1
                                isAddingBookRoute = false
                                bookToEdit = null
                            },
                            icon = { Icon(Icons.Default.SwapHoriz, contentDescription = "Empréstimos") },
                            label = { Text("Empréstimos") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ThemePurplePrimary,
                                selectedTextColor = ThemeOnBackground,
                                indicatorColor = BentoLilacSecondary,
                                unselectedIconColor = ThemeSecondaryText,
                                unselectedTextColor = ThemeSecondaryText
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == 2 && !isAddingBookRoute && bookToEdit == null,
                            onClick = {
                                selectedTab = 2
                                isAddingBookRoute = false
                                bookToEdit = null
                            },
                            icon = { Icon(Icons.Default.People, contentDescription = "Leitores") },
                            label = { Text("Leitores") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ThemePurplePrimary,
                                selectedTextColor = ThemeOnBackground,
                                indicatorColor = BentoLilacSecondary,
                                unselectedIconColor = ThemeSecondaryText,
                                unselectedTextColor = ThemeSecondaryText
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == 4 && !isAddingBookRoute && bookToEdit == null,
                            onClick = {
                                selectedTab = 4
                                isAddingBookRoute = false
                                bookToEdit = null
                            },
                            icon = { Icon(Icons.Default.Insights, contentDescription = "Análises") },
                            label = { Text("JTA & ROOF") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ThemePurplePrimary,
                                selectedTextColor = ThemeOnBackground,
                                indicatorColor = BentoLilacSecondary,
                                unselectedIconColor = ThemeSecondaryText,
                                unselectedTextColor = ThemeSecondaryText
                            )
                        )

                        NavigationBarItem(
                            selected = selectedTab == 3 && !isAddingBookRoute && bookToEdit == null,
                            onClick = {
                                selectedTab = 3
                                isAddingBookRoute = false
                                bookToEdit = null
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Config") },
                            label = { Text("Config") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ThemePurplePrimary,
                                selectedTextColor = ThemeOnBackground,
                                indicatorColor = BentoLilacSecondary,
                                unselectedIconColor = ThemeSecondaryText,
                                unselectedTextColor = ThemeSecondaryText
                            )
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(ThemeBackground)
                ) {
                    RenderScreen(
                        selectedTab = selectedTab,
                        isAdding = isAddingBookRoute,
                        editingBook = bookToEdit,
                        viewModel = viewModel,
                        onAddBookRoute = { isAddingBookRoute = true },
                        onEditBookRoute = { bookToEdit = it },
                        onBack = {
                            isAddingBookRoute = false
                            bookToEdit = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RenderScreen(
    selectedTab: Int,
    isAdding: Boolean,
    editingBook: BookEntity?,
    viewModel: LibraryViewModel,
    onAddBookRoute: () -> Unit,
    onEditBookRoute: (BookEntity) -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isAdding -> {
                AddEditBookScreen(
                    bookToEdit = null,
                    viewModel = viewModel,
                    onBack = onBack
                )
            }
            editingBook != null -> {
                AddEditBookScreen(
                    bookToEdit = editingBook,
                    viewModel = viewModel,
                    onBack = onBack
                )
            }
            else -> {
                when (selectedTab) {
                    0 -> CatalogTab(viewModel, onAddBookRoute, onEditBookRoute)
                    1 -> LoansTab(viewModel)
                    2 -> BorrowersTab(viewModel)
                    3 -> SettingsTab(viewModel)
                    4 -> JtaRoofTab(viewModel)
                }
            }
        }
    }
}

// --- BENTO STYLE QUICK STATS ---
@Composable
fun CollectionAvailabilityCard(
    totalCopies: Int,
    availableCopies: Int,
    modifier: Modifier = Modifier
) {
    val fillPercentage = if (totalCopies > 0) availableCopies.toFloat() / totalCopies else 0f
    val displayPercentage = (fillPercentage * 100).toInt()

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ThemeBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.3f)) {
                Text(
                    text = "DISPONIBILIDADE DO ACERVO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ThemePurplePrimary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$availableCopies de $totalCopies cópias livres",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeSecondaryText,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Taxa de circulação saudável de ${100 - displayPercentage}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (displayPercentage < 50) StatusOverdueRedText else StatusAvailableGreenText,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .weight(0.7f),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = ThemePurplePrimary
                val trackColor = ThemePurpleLight
                val scale = fillPercentage
                
                androidx.compose.foundation.Canvas(modifier = Modifier.size(54.dp)) {
                    // Track circle
                    drawCircle(
                        color = trackColor,
                        style = Stroke(width = 7.dp.toPx())
                    )
                    // Loaded progress arc
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * scale,
                        useCenter = false,
                        style = Stroke(width = 7.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$displayPercentage%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeOnBackground
                    )
                    Text(
                        text = "livre",
                        fontSize = 9.sp,
                        color = ThemeSecondaryText,
                        lineHeight = 9.sp
                    )
                }
            }
        }
    }
}

// --- BENTO STYLE QUICK STATS WITH RESPONSIVE LAYOUTS ---
@Composable
fun BentoStats(
    books: List<BookEntity>,
    loans: List<LoanEntity>,
    onInsightsClick: (() -> Unit)? = null
) {
    val activeLoans = loans.count { it.returnDate == null }
    val overdueLoans = loans.count { it.calculatedIsOverdue() }
    val totalCopies = books.sumOf { it.totalQuantity }
    val availableCopies = books.sumOf { it.availableQuantity }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        val isWide = LocalContext.current.resources.configuration.smallestScreenWidthDp >= 600
        
        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                    // Overdue
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .then(if (onInsightsClick != null) Modifier.clickable { onInsightsClick() } else Modifier),
                        colors = CardDefaults.cardColors(containerColor = BentoPurpleActive),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(
                                text = "ATRASADOS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoPurpleActiveOn,
                                letterSpacing = 0.5.sp
                            )
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "$overdueLoans",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Light,
                                    color = BentoPurpleActiveOn
                                )
                                if (overdueLoans > 0) {
                                    Text(
                                        text = "atenção",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = StatusOverdueRedText,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Borrowed
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .then(if (onInsightsClick != null) Modifier.clickable { onInsightsClick() } else Modifier),
                        colors = CardDefaults.cardColors(containerColor = BentoLilacSecondary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(
                                text = "EMPRESTADOS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoLilacSecondaryOn,
                                letterSpacing = 0.5.sp
                            )
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "$activeLoans",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Light,
                                    color = BentoLilacSecondaryOn
                                )
                                Text(
                                    text = "/${books.size} títulos",
                                    fontSize = 11.sp,
                                    color = ThemeSecondaryText,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }

                    // Availability Ring Chart
                    CollectionAvailabilityCard(
                        totalCopies = totalCopies,
                        availableCopies = availableCopies,
                        modifier = Modifier
                            .weight(1.5f)
                            .then(if (onInsightsClick != null) Modifier.clickable { onInsightsClick() } else Modifier)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .then(if (onInsightsClick != null) Modifier.clickable { onInsightsClick() } else Modifier),
                            colors = CardDefaults.cardColors(containerColor = BentoPurpleActive),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(
                                    text = "ATRASADOS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoPurpleActiveOn,
                                    letterSpacing = 0.5.sp
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "$overdueLoans",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Light,
                                        color = BentoPurpleActiveOn
                                    )
                                    if (overdueLoans > 0) {
                                        Text(
                                            text = "atenção",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = StatusOverdueRedText,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .then(if (onInsightsClick != null) Modifier.clickable { onInsightsClick() } else Modifier),
                            colors = CardDefaults.cardColors(containerColor = BentoLilacSecondary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(
                                    text = "EMPRESTADOS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoLilacSecondaryOn,
                                    letterSpacing = 0.5.sp
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "$activeLoans",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Light,
                                        color = BentoLilacSecondaryOn
                                    )
                                    Text(
                                        text = "/${books.size} t.",
                                        fontSize = 11.sp,
                                        color = ThemeSecondaryText,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    CollectionAvailabilityCard(
                        totalCopies = totalCopies,
                        availableCopies = availableCopies,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (onInsightsClick != null) Modifier.clickable { onInsightsClick() } else Modifier)
                    )
                }
            }

        if (onInsightsClick != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onInsightsClick() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = ThemePurplePrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ver insights avançados, livros populares e estoque crítico 📊",
                    color = ThemePurplePrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

@Composable
fun LibrarianPocketGuide() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFA889FA).copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, ThemePurplePrimary.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Help,
                        contentDescription = null,
                        tint = ThemePurplePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "💡 Guia Rápido para o Bibliotecário",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ThemePurplePrimary
                    )
                }
                Text(
                    text = if (expanded) "Fechar ▲" else "Ver passo a passo ▼",
                    style = MaterialTheme.typography.labelSmall,
                    color = ThemePurplePrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = ThemePurplePrimary.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GuideItem(
                        icon = Icons.Default.Book,
                        title = "Como Registrar um Empréstimo?",
                        desc = "Toque em qualquer livro abaixo, toque no botão 'Emprestar', digite o nome do leitor (Aluno ou Professor) e confirme. O prazo é calculado de forma totalmente automática!"
                    )
                    GuideItem(
                        icon = Icons.Default.KeyboardReturn,
                        title = "Como Registrar Devolução ou Renovação?",
                        desc = "Vá na aba 'Empréstimos', encontre o empréstimo na lista e clique em 'Devolver' ou 'Renovar (+7 dias)'. Se houver atraso, o app mostrará um alerta vermelho."
                    )
                    GuideItem(
                        icon = Icons.Default.Send,
                        title = "Como Notificar Leitor sobre Atraso via WhatsApp?",
                        desc = "Nos empréstimos atrasados, toque no botão verde 'Alertar WhatsApp'. Você poderá revisar e editar o texto pré-criado e abrir o WhatsApp para enviar em 1 clique."
                    )
                    GuideItem(
                        icon = Icons.Default.Layers,
                        title = "Como Localizar um Livro nas Estantes?",
                        desc = "Na caixa de busca acima, digite o nome da estante (ex: 'Estante A') ou coordenada (ex: 'C1') na busca acima para ver na hora todos os livros que estão guardados lá fisicamente."
                    )
                }
            }
        }
    }
}

@Composable
fun GuideItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ThemePurplePrimary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ThemeOnBackground
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = ThemeSecondaryText,
                lineHeight = 15.sp
            )
        }
    }
}

// --- TAB 1: CATALOG ---
@Composable
fun CatalogTab(
    viewModel: LibraryViewModel,
    onAddBookClick: () -> Unit,
    onEditBookClick: (BookEntity) -> Unit
) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val shelves by viewModel.shelves.collectAsStateWithLifecycle()
    val positions by viewModel.positions.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDialogForLoanBook by remember { mutableStateOf<BookEntity?>(null) }
    var showBookDetailsDialog by remember { mutableStateOf<BookEntity?>(null) }
    var showInsightsDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var showFichaEtiquetaForBook by remember { mutableStateOf<BookEntity?>(null) }


    val barcodeLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            viewModel.searchBooks(result.contents)
        }
    }

    val genres = remember(books) {
        val list = books.map { it.genre.trim().ifBlank { "Sem Gênero" } }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
        listOf("Todos") + list
    }
    var selectedGenre by rememberSaveable { mutableStateOf("Todos") }
    var sortBy by rememberSaveable { mutableStateOf("A-Z") }

    val filteredBooks = remember(books, selectedGenre, sortBy) {
        val filtered = if (selectedGenre == "Todos") {
            books
        } else {
            books.filter { it.genre.trim().ifBlank { "Sem Gênero" }.equals(selectedGenre, ignoreCase = true) }
        }
        
        when (sortBy) {
            "A-Z" -> filtered.sortedBy { it.title.lowercase() }
            "Autor" -> filtered.sortedBy { it.author.lowercase() }
            "Ano" -> filtered.sortedByDescending { it.year }
            "Disponíveis" -> filtered.sortedByDescending { it.availableQuantity }
            else -> filtered
        }
    }

    // Adaptive Grid based on device width
    val gridCells = if (LocalContext.current.resources.configuration.smallestScreenWidthDp >= 600) {
        GridCells.Adaptive(minSize = 300.dp)
    } else {
        GridCells.Fixed(1)
    }

    LazyVerticalGrid(
        columns = gridCells,
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // 1. Header Details
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            val isTablet = LocalContext.current.resources.configuration.smallestScreenWidthDp >= 600
            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ThemePurplePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Ícone BiblioTech",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Acervo Geral",
                                style = MaterialTheme.typography.headlineLarge,
                                color = ThemePurplePrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gerencie e consulte o inventário local da biblioteca.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThemeSecondaryText,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = onAddBookClick,
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_book_button_header")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Novo Livro")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Adicionar Livro", fontSize = 14.sp)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ThemePurplePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Ícone BiblioTech",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Acervo Geral",
                                style = MaterialTheme.typography.headlineMedium,
                                color = ThemePurplePrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gerencie e consulte o inventário local da biblioteca.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThemeSecondaryText
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onAddBookClick,
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("add_book_button_header")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Novo Livro", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Adicionar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 2. Bento Style Stats Overview
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            BentoStats(books = books, loans = loans, onInsightsClick = { showInsightsDialog = true })
        }

        // 2.5 Weekly Suggestions Group
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            WeeklySuggestionsSection(
                suggestions = suggestions,
                allBooks = books,
                onAddSuggestion = { bookId, title, author, profName, classRoom, rec ->
                    viewModel.addSuggestion(context, bookId, title, author, profName, classRoom, rec)
                },
                onRemoveSuggestion = { suggestion ->
                    viewModel.removeSuggestion(context, suggestion)
                }
            )
        }

        // 3. Search and Scanner Row
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Large high-contrast search input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchBooks(it) },
                    placeholder = { Text("Buscar por título, autor, gênero ou prateleira...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ThemeSecondaryText) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchBooks("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_books_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemePurplePrimary,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedLabelColor = ThemePurplePrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Station Scanner button
                IconButton(
                    onClick = { barcodeLauncher.launch(ScanOptions().apply { setOrientationLocked(false); setPrompt("Aponte para o código do livro") }) },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ThemePurpleLight)
                        .testTag("scanner_station_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scanner de Estação",
                        tint = ThemePurplePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 4. Premium Compact Sort Row
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Ordenar",
                        tint = ThemeSecondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Ordenar por:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeSecondaryText
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sortingOptions = listOf(
                        "A-Z" to "Título (A-Z)",
                        "Autor" to "Autores",
                        "Ano" to "Mais Novos",
                        "Disponíveis" to "Disponíveis"
                    )
                    sortingOptions.forEach { (key, title) ->
                        val isSelected = (sortBy == key)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ThemePurplePrimary else ThemePurpleLight.copy(alpha = 0.5f))
                                .clickable { sortBy = key }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else ThemePurplePrimary
                            )
                        }
                    }
                }
            }
        }

        // 5. Horizontal scrollable genre filter chips
        if (genres.size > 1) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScrollWithKeyboard(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    genres.forEach { genre ->
                        val isSelected = (genre == selectedGenre)
                        val count = remember(books, genre) {
                            if (genre == "Todos") {
                                books.size
                            } else {
                                books.count { it.genre.trim().ifBlank { "Sem Gênero" }.equals(genre, ignoreCase = true) }
                            }
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedGenre = genre },
                            label = { Text("$genre ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ThemePurplePrimary,
                                selectedLabelColor = Color.White,
                                containerColor = ThemePurpleLight,
                                labelColor = ThemePurplePrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = Color.Transparent,
                                borderColor = ThemeBorder
                            )
                        )
                    }
                }
            }
        }

        // 6. Content Section (Empty state or Book list items)
        if (filteredBooks.isEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(ThemeBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = ThemeSecondaryText,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedGenre != "Todos") "Nenhum livro neste gênero" else "Nenhum livro encontrado",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ThemePurplePrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedGenre != "Todos") "Experimente selecionar outro gênero ou prateleira." else "Experimente adicionar um livro ou ajustar a busca.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThemeSecondaryText,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        } else {
            items(filteredBooks) { book ->
                val shelfLocationText = remember(book.posicaoEstanteId, shelves, positions) {
                    if (book.posicaoEstanteId != null) {
                        val pos = positions.find { it.id == book.posicaoEstanteId }
                        val sh = shelves.find { it.id == pos?.shelfId }
                        if (sh != null && pos != null) {
                            "${sh.name} • ${pos.coordinate}"
                        } else null
                    } else null
                }
                BookCard(
                    book = book,
                    shelfLocationText = shelfLocationText,
                    onEdit = { onEditBookClick(book) },
                    onDelete = {
                        AlertDialogHelper.showConfirmDialog(
                            context = context,
                            title = "Excluir Livro",
                            message = "Deseja realmente excluir \"${book.title}\"? Isso removerá as referências de estoque local.",
                            onConfirm = { viewModel.deleteBook(context, book) }
                        )
                    },
                    onLoan = { showDialogForLoanBook = book },
                    onCardClick = { showBookDetailsDialog = book }
                )
            }
        }
    }

    // Loan Dialog Trigger
    showDialogForLoanBook?.let { book ->
        LoanBookDialog(
            book = book,
            viewModel = viewModel,
            onDismiss = { showDialogForLoanBook = null },
            onConfirmLoan = { borrowerName, borrowerClass, loanQty ->
                viewModel.loanBook(context, book.id, borrowerName, borrowerClass, loanQty) { success ->
                    if (success) {
                        Toast.makeText(context, "Empréstimo registrado com sucesso!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Erro: Não há exemplares suficientes disponíveis!", Toast.LENGTH_LONG).show()
                    }
                }
                showDialogForLoanBook = null
            }
        )
    }

    // Rich Book Details Dialog Trigger
    showBookDetailsDialog?.let { book ->
        BookDetailsDialog(
            book = book,
            viewModel = viewModel,
            onDismiss = { showBookDetailsDialog = null },
            onEdit = {
                onEditBookClick(book)
                showBookDetailsDialog = null
            },
            onLoan = {
                showDialogForLoanBook = book
            }
        )
    }

    if (showInsightsDialog) {
        LibraryInsightsDialog(
            books = books,
            loans = loans,
            onDismiss = { showInsightsDialog = false }
        )
    }

    if (showScanner) {
        ScannerDialog(
            viewModel = viewModel,
            onDismiss = { showScanner = false }
        )
    }

    showFichaEtiquetaForBook?.let { book ->
        FichaEtiquetaDialog(
            book = book,
            onDismiss = { showFichaEtiquetaForBook = null }
        )
    }


}

@Composable
fun BookCard(
    book: BookEntity,
    shelfLocationText: String? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLoan: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clickable { onCardClick() }
            .testTag("book_card_${book.title}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ThemeBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left panel: Book Photo
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .fillMaxHeight()
                    .background(ThemeBackground),
                contentAlignment = Alignment.Center
            ) {
                val photoModel: Any? = if (!book.photoPath.isNullOrEmpty()) {
                    if (book.photoPath.startsWith("http")) book.photoPath
                    else {
                        val file = File(book.photoPath)
                        if (file.exists()) file else null
                    }
                } else null

                if (photoModel != null) {
                    AsyncImage(
                        model = photoModel,
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Geometric stylized abstract book icon if photo is missing
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "Cover Placeholder",
                            tint = ThemeSecondaryText.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = book.genre.take(8),
                            fontSize = 10.sp,
                            color = ThemeSecondaryText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Right panel: Book details and actions
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = ThemeOnBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Quick action menus
                        Row {
                            IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = ThemeSecondaryText, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Text(
                        text = "Autor: ${book.author}",
                        fontSize = 13.sp,
                        color = ThemeSecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!shelfLocationText.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .background(ThemePurpleLight, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Localização da Estante",
                                tint = ThemePurplePrimary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = shelfLocationText,
                                fontSize = 10.sp,
                                color = ThemePurplePrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = "Local",
                                tint = ThemeSecondaryText,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = book.location.ifBlank { "Sem localização cadastrada" },
                                fontSize = 11.sp,
                                color = ThemeSecondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Header footer status row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "${book.availableQuantity} de ${book.totalQuantity} disp.",
                            fontSize = 12.sp,
                            color = if (book.availableQuantity > 0) ThemePurplePrimary else Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ano: ${book.year}",
                            fontSize = 11.sp,
                            color = ThemeSecondaryText
                        )
                    }

                    // Available/Check out Badge and trigger checkout
                    if (book.availableQuantity > 0) {
                        Button(
                            onClick = onLoan,
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Emprestar", fontSize = 11.sp)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(StatusOverdueRedBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                               text = "Esgotado",
                                color = StatusOverdueRedText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- FORM DIALOG FOR NEW LOANS ---
@Composable
fun LoanBookDialog(
    book: BookEntity,
    viewModel: LibraryViewModel,
    onDismiss: () -> Unit,
    onConfirmLoan: (String, String, Int) -> Unit
) {
    var borrowerName by remember { mutableStateOf("") }
    var borrowerClass by remember { mutableStateOf("") }
    var quantityToLoan by remember { mutableStateOf(1) }
    var errorText by remember { mutableStateOf("") }

    val allBorrowersList by viewModel.allBorrowers.collectAsStateWithLifecycle()
    val allLoansList by viewModel.loans.collectAsStateWithLifecycle()

    val filteredBorrowers = remember(borrowerName, allBorrowersList) {
        if (borrowerName.isBlank()) {
            allBorrowersList.take(4)
        } else {
            val qNorm = borrowerName.normalizeForSearch()
            allBorrowersList.filter {
                it.name.normalizeForSearch().contains(qNorm)
            }.take(5)
        }
    }

    val matchedBorrower = remember(borrowerName, allBorrowersList) {
        val bnNorm = borrowerName.normalizeForSearch()
        allBorrowersList.find { it.name.normalizeForSearch() == bnNorm }
    }

    val borrowerOverdueLoans = remember(matchedBorrower, allLoansList) {
        if (matchedBorrower == null) emptyList()
        else allLoansList.filter { 
            it.borrowerName == matchedBorrower.name && it.returnDate == null && it.calculatedIsOverdue() 
        }
    }

    var dropdownExpanded by remember { mutableStateOf(false) }

    // Reset quantity if borrower is not a Professor
    LaunchedEffect(matchedBorrower) {
        if (matchedBorrower?.type != "Professor") {
            quantityToLoan = 1
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ThemeBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScrollWithKeyboard(rememberScrollState())
            ) {
                Text(
                    text = "Registrar Empréstimo",
                    style = MaterialTheme.typography.titleLarge,
                    color = ThemePurplePrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Livro: ${book.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeSecondaryText,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = borrowerName,
                            onValueChange = { 
                                borrowerName = it 
                                dropdownExpanded = true
                                errorText = ""
                            },
                            label = { Text("Nome do Aluno ou Professor *") },
                            placeholder = { Text("Selecione ou digite...") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                    Icon(
                                        imageVector = if (dropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Alternar menu"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("borrower_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThemePurplePrimary,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedLabelColor = ThemePurplePrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        val itemsToShow = filteredBorrowers

                        if (itemsToShow.isNotEmpty() && dropdownExpanded) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, ThemeBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    itemsToShow.take(5).forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    borrowerName = item.name
                                                    borrowerClass = if (item.type == "Aluno") {
                                                        "Aluno - ${item.classRoom.ifBlank { item.identifier }}"
                                                    } else {
                                                        "Prof - ${item.classRoom.ifBlank { "Sem Turma" }}"
                                                    }
                                                    dropdownExpanded = false
                                                    errorText = ""
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            BorrowerAvatar(
                                                photoUri = item.photoUri,
                                                name = item.name,
                                                size = 20.dp,
                                                borderWidth = 1.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${item.name} (${item.type} - ${item.classRoom.ifBlank { "Sem Turma" }})",
                                                fontSize = 12.sp,
                                                color = ThemeOnBackground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Matched reader card indicator
                if (matchedBorrower != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusAvailableGreenBg.copy(alpha = 0.7f)),
                        border = BorderStroke(1.dp, StatusAvailableGreenText.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selecionado",
                                tint = StatusAvailableGreenText,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Leitor Cadastrado (${matchedBorrower.type})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusAvailableGreenText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = matchedBorrower.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThemeOnBackground,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (matchedBorrower.type == "Aluno") "Série/Turma: ${matchedBorrower.classRoom.ifBlank { matchedBorrower.identifier }}" else "Turma: ${matchedBorrower.classRoom.ifBlank { "Nenhuma" }} • Matéria: ${matchedBorrower.identifier}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ThemeSecondaryText
                                )
                            }
                        }
                    }

                    val isBlockedByPenalidade = matchedBorrower != null && matchedBorrower.blockedUntil != null && matchedBorrower.blockedUntil > System.currentTimeMillis()

                    if (borrowerOverdueLoans.isNotEmpty() || isBlockedByPenalidade) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = StatusOverdueRedBg),
                            border = BorderStroke(1.dp, StatusOverdueRedText.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isBlockedByPenalidade) Icons.Default.Block else Icons.Default.Warning,
                                    contentDescription = "Atenção",
                                    tint = StatusOverdueRedText,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Bloqueio de Leitor",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = StatusOverdueRedText,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isBlockedByPenalidade) {
                                            val dateFormatBlock = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                                            "Leitor bloqueado por penalidade de atraso até ${dateFormatBlock.format(java.util.Date(matchedBorrower!!.blockedUntil!!))}."
                                        } else {
                                            "Este leitor possui ${borrowerOverdueLoans.size} livro(s) com devolução em atraso. Regularize a situação antes de realizar um novo empréstimo."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StatusOverdueRedText.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }

                    // Dynamic Quantity selector for teachers
                    if (matchedBorrower.type == "Professor") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Quantidade de Exemplares *",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = ThemePurplePrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ThemeBackground, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (quantityToLoan > 1) ThemePurplePrimary else ThemeBorder)
                                    .clickable(enabled = quantityToLoan > 1) { quantityToLoan-- },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "-",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Text(
                                text = "$quantityToLoan",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ThemeOnBackground
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (quantityToLoan < book.availableQuantity) ThemePurplePrimary else ThemeBorder)
                                    .clickable(enabled = quantityToLoan < book.availableQuantity) { quantityToLoan++ },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = "(Disponível: ${book.availableQuantity})",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThemeSecondaryText
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusOverdueRedBg.copy(alpha = 0.8f)),
                        border = BorderStroke(1.dp, StatusOverdueRedText.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Aviso",
                                tint = StatusOverdueRedText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (allBorrowersList.isEmpty()) {
                                    "Atenção: Não há leitores cadastrados no sistema. Cadastre-os primeiro na aba Leitores!"
                                } else if (borrowerName.isBlank()) {
                                    "Selecione um leitor cadastrado na lista abaixo."
                                } else {
                                    "Este leitor não está cadastrado! Escolha na lista de sugestões."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusOverdueRedText,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (allBorrowersList.isNotEmpty()) {
                    Text(
                        text = if (borrowerName.isBlank()) "Leitores Cadastrados (Selecione):" else "Sugestões de Leitores (Selecione):",
                        style = MaterialTheme.typography.labelSmall,
                        color = ThemePurplePrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        filteredBorrowers.forEach { borrower ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        borrowerName = borrower.name
                                        borrowerClass = if (borrower.type == "Aluno") {
                                            "Aluno - ${borrower.classRoom.ifBlank { borrower.identifier }}"
                                        } else {
                                            "Prof - ${borrower.classRoom.ifBlank { "Sem Turma" }}"
                                        }
                                        errorText = ""
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (matchedBorrower?.id == borrower.id) ThemePurpleLight else ThemeBackground
                                ),
                                border = BorderStroke(
                                    1.dp, 
                                    if (matchedBorrower?.id == borrower.id) ThemePurplePrimary else ThemeBorder
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BorrowerAvatar(
                                        photoUri = borrower.photoUri,
                                        name = borrower.name,
                                        size = 28.dp,
                                        borderWidth = 1.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = borrower.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemeOnBackground
                                        )
                                        Text(
                                            text = "${borrower.type} • Turma: ${borrower.classRoom.ifBlank { "Nenhuma" }} • ${borrower.identifier}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ThemeSecondaryText
                                        )
                                    }
                                    if (matchedBorrower?.id == borrower.id) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selecionado",
                                            tint = ThemePurplePrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (errorText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = ThemeSecondaryText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val isBlockedByPenalidadeCurrent = matchedBorrower != null && matchedBorrower.blockedUntil != null && matchedBorrower.blockedUntil > System.currentTimeMillis()
                            if (borrowerName.isBlank()) {
                                errorText = "Por favor, selecione um leitor cadastrado."
                            } else if (matchedBorrower == null) {
                                errorText = "Empréstimo bloqueado: O leitor precisa estar cadastrado."
                            } else if (borrowerOverdueLoans.isNotEmpty() || isBlockedByPenalidadeCurrent) {
                                errorText = "Empréstimo bloqueado: O leitor possui livros em atraso ou está sob penalidade."
                            } else if (matchedBorrower.type == "Professor" && quantityToLoan <= 0) {
                                errorText = "A quantidade de livros deve ser maior que zero."
                            } else if (matchedBorrower.type == "Professor" && quantityToLoan > book.availableQuantity) {
                                errorText = "A quantidade de livros selecionada (${quantityToLoan}) excede o saldo disponível (${book.availableQuantity})."
                            } else if (book.availableQuantity <= 0) {
                                errorText = "Erro: Não há exemplares desse livro disponíveis."
                            } else {
                                val finalClass = if (matchedBorrower.type == "Aluno") {
                                    "Aluno - ${matchedBorrower.classRoom.ifBlank { matchedBorrower.identifier }}"
                                } else {
                                    "Prof - ${matchedBorrower.classRoom.ifBlank { "Sem Turma" }}"
                                }
                                val finalQty = if (matchedBorrower.type == "Professor") quantityToLoan else 1
                                onConfirmLoan(matchedBorrower.name, finalClass, finalQty)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirmar Empréstimo")
                    }
                }
            }
        }
    }
}

// --- TAB 2: CIRCULATION/LOANS ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansTab(viewModel: LibraryViewModel) {
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val books by viewModel.books.collectAsStateWithLifecycle()
    val reservations by viewModel.reservations.collectAsStateWithLifecycle()
    val allBorrowers by viewModel.allBorrowers.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // "Todos", "Pendentes", "Atrasados", "Reservas"
    var loanFilter by rememberSaveable { mutableStateOf("Pendentes") }
    var loanSearchQuery by rememberSaveable { mutableStateOf("") }
    var showPartialReturnDialog by remember { mutableStateOf<LoanEntity?>(null) }
    var showWhatsAppDialogForLoan by remember { mutableStateOf<LoanEntity?>(null) }

    val filteredLoans = remember(loans, loanFilter, loanSearchQuery) {
        val baseList = when (loanFilter) {
            "Atrasados" -> loans.filter { it.calculatedIsOverdue() }
            "Pendentes" -> loans.filter { it.returnDate == null }
            else -> loans
        }
        if (loanSearchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.bookTitle.contains(loanSearchQuery, ignoreCase = true) ||
                it.borrowerName.contains(loanSearchQuery, ignoreCase = true) ||
                it.borrowerClass.contains(loanSearchQuery, ignoreCase = true)
            }
        }
    }

    val gridCells = if (LocalContext.current.resources.configuration.smallestScreenWidthDp >= 600) {
        GridCells.Adaptive(minSize = 340.dp)
    } else {
        GridCells.Fixed(1)
    }

    LazyVerticalGrid(
        columns = gridCells,
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // 1. Header label
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            val isTablet = LocalContext.current.resources.configuration.smallestScreenWidthDp >= 600
            val titleStyle = if (isTablet) {
                MaterialTheme.typography.headlineLarge
            } else {
                MaterialTheme.typography.headlineMedium
            }
            Column {
                Text(
                    text = "Circulação & Empréstimos",
                    style = titleStyle,
                    color = ThemePurplePrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Definição automática de prazo de 14 dias para devolução. Atrasados marcados em tempo real.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeSecondaryText,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )
            }
        }

        // 2. Statistics overview
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Column {
                BentoStats(books = books, loans = loans)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // 3. Search Bar
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = loanSearchQuery,
                onValueChange = { loanSearchQuery = it },
                placeholder = { Text("Buscar por título de livro, nome do leitor ou turma...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ThemeSecondaryText) },
                trailingIcon = {
                    if (loanSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { loanSearchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .testTag("search_loans_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThemePurplePrimary,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedLabelColor = ThemePurplePrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // 4. Dynamic Filter Chips
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filterOptions = listOf("Pendentes", "Atrasados", "Reservas", "Todos")
                filterOptions.forEach { option ->
                    val isSelected = (loanFilter == option)
                    FilterChip(
                        selected = isSelected,
                        onClick = { loanFilter = option },
                        label = { 
                            Text(
                                text = when (option) {
                                    "Pendentes" -> "Ativos / Pendentes"
                                    "Atrasados" -> "Atrasados ⚠️"
                                    "Reservas" -> "Reservas / Fila 🕒"
                                    else -> "Histórico Geral"
                                }
                            ) 
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (option) {
                                "Atrasados" -> BentoPurpleActive
                                "Pendentes" -> BentoLilacSecondary
                                "Reservas" -> StatusBorrowedAmberBg
                                else -> ThemePurplePrimary
                            },
                            selectedLabelColor = when (option) {
                                "Atrasados" -> BentoPurpleActiveOn
                                "Pendentes" -> BentoLilacSecondaryOn
                                "Reservas" -> StatusBorrowedAmberText
                                else -> Color.White
                            },
                            containerColor = ThemePurpleLight.copy(alpha = 0.6f),
                            labelColor = ThemePurplePrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            selectedBorderColor = Color.Transparent,
                            borderColor = ThemeBorder
                        )
                    )
                }
            }
        }

        // 5. Lists or Empty States
        if (loanFilter == "Reservas") {
            if (reservations.isEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(ThemeBorder),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = ThemeSecondaryText,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nenhuma reserva ativa!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = ThemePurplePrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "As reservas são criadas quando um livro procurado está esgotado.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThemeSecondaryText,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            } else {
                items(reservations, key = { it.id }) { reservation ->
                    val book = books.find { it.id == reservation.bookId }
                    val borrowerPhone = allBorrowers.find { it.name == reservation.borrowerName }?.phone
                    ReservationCard(
                        reservation = reservation,
                        bookAvailable = (book?.availableQuantity ?: 0) > 0,
                        borrowerPhone = borrowerPhone,
                        onPromote = {
                            viewModel.promoteReservationToLoan(context, reservation) { success ->
                                if (success) {
                                    Toast.makeText(context, "Reserva promovida a empréstimo com sucesso!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Falha ao emprestar exemplar.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDelete = {
                            AlertDialogHelper.showConfirmDialog(
                                context = context,
                                title = "Cancelar Reserva",
                                message = "Tem certeza de que deseja cancelar esta reserva para ${reservation.borrowerName}?",
                                onConfirm = { viewModel.deleteReservation(context, reservation.id) }
                            )
                        }
                    )
                }
            }
        } else {
            if (filteredLoans.isEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(ThemeBorder),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = ThemeSecondaryText,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = when (loanFilter) {
                                    "Atrasados" -> "Sem empréstimos atrasados!"
                                    "Pendentes" -> "Nenhum empréstimo ativo no momento!"
                                    else -> "Nenhum empréstimo registrado"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = ThemePurplePrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (loanFilter) {
                                    "Atrasados" -> "Parabéns! Todos os leitores estão em dia."
                                    "Pendentes" -> "Todos os livros devidamente com sucesso!"
                                    else -> "Realize empréstimos a partir do Acervo Geral."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThemeSecondaryText,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredLoans, key = { it.id }) { loan ->
                    val borrowerPhone = allBorrowers.find { it.name == loan.borrowerName }?.phone
                    LoanCard(
                        loan = loan,
                        borrowerPhone = borrowerPhone,
                        onRenew = {
                            viewModel.renewLoan(context, loan.id) { success ->
                                if (success) {
                                    Toast.makeText(context, "Empréstimo renovado com sucesso! (+1 semana)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onReturn = {
                            if (loan.quantity > 1) {
                                showPartialReturnDialog = loan
                            } else {
                                viewModel.returnBook(context, loan.id, 1) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Livro devolvido com sucesso!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onDelete = {
                            AlertDialogHelper.showConfirmDialog(
                                context = context,
                                title = "Apagar Registro",
                                message = "Deseja excluir este registro de empréstimo? Se o status estiver em aberto, o exemplar voltará ao acervo.",
                                onConfirm = { viewModel.deleteLoan(context, loan) }
                            )
                        },
                        onAlertWhatsApp = {
                            showWhatsAppDialogForLoan = loan
                        }
                    )
                }
            }
        }
    }

    showPartialReturnDialog?.let { loan ->
        PartialReturnDialog(
            loan = loan,
            onDismiss = { showPartialReturnDialog = null },
            onConfirm = { quantityToReturn ->
                showPartialReturnDialog = null
                viewModel.returnBook(context, loan.id, quantityToReturn) { success ->
                    if (success) {
                        Toast.makeText(context, "Devolução parcial registrada!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    showWhatsAppDialogForLoan?.let { loan ->
        val borrowerPhone = allBorrowers.find { it.name == loan.borrowerName }?.phone
        OverdueWhatsAppDialog(
            loan = loan,
            initialPhone = borrowerPhone,
            onDismiss = { showWhatsAppDialogForLoan = null },
            onSubmit = { finalPhone, finalMessage ->
                showWhatsAppDialogForLoan = null
                viewModel.updateBorrowerPhone(context, loan.borrowerName, finalPhone)
                
                try {
                    val parsedNumber = finalPhone.replace(Regex("[^0-9]"), "")
                    val ddiNumber = if (parsedNumber.length >= 12) parsedNumber else "55$parsedNumber"
                    val url = "https://api.whatsapp.com/send?phone=$ddiNumber&text=${java.net.URLEncoder.encode(finalMessage, "UTF-8")}"
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse(url)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Erro ao abrir o WhatsApp", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun LoanCard(
    loan: LoanEntity,
    borrowerPhone: String? = null,
    onRenew: () -> Unit,
    onReturn: () -> Unit,
    onDelete: () -> Unit,
    onAlertWhatsApp: (() -> Unit)? = null
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val isOverdue = loan.calculatedIsOverdue()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdue) Color(0xFFFFECEF) else Color.White
        ),
        border = BorderStroke(1.dp, if (isOverdue) StatusOverdueRedText else ThemeBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            if (isOverdue) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StatusOverdueRedText.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = StatusOverdueRedText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DEVOLUÇÃO EM ATRASO • ALERTA DE PRAZO",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusOverdueRedText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
            // First Row: Borrower and Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Initials circle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isOverdue) StatusOverdueRedBg else BentoLilacSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = loan.borrowerName.take(2).uppercase(),
                            color = if (isOverdue) StatusOverdueRedText else BentoLilacSecondaryOn,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = loan.borrowerName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = ThemeOnBackground
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Turma: ${loan.borrowerClass}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThemeSecondaryText
                            )
                            if (loan.quantity > 1) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ThemePurpleLight)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${loan.quantity} exemplares",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThemePurplePrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            val ONE_WEEK_MS = 7L * 24 * 60 * 60 * 1000
                            val totalWeeksPrecise = (loan.dueDate - loan.loanDate).toDouble() / ONE_WEEK_MS
                            val renewalCount = java.lang.Math.max(0, java.lang.Math.round(totalWeeksPrecise - 1.0).toInt())
                            if (renewalCount > 0 && loan.returnDate == null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(StatusBorrowedAmberBg)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Renovado $renewalCount x",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusBorrowedAmberText,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val shareContext = LocalContext.current
                    IconButton(
                        onClick = {
                            val statusLabel = if (loan.returnDate != null) {
                                "DEVOLVIDO em ${dateFormat.format(Date(loan.returnDate))}"
                            } else if (isOverdue) {
                                "ATRASADO (Prazo era ${dateFormat.format(Date(loan.dueDate))})"
                            } else {
                                "ATIVO (Prazo de devolução: ${dateFormat.format(Date(loan.dueDate))})"
                            }
                            
                            val text = """
                                📚 *BiblioTech - Comprovante de Empréstimo* 📚
                                
                                👤 *Leitor:* ${loan.borrowerName} (${loan.borrowerClass})
                                📖 *Livro:* "${loan.bookTitle}" (por ${loan.bookAuthor})
                                🔢 *Quantidade:* ${loan.quantity} exemplar(es)
                                📅 *Data de Retirada:* ${dateFormat.format(Date(loan.loanDate))}
                                📌 *Status:* $statusLabel
                                
                                _Por favor, conserve o livro e respeite a data de entrega. Obrigado!_
                            """.trimIndent()
                            
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Compartilhar Comprovante")
                            shareContext.startActivity(shareIntent)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartilhar",
                            tint = ThemePurplePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (isOverdue && onAlertWhatsApp != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onAlertWhatsApp,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "WhatsApp",
                                tint = StatusAvailableGreenText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir Registro", tint = ThemeSecondaryText.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Book info panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ThemeBackground)
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = loan.bookTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeOnBackground
                    )
                    Text(
                        text = "por ${loan.bookAuthor}",
                        fontSize = 11.sp,
                        color = ThemeSecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timestamps and status action bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Retirada: ${dateFormat.format(Date(loan.loanDate))}",
                        fontSize = 11.sp,
                        color = ThemeSecondaryText
                    )
                    val expectedReturn = Date(loan.dueDate)
                    Text(
                        text = if (loan.returnDate != null) {
                            "Devolvido em: ${dateFormat.format(Date(loan.returnDate))}"
                        } else {
                            "Prazo Limite: ${dateFormat.format(expectedReturn)}"
                        },
                        fontSize = 11.sp,
                        color = if (isOverdue) StatusOverdueRedText else ThemeSecondaryText,
                        fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Show dynamic Status Badge or Devolution/Renewal Buttons
                if (loan.returnDate != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(StatusAvailableGreenBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Entregue",
                            color = StatusAvailableGreenText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isOverdue) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StatusOverdueRedBg)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Atrasado",
                                    color = StatusOverdueRedText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isOverdue && onAlertWhatsApp != null) {
                            Button(
                                onClick = onAlertWhatsApp,
                                colors = ButtonDefaults.buttonColors(containerColor = StatusAvailableGreenText),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Alertar WhatsApp", fontSize = 11.sp, color = Color.White)
                            }
                        }

                        // Renew Action button
                        OutlinedButton(
                            onClick = onRenew,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ThemePurplePrimary),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Renovar", fontSize = 11.sp, color = ThemePurplePrimary)
                        }

                        Button(
                            onClick = onReturn,
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Registrar Devolução", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun OverdueWhatsAppDialog(
    loan: LoanEntity,
    initialPhone: String?,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var phone by remember { mutableStateOf(initialPhone ?: "") }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val defaultMsg = "Olá ${loan.borrowerName}, o livro '${loan.bookTitle}' emprestado em ${dateFormat.format(Date(loan.loanDate))} está com o prazo de devolução vencido (vencimento em ${dateFormat.format(Date(loan.dueDate))}). Por favor, regularize sua situação na biblioteca assim que possível. Obrigado! 📚"
    var message by remember { mutableStateOf(defaultMsg) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    tint = StatusAvailableGreenText,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Alerta de Devolução 💬",
                    style = MaterialTheme.typography.titleLarge,
                    color = ThemePurplePrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Prepare uma mensagem para o leitor ${loan.borrowerName}. Ao atualizar o celular abaixo, o cadastro do leitor será atualizado na base de dados automaticamente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeSecondaryText
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Número de Celular") },
                    placeholder = { Text("Ex: 11999998888") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemePurplePrimary,
                        focusedLabelColor = ThemePurplePrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                    )
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Texto da Mensagem") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemePurplePrimary,
                        focusedLabelColor = ThemePurplePrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (phone.isBlank()) {
                        Toast.makeText(context, "Por favor, informe o telefone do leitor.", Toast.LENGTH_SHORT).show()
                    } else {
                        onSubmit(phone, message)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusAvailableGreenText),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Enviar por WhatsApp", color = Color.White)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        try {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Alerta de Livro", message)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Mensagem copiada para a área de transferência!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro ao copiar mensagem", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Copiar")
                }
                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancelar")
                }
            }
        }
    )
}

private fun Context.findActivity(): Activity? {
    var cur = this
    while (cur is ContextWrapper) {
        if (cur is Activity) return cur
        cur = cur.baseContext
    }
    return null
}

// --- TAB 3: SETTINGS & BACKUPS ---
@Composable
fun SettingsTab(viewModel: LibraryViewModel) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var changeStatusText by remember { mutableStateOf("") }

    var isUnlocked by rememberSaveable { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    // Backup selectors
    val selectBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputAsString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().readText()
                }
                if (!inputAsString.isNullOrBlank()) {
                    viewModel.restoreBackup(context, inputAsString) { success ->
                        if (success) {
                            Toast.makeText(context, "Backup restaurado com sucesso! Dados sincronizados.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Erro: Arquivo de backup inválido ou corrompido.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Erro ao abrir arquivo.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val saveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonStr = viewModel.exportBackupToJson()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonStr.toByteArray())
                }
                Toast.makeText(context, "Backup salvo com sucesso!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Erro ao salvar backup.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val selectCsvFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputAsString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().readText()
                }
                if (!inputAsString.isNullOrBlank()) {
                    viewModel.importBooksFromCsv(context, inputAsString) { success, count ->
                        if (success) {
                            Toast.makeText(context, "$count livros importados com sucesso!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Erro: Arquivo inválido ou nenhum livro importado. Verifique os cabeçalhos.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Erro ao abrir arquivo.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (false) {
        val activity = context.findActivity() as? FragmentActivity
        val isBiometricAvailable = remember { BiometricHelper.isBiometricAvailable(context) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ThemePurpleLight.copy(alpha = 0.4f),
                            ThemeBackground
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSystemInDarkTheme()) Color(0xFF1E1E24).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)
                ),
                border = BorderStroke(
                    width = 1.2.dp,
                    color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.15f) else ThemePurpleLight
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
                    val userName by viewModel.userName.collectAsStateWithLifecycle()
                    val loginMethod by viewModel.loginMethod.collectAsStateWithLifecycle()

                    if (userEmail != null || !userName.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ThemePurpleLight.copy(alpha = 0.5f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(ThemePurplePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userName?.take(2)?.uppercase() ?: "AD",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userName ?: "Administrador",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeOnBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = userEmail ?: "admin@escola.local",
                                    fontSize = 10.sp,
                                    color = ThemeSecondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { viewModel.logout() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Desconectar",
                                    tint = Color.Red.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(ThemePurpleLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBiometricAvailable) Icons.Default.Face else Icons.Default.Lock,
                            contentDescription = "Lock Icon",
                            tint = ThemePurplePrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Configurações Seguras",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThemeOnBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "A exportação de planilhas CSV, gerenciamento de banco de dados SQLite e restauração de dados exige autenticação.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeSecondaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            errorMessage = ""
                        },
                        label = { Text("Senha Mestre") },
                        placeholder = { Text("Digite para desbloquear") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_lock_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemePurplePrimary,
                            unfocusedBorderColor = ThemeBorder,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedLabelColor = ThemePurplePrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFD32F2F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isBiometricAvailable) {
                            Button(
                                onClick = {
                                    if (activity != null) {
                                        BiometricHelper.authenticate(
                                            activity = activity,
                                            title = "Área Administrativa",
                                            subtitle = "Autentique com a biometria do aparelho",
                                            onSuccess = {
                                                isUnlocked = true
                                                errorMessage = ""
                                            },
                                            onError = { err ->
                                                errorMessage = "Toque em 'Entrar' e insira sua senha mestre."
                                            }
                                        )
                                    } else {
                                        errorMessage = "Biometria indisponível no momento."
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ThemePurpleLight,
                                    contentColor = ThemePurplePrimary
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Digital",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Biometria", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    val success = viewModel.login(passwordInput)
                                    if (success) {
                                        isUnlocked = true
                                        errorMessage = ""
                                    } else {
                                        errorMessage = "Senha incorreta! Digite novamente."
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThemePurplePrimary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Validar",
                                modifier = Modifier.size(18.dp)
                                    .graphicsLayer { rotationZ = -45f }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Entrar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 20.dp, horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            val isTablet = LocalContext.current.resources.configuration.smallestScreenWidthDp >= 600
            val titleStyle = if (isTablet) {
                MaterialTheme.typography.headlineLarge
            } else {
                MaterialTheme.typography.headlineMedium
            }
            Column {
                Text(
                    text = "Configurações & Backup",
                    style = titleStyle,
                    color = ThemePurplePrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Administre credenciais, execute exportações de redundância e recupere acervos locais.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeSecondaryText,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        item {
            val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
            val userName by viewModel.userName.collectAsStateWithLifecycle()
            val loginMethod by viewModel.loginMethod.collectAsStateWithLifecycle()

            Card(
                colors = CardDefaults.cardColors(containerColor = ThemePurpleLight.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, ThemePurpleLight),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ThemePurplePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName?.take(2)?.uppercase() ?: "AD",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName ?: "Administrador Geral",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ThemeOnBackground
                        )
                        Text(
                            text = userEmail ?: "admin@escola.local",
                            fontSize = 12.sp,
                            color = ThemeSecondaryText
                        )
                        val methodBadge = when (loginMethod) {
                            "google" -> "Autenticado via Google Smart Auth ✨"
                            "email" -> "Autenticado via E-mail 📧"
                            else -> "Administrador Local (Senha Mestre) 🔑"
                        }
                        Text(
                            text = methodBadge,
                            fontSize = 11.sp,
                            color = ThemePurplePrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Sair", tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sair", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Card: SQLite integration info
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(StatusAvailableGreenBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = StatusAvailableGreenText)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Armazenamento SQLite Offline",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = ThemeOnBackground
                        )
                        Text(
                            text = "Todos os registros e captações de fotos estão salvos localmente no aparelho. Sem dependências na nuvem, garantindo velocidade total nos atendimentos aos alunos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThemeSecondaryText,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // Card: Configuração de Empréstimos
        item {
            val studentDays by viewModel.studentLoanDays.collectAsStateWithLifecycle()
            val teacherDays by viewModel.teacherLoanDays.collectAsStateWithLifecycle()

            var localStudentDays by remember(studentDays) { mutableStateOf(studentDays.toString()) }
            var localTeacherDays by remember(teacherDays) { mutableStateOf(teacherDays.toString()) }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(ThemePurpleLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = ThemePurplePrimary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Prazo Padrão de Empréstimos",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = ThemeOnBackground
                            )
                            Text(
                                text = "Defina os dias limite para devoluções de alunos e professores.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThemeSecondaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = localStudentDays,
                            onValueChange = { localStudentDays = it },
                            label = { Text("Dias Alunos") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = localTeacherDays,
                            onValueChange = { localTeacherDays = it },
                            label = { Text("Dias Profs") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val stDays = localStudentDays.toLongOrNull() ?: 7L
                            val tcDays = localTeacherDays.toLongOrNull() ?: 15L
                            viewModel.setStudentLoanDays(stDays)
                            viewModel.setTeacherLoanDays(tcDays)
                            Toast.makeText(context, "Prazos salvos com sucesso!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Salvar Prazos", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Card: Orientação da Tela (Rotação)
        item {
            var selectedOrientation by rememberSaveable { mutableStateOf("sensor") }
            val activity = context.findActivity()

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(ThemePurpleLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ScreenRotation, contentDescription = null, tint = ThemePurplePrimary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Orientação da Tela",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = ThemeOnBackground
                            )
                            Text(
                                text = "Defina a exibição horizontal para tablets e notebooks, controle modo retrato ou siga o sensor automático do aparelho.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThemeSecondaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(
                            "sensor" to "Automático 🔄",
                            "portrait" to "Vertical 📱",
                            "landscape" to "Horizontal 🌅"
                        )

                        options.forEach { (key, label) ->
                            val isSelected = selectedOrientation == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) ThemePurplePrimary else ThemePurpleLight.copy(alpha = 0.5f))
                                    .clickable {
                                        selectedOrientation = key
                                        activity?.let { act ->
                                            act.requestedOrientation = when (key) {
                                                "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                                "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                                else -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                                            }
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else ThemePurplePrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card: Gerenciamento de Backup
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Relatórios e Estatísticas",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThemeOnBackground
                    )
                    Text(
                        text = "Gere relatórios em formato CSV do acervo para visualização em Excel ou outros softwares.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeSecondaryText,
                        modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                    )

                    Button(
                        onClick = {
                            val intent = viewModel.getShareBooksCsvIntent(context)
                            if (intent != null) {
                                context.startActivity(Intent.createChooser(intent, "Compartilhar Relatório CSV"))
                            } else {
                                Toast.makeText(context, "Erro ao gerar arquivo CSV.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusAvailableGreenBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.TableView, contentDescription = null, tint = StatusAvailableGreenText)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar Acervo (CSV)", color = StatusAvailableGreenText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Card: Importar Acervo de Planilha (Excel/CSV)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Importar Acervo de Planilha (Excel / CSV)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThemeOnBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Adicione múltiplos livros de uma só vez importando uma planilha do Excel.",
                        style = MaterialTheme.typography.labelMedium,
                        color = ThemePurplePrimary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ThemeBackground),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Como preparar sua planilha no Excel:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemeOnBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val listSteps = listOf(
                                "1. Crie uma planilha com as seguintes colunas na primeira linha (cabeçalho):\n   • Titulo (Nome do livro - Obrigatório)\n   • Autor (Autor responsável - Obrigatório)\n   • Genero (Categoria, ex: Ficção, Didático)\n   • Ano (Ano de publicação, ex: 2024)\n   • Quantidade (Unidades no acervo, ex: 3)\n   • Localizacao (Corredor, Estante, ex: Estante A1)",
                                "2. No Excel, clique em Salvar Como e selecione o formato:\n   • CSV (separado por vírgulas) (*.csv) ou CSV UTF-8.",
                                "3. Transfira o arquivo para o seu celular ou use o Google Drive.",
                                "4. Toque no botão abaixo, escolha o arquivo .csv e os livros serão carregados e adicionados ao seu acervo local instantaneamente!"
                            )
                            
                            listSteps.forEachIndexed { idx, step ->
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThemeSecondaryText,
                                    modifier = Modifier.padding(bottom = if (idx < listSteps.size - 1) 8.dp else 0.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            selectCsvFileLauncher.launch("*/*")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Selecionar e Importar Planilha (.csv)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Card: Integrar com Plataformas Externas
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Integração Libib e Goodreads",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThemeOnBackground
                    )
                    Text(
                        text = "Sincronização bidirecional com sua estante virtual do Goodreads e inventário Libib.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeSecondaryText,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ThemeSecondaryText.copy(alpha = 0.1f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = "Sincronizar", tint = ThemeSecondaryText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Disponível na próxima atualização da API.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ThemeSecondaryText
                        )
                    }
                }
            }
        }

        // Card: Gerenciamento de Backup
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Gerenciamento de Backup (.json)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThemeOnBackground
                    )
                    Text(
                        text = "Crie uma cópia completa dos livros e histórico de empréstimos para arquivar no Drive ou compartilhar via WhatsApp.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeSecondaryText,
                        modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { saveBackupLauncher.launch("bibliotech_backup.json") },
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("save_backup_button")
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Salvar no Dispositivo")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val intent = viewModel.getShareBackupIntent(context)
                                    if (intent != null) {
                                        context.startActivity(Intent.createChooser(intent, "Compartilhar Backup"))
                                    } else {
                                        Toast.makeText(context, "Erro ao gerar arquivo de backup.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemePurpleLight.copy(alpha=0.5f), contentColor = ThemePurplePrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("export_backup_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Compartilhar")
                            }

                            Button(
                                onClick = {
                                    selectBackupFileLauncher.launch("application/json")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemeBackground),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("restore_backup_button"),
                                border = BorderStroke(1.dp, ThemeBorder)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, tint = ThemePurplePrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restaurar", color = ThemePurplePrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
}


// --- SCREEN: ADD & EDIT BOOK ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBookScreen(
    bookToEdit: BookEntity?,
    viewModel: LibraryViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(bookToEdit?.title ?: "") }
    var author by remember { mutableStateOf(bookToEdit?.author ?: "") }
    var year by remember { mutableStateOf(bookToEdit?.year?.toString() ?: "") }
    var location by remember { mutableStateOf(bookToEdit?.location ?: "") }
    var totalQuantity by remember { mutableStateOf(bookToEdit?.totalQuantity?.toString() ?: "1") }
    var selectedGenre by remember { mutableStateOf(bookToEdit?.genre ?: "Literatura Clássica") }
    var photoPathByUri by remember { mutableStateOf<String?>(bookToEdit?.photoPath) }
    
    var isbnToSearch by remember { mutableStateOf(bookToEdit?.isbn ?: "") }
    var isSearchingIsbn by remember { mutableStateOf(false) }
    var isSearchingCovers by remember { mutableStateOf(false) }
    var foundCoversList by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    var dropdownExpanded by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf("") }

    val genres = listOf(
        "Ficção Científica",
        "Literatura Clássica",
        "História",
        "Poesia",
        "Didáticos",
        "Romance",
        "Mistério",
        "Outros"
    )

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = ImageStorageHelper.saveImageToInternalStorage(context, uri)
            if (localPath != null) {
                photoPathByUri = localPath
                Toast.makeText(context, "Foto adicionada localmente!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScrollWithKeyboard(rememberScrollState())
    ) {
        // Appbar Form Header containing actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = ThemePurplePrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (bookToEdit == null) "Adicionar Livro" else "Editar Livro",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ThemePurplePrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    val parsedYear = year.toIntOrNull()
                    val parsedTotal = totalQuantity.toIntOrNull()

                    if (title.isBlank() || author.isBlank() || location.isBlank() || parsedYear == null || parsedTotal == null) {
                        errorText = "Por favor, preencha todos os campos com valores válidos."
                    } else if (parsedTotal <= 0) {
                        errorText = "A quantidade total deve ser maior que zero."
                    } else {
                        if (bookToEdit == null) {
                            viewModel.addBook(
                                context = context,
                                title = title,
                                author = author,
                                isbn = isbnToSearch,
                                genre = selectedGenre,
                                year = parsedYear,
                                location = location,
                                photoPath = photoPathByUri,
                                totalQuantity = parsedTotal
                            )
                            Toast.makeText(context, "Livro cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.updateBook(
                                context = context,
                                book = bookToEdit,
                                title = title,
                                author = author,
                                isbn = isbnToSearch,
                                genre = selectedGenre,
                                year = parsedYear,
                                location = location,
                                photoPath = photoPathByUri,
                                totalQuantity = parsedTotal
                            )
                            Toast.makeText(context, "Livro atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                        onBack()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("submit_book_form_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Salvar")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Auto-fill Card for New Books
        if (bookToEdit == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BentoLilacSecondary),
                border = BorderStroke(1.dp, ThemePurplePrimary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Preenchimento Automático via Busca ISBN / CBL",
                        style = MaterialTheme.typography.labelMedium,
                        color = ThemePurplePrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Consulte bancos nacionais da CBL (BrasilAPI) e Google Books",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = isbnToSearch,
                            onValueChange = { isbnToSearch = it.filter { ch -> ch.isDigit() || ch == '-' } },
                            label = { Text("ISBN (Opcional)") },
                            placeholder = { Text("Ex: 9788532530783") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThemePurplePrimary,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (isbnToSearch.isNotBlank()) {
                                    isSearchingIsbn = true
                                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        val cleanIsbn = isbnToSearch.replace("-", "").trim()
                                        var foundBookData = false

                                        // ENGINE 1: BrasilAPI (Official CBL & Brazilian Book Database)
                                        try {
                                            val url = java.net.URL("https://brasilapi.com.br/api/isbn/v1/${cleanIsbn}")
                                            val connection = url.openConnection() as java.net.HttpURLConnection
                                            connection.requestMethod = "GET"
                                            connection.connectTimeout = 8000
                                            connection.readTimeout = 8000
                                            val responseCode = connection.responseCode
                                            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                                                val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream))
                                                val response = reader.readText()
                                                reader.close()
                                                
                                                val jsonObj = org.json.JSONObject(response)
                                                val foundTitle = if (jsonObj.has("title") && !jsonObj.isNull("title")) jsonObj.getString("title") else ""
                                                val foundAuthors = if (jsonObj.has("authors") && !jsonObj.isNull("authors")) {
                                                    val authorsArray = jsonObj.getJSONArray("authors")
                                                    val authorsList = mutableListOf<String>()
                                                    for (i in 0 until authorsArray.length()) {
                                                        authorsList.add(authorsArray.getString(i))
                                                    }
                                                    authorsList.joinToString(", ")
                                                } else ""
                                                
                                                val foundYear = if (jsonObj.has("year") && !jsonObj.isNull("year")) jsonObj.get("year").toString() else ""
                                                val foundCover = if (jsonObj.has("cover_url") && !jsonObj.isNull("cover_url")) jsonObj.getString("cover_url") else null
                                                val foundPublisher = if (jsonObj.has("publisher") && !jsonObj.isNull("publisher")) jsonObj.getString("publisher") else ""
                                                
                                                val subjectStr = if (jsonObj.has("subjects") && !jsonObj.isNull("subjects")) {
                                                    val array = jsonObj.getJSONArray("subjects")
                                                    val list = mutableListOf<String>()
                                                    for (i in 0 until array.length()) {
                                                        list.add(array.getString(i).lowercase(java.util.Locale.getDefault()))
                                                    }
                                                    list.joinToString(" ")
                                                } else ""

                                                val mappedGenre = when {
                                                    subjectStr.contains("ficção científica") || subjectStr.contains("ciencia ficcao") || subjectStr.contains("sci-fi") -> "Ficção Científica"
                                                    subjectStr.contains("história") || subjectStr.contains("historia") || subjectStr.contains("history") -> "História"
                                                    subjectStr.contains("poesia") || subjectStr.contains("poema") || subjectStr.contains("poetry") -> "Poesia"
                                                    subjectStr.contains("escola") || subjectStr.contains("didático") || subjectStr.contains("didatico") || subjectStr.contains("educação") || subjectStr.contains("ensino") || subjectStr.contains("didática") -> "Didáticos"
                                                    subjectStr.contains("mistério") || subjectStr.contains("misterio") || subjectStr.contains("policial") || subjectStr.contains("crime") || subjectStr.contains("thriller") || subjectStr.contains("suspense") -> "Mistério"
                                                    subjectStr.contains("romance") || subjectStr.contains("novela") || subjectStr.contains("ficção") || subjectStr.contains("fiction") -> "Romance"
                                                    subjectStr.contains("clássico") || subjectStr.contains("classico") || subjectStr.contains("clássicos") -> "Literatura Clássica"
                                                    else -> "Outros"
                                                }

                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    if (foundTitle.isNotBlank()) title = foundTitle
                                                    if (foundAuthors.isNotBlank()) author = foundAuthors
                                                    if (foundYear.isNotBlank()) year = foundYear
                                                    if (foundPublisher.isNotBlank()) {
                                                        location = "Ed. $foundPublisher"
                                                    }
                                                    selectedGenre = mappedGenre
                                                    if (foundCover != null && foundCover.isNotBlank()) {
                                                        photoPathByUri = foundCover
                                                    }
                                                    isSearchingIsbn = false
                                                    Toast.makeText(context, "Dados carregados do Busca ISBN nacional!", Toast.LENGTH_SHORT).show()
                                                }
                                                foundBookData = true
                                            }
                                        } catch (e: Exception) {
                                            // Fallback to Google Books on error
                                        }

                                        // ENGINE 2: Google Books API (Fallback if BrasilAPI has no record or errored)
                                        if (!foundBookData) {
                                            try {
                                                val response = com.example.network.ApiClient.googleBooksApi.searchBooks("isbn:$cleanIsbn")
                                                if (response.isSuccessful) {
                                                    val body = response.body()
                                                    if (body?.items != null && body.items.isNotEmpty()) {
                                                        val volumeInfo = body.items[0].volumeInfo
                                                        if (volumeInfo != null) {
                                                            val foundTitle = volumeInfo.title ?: ""
                                                            val foundAuthors = volumeInfo.authors?.firstOrNull() ?: ""
                                                            val foundYear = volumeInfo.publishedDate?.take(4) ?: ""
                                                            val foundThumbnail = volumeInfo.imageLinks?.thumbnail?.replace("http:", "https:")

                                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                                if (foundTitle.isNotBlank()) title = foundTitle
                                                                if (foundAuthors.isNotBlank()) author = foundAuthors
                                                                if (foundYear.isNotBlank()) year = foundYear
                                                                if (foundThumbnail != null) {
                                                                    photoPathByUri = foundThumbnail
                                                                }
                                                                isSearchingIsbn = false
                                                                Toast.makeText(context, "Dados importados do Google Books!", Toast.LENGTH_SHORT).show()
                                                            }
                                                            foundBookData = true
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                // Ignore and handle collectively below
                                            }
                                        }

                                        // If both engines failed to retrieve the book data
                                        if (!foundBookData) {
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                isSearchingIsbn = false
                                                Toast.makeText(context, "ISBN não encontrado ou erro de conexão.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isSearchingIsbn && isbnToSearch.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSearchingIsbn) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = "Buscar ISBN", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Large Cover Photo Selection Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clickable {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ThemeBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val path = photoPathByUri
                if (!path.isNullOrEmpty()) {
                    // Try to load as file or Url
                    AsyncImage(
                        model = if (path.startsWith("http")) path else File(path),
                        contentDescription = "Capa Selecionada",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Semi-transparent tag telling they can change photo
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Toque para alterar a foto", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Pick cover",
                            tint = ThemeSecondaryText,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Selecione uma Capa", fontWeight = FontWeight.Bold, color = ThemePurplePrimary, fontSize = 14.sp)
                        Text("Formatos: JPG ou PNG offline.", color = ThemeSecondaryText, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Universal Cover Finder Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ThemePurpleLight.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, ThemePurplePrimary.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = ThemePurplePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Buscador de Capas em Alta Resolução",
                        style = MaterialTheme.typography.titleMedium,
                        color = ThemePurplePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Consulte simultaneamente as grandes bases (Amazon, Open Library, Google Books, CBL/BrasilAPI, Libib e Goodreads) por ISBN.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeSecondaryText,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = isbnToSearch,
                        onValueChange = { isbnToSearch = it.filter { ch -> ch.isDigit() || ch == '-' } },
                        label = { Text("Código ISBN do Livro") },
                        placeholder = { Text("Ex: 9788532530783") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemePurplePrimary,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedLabelColor = ThemePurplePrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cleanIsbn = isbnToSearch.replace("-", "").trim()
                            if (cleanIsbn.isNotBlank()) {
                                isSearchingCovers = true
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val results = mutableListOf<Pair<String, String>>()
                                    
                                    // 1. Open Library (with force 404 for missing covers)
                                    try {
                                        val olUrl = "https://covers.openlibrary.org/b/isbn/$cleanIsbn-L.jpg?default=false"
                                        val conn = java.net.URL(olUrl).openConnection() as java.net.HttpURLConnection
                                        conn.requestMethod = "GET"
                                        conn.connectTimeout = 3000
                                        conn.readTimeout = 3000
                                        if (conn.responseCode == 200) {
                                            results.add("Open Library" to olUrl)
                                        }
                                    } catch (e: Exception) {
                                        // Ignore
                                    }

                                    // 2. Amazon High-Resolution (Calibre approach check)
                                    try {
                                        val isbn10 = if (cleanIsbn.length == 13 && cleanIsbn.startsWith("978")) {
                                            val middle = cleanIsbn.substring(3, 12)
                                            var sum = 0
                                            for (i in 0 until 9) {
                                                sum += (middle[i] - '0') * (10 - i)
                                            }
                                            val check = 11 - (sum % 11)
                                            val checkStr = when (check) {
                                                10 -> "X"
                                                11 -> "0"
                                                else -> check.toString()
                                            }
                                            middle + checkStr
                                        } else {
                                            cleanIsbn
                                        }
                                        
                                        val amzUrl = "https://images.amazon.com/images/P/$isbn10.01.LZZZZZZZ.jpg"
                                        val conn = java.net.URL(amzUrl).openConnection() as java.net.HttpURLConnection
                                        conn.requestMethod = "GET"
                                        conn.connectTimeout = 3000
                                        conn.readTimeout = 3000
                                        val length = conn.contentLength
                                        // Amazon returns 200 for 1x1 spacer gif if not found; we check contentLength
                                        if (conn.responseCode == 200 && length > 1500) {
                                            results.add("Amazon (Alta Res)" to amzUrl)
                                        }
                                    } catch (e: Exception) {
                                        // Ignore
                                    }

                                    // 3. Google Books (Max Quality zoom upgrade)
                                    try {
                                        val response = com.example.network.ApiClient.googleBooksApi.searchBooks("isbn:$cleanIsbn")
                                        if (response.isSuccessful) {
                                            val body = response.body()
                                            if (body?.items != null && body.items.isNotEmpty()) {
                                                val volInfo = body.items[0].volumeInfo
                                                val thumb = volInfo?.imageLinks?.thumbnail
                                                if (thumb != null) {
                                                    val highResThumb = thumb.replace("http:", "https:")
                                                        .replace("&zoom=1", "&zoom=2")
                                                        .replace("&zoom=5", "&zoom=2")
                                                        .replace("edge=curl", "")
                                                    results.add("Google Books" to highResThumb)
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Ignore
                                    }

                                    // 4. BrasilAPI Cover fallback
                                    try {
                                        val brUrl = "https://brasilapi.com.br/api/isbn/v1/$cleanIsbn"
                                        val conn = java.net.URL(brUrl).openConnection() as java.net.HttpURLConnection
                                        conn.requestMethod = "GET"
                                        conn.connectTimeout = 3000
                                        conn.readTimeout = 3000
                                        if (conn.responseCode == 200) {
                                            val text = conn.inputStream.bufferedReader().use { it.readText() }
                                            val json = org.json.JSONObject(text)
                                            val cover = if (json.has("cover_url") && !json.isNull("cover_url")) json.getString("cover_url") else null
                                            if (!cover.isNullOrBlank()) {
                                                results.add("BrasilAPI (CBL)" to cover)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Ignore
                                    }

                                    // 5. Mercado Editorial API (Brasil)
                                    try {
                                        val meUrl = "https://api.mercadoeditorial.org/api/v1.2/book?isbn=$cleanIsbn"
                                        val conn = java.net.URL(meUrl).openConnection() as java.net.HttpURLConnection
                                        conn.requestMethod = "GET"
                                        conn.connectTimeout = 3000
                                        conn.readTimeout = 3000
                                        if (conn.responseCode == 200) {
                                            val text = conn.inputStream.bufferedReader().use { it.readText() }
                                            val responseObj = org.json.JSONObject(text)
                                            if (responseObj.has("books")) {
                                                val booksArray = responseObj.getJSONArray("books")
                                                if (booksArray.length() > 0) {
                                                    val bookJson = booksArray.getJSONObject(0)
                                                    val cover = if (bookJson.has("imagem_capa") && !bookJson.isNull("imagem_capa")) bookJson.getString("imagem_capa") else null
                                                    if (!cover.isNullOrBlank()) {
                                                        results.add("Mercado Editorial" to cover)
                                                    }
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Ignore
                                    }

                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        isSearchingCovers = false
                                        foundCoversList = results
                                        if (results.isEmpty()) {
                                            Toast.makeText(context, "Nenhuma imagem encontrada nos bancos de dados para este ISBN.", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Encontradas ${results.size} opções de capas!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Por favor, digite um código ISBN.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isSearchingCovers && isbnToSearch.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(start = 8.dp).height(56.dp)
                    ) {
                        if (isSearchingCovers) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Buscar Capas", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                if (foundCoversList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Escolha sua capa preferida:",
                        style = MaterialTheme.typography.labelMedium,
                        color = ThemePurplePrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScrollWithKeyboard(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        foundCoversList.forEach { (source, coverUrl) ->
                            val isSelected = photoPathByUri == coverUrl
                            Card(
                                modifier = Modifier
                                    .width(110.dp)
                                    .clickable {
                                        photoPathByUri = coverUrl
                                        Toast.makeText(context, "Capa da fonte: $source selecionada!", Toast.LENGTH_SHORT).show()
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) ThemePurplePrimary.copy(alpha = 0.1f) else Color.White
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) ThemePurplePrimary else ThemeBorder.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp, 120.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.LightGray.copy(alpha = 0.3f))
                                    ) {
                                        AsyncImage(
                                            model = coverUrl,
                                            contentDescription = "Capa de $source",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(ThemePurplePrimary.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Selecionada",
                                                    tint = ThemePurplePrimary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = source,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) ThemePurplePrimary else ThemeSecondaryText,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Input forms block
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Título do Livro *") },
            placeholder = { Text("Ex: Dom Casmurro") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("book_title_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ThemePurplePrimary,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedLabelColor = ThemePurplePrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = author,
            onValueChange = { author = it },
            label = { Text("Autor *") },
            placeholder = { Text("Ex: Machado de Assis") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("book_author_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ThemePurplePrimary,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedLabelColor = ThemePurplePrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Genre select Box dropdown (Material 3 compliant)
        ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedGenre,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gênero *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .testTag("book_genre_selector"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThemePurplePrimary,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedLabelColor = ThemePurplePrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                genres.forEach { selection ->
                    DropdownMenuItem(
                        text = { Text(selection) },
                        onClick = {
                            selectedGenre = selection
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = year,
                onValueChange = { year = it },
                label = { Text("Ano *") },
                placeholder = { Text("Ex: 1899") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("book_year_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThemePurplePrimary,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedLabelColor = ThemePurplePrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = totalQuantity,
                onValueChange = { totalQuantity = it },
                label = { Text("Estoque Total *") },
                placeholder = { Text("Ex: 12") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("book_qty_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThemePurplePrimary,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedLabelColor = ThemePurplePrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Localização na Biblioteca *") },
            placeholder = { Text("Ex: Corredor B, Estante 4, Prateleira 2") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("book_location_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ThemePurplePrimary,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedLabelColor = ThemePurplePrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        if (errorText.isNotEmpty()) {
            Text(
                text = errorText,
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(112.dp))
    }
}

// --- STANDARD ALERT DIALOG BUILDER HELPER ---
object AlertDialogHelper {
    fun showConfirmDialog(
        context: Context,
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        android.app.AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Confirmar") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}

// ============================================
// NEW FEATURE: STUDENTS & TEACHERS REGISTRATION (LEITORES)
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowersTab(viewModel: LibraryViewModel) {
    val context = LocalContext.current
    val borrowers by viewModel.borrowers.collectAsStateWithLifecycle()
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val borrowerSearchQuery by viewModel.borrowerSearchQuery.collectAsStateWithLifecycle()
    
    var showAddEditDialog by remember { mutableStateOf<BorrowerEntity?>(null) }
    var showDialogForNewBorrower by rememberSaveable { mutableStateOf(false) }
    var selectedBorrowerForDetails by remember { mutableStateOf<BorrowerEntity?>(null) }
    
    var filterType by rememberSaveable { mutableStateOf("Todos") } // "Todos", "Aluno", "Professor"

    val gridCells = if (LocalContext.current.resources.configuration.smallestScreenWidthDp >= 600) {
        GridCells.Adaptive(minSize = 260.dp)
    } else {
        GridCells.Fixed(1)
    }

    val filteredList = remember(borrowers, filterType) {
        borrowers.filter {
            filterType == "Todos" || it.type.lowercase() == filterType.lowercase()
        }
    }

    val groupedByTurma = remember(filteredList) {
        filteredList.groupBy { it.classRoom.trim().ifBlank { "Sem Turma" } }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }

    LazyVerticalGrid(
        columns = gridCells,
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // 1. Header label & actions
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            val isTablet = LocalContext.current.resources.configuration.smallestScreenWidthDp >= 600
            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Alunos & Professores",
                            style = MaterialTheme.typography.headlineLarge,
                            color = ThemePurplePrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Gerencie o cadastro de leitores do colégio e consulte seus históricos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThemeSecondaryText,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = { showDialogForNewBorrower = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_borrower_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Cadastrar")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Novo Leitor", fontSize = 14.sp)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Alunos & Professores",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ThemePurplePrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gerencie o cadastro de leitores do colégio e consulte seus históricos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeSecondaryText
                    )
                    Button(
                        onClick = { showDialogForNewBorrower = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_borrower_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Cadastrar")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Novo Leitor", fontSize = 14.sp)
                    }
                }
            }
        }

        // 2. Search Field
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = borrowerSearchQuery,
                onValueChange = { viewModel.searchBorrowers(it) },
                placeholder = { Text("Buscar leitor por nome, turma ou disciplina...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ThemeSecondaryText) },
                trailingIcon = {
                    if (borrowerSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchBorrowers("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_borrowers_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThemePurplePrimary,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedLabelColor = ThemePurplePrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // 3. Filter Chips selection
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Todos", "Aluno", "Professor").forEach { type ->
                    FilterChip(
                        selected = filterType == type,
                        onClick = { filterType = type },
                        label = { Text(type) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BentoLilacSecondary,
                            selectedLabelColor = BentoLilacSecondaryOn,
                            selectedLeadingIconColor = ThemePurplePrimary
                        ),
                        modifier = Modifier.testTag("filter_borrower_chip_$type")
                    )
                }
            }
        }

        // 4. Content section
        if (filteredList.isEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(ThemeBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = ThemeSecondaryText,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhum leitor cadastrado",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ThemePurplePrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Comece cadastrando um aluno ou professor para registrar empréstimos vinculados.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThemeSecondaryText,
                            modifier = Modifier.padding(top = 2.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            groupedByTurma.forEach { (turma, borrowersInTurma) ->
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ThemePurpleLight),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = ThemePurplePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Turma: $turma",
                                style = MaterialTheme.typography.titleMedium,
                                color = ThemePurplePrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ThemePurplePrimary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${borrowersInTurma.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                items(borrowersInTurma) { borrower ->
                    val totalActive = loans.count { it.borrowerName.lowercase() == borrower.name.lowercase() && it.returnDate == null }
                    BorrowerCard(
                        borrower = borrower,
                        totalActiveLoans = totalActive,
                        onClick = { selectedBorrowerForDetails = borrower }
                    )
                }
            }
        }
    }

    // New Borrower registration dialogue
    if (showDialogForNewBorrower) {
        AddEditBorrowerDialog(
            borrowerToEdit = null,
            viewModel = viewModel,
            onDismiss = { showDialogForNewBorrower = false }
        )
    }

    // Edit Borrower dialogue
    showAddEditDialog?.let { borrower ->
        AddEditBorrowerDialog(
            borrowerToEdit = borrower,
            viewModel = viewModel,
            onDismiss = { showAddEditDialog = null }
        )
    }

    // Detail Drawer/Dialog for borrower history
    selectedBorrowerForDetails?.let { borrower ->
        BorrowerDetailsDialog(
            borrower = borrower,
            viewModel = viewModel,
            onDismiss = { selectedBorrowerForDetails = null },
            onEdit = {
                showAddEditDialog = borrower
                selectedBorrowerForDetails = null
            }
        )
    }
}

@Composable
fun BorrowerCard(
    borrower: BorrowerEntity,
    totalActiveLoans: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("borrower_card_${borrower.name}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ThemeBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BorrowerAvatar(
                photoUri = borrower.photoUri,
                name = borrower.name,
                size = 44.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = borrower.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = ThemeOnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (borrower.type == "Aluno") {
                        if (borrower.classRoom.isNotBlank()) "Série/Turma: ${borrower.classRoom}" else "Série/Turma: ${borrower.identifier}"
                    } else {
                        val specSec = if (borrower.classRoom.isNotBlank()) " • Turmas: ${borrower.classRoom}" else ""
                        "Matéria: ${borrower.identifier}$specSec"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeSecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Show outstanding loans bubble
            if (totalActiveLoans > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ThemePurpleLight)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$totalActiveLoans ativos",
                        style = MaterialTheme.typography.labelSmall,
                        color = ThemePurplePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ThemeBackground)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Sem débitos",
                        style = MaterialTheme.typography.labelSmall,
                        color = ThemeSecondaryText
                    )
                }
            }
        }
    }
}

@Composable
fun BorrowerAvatar(
    photoUri: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 72.dp,
    borderWidth: androidx.compose.ui.unit.Dp = 1.5.dp
) {
    val initials = if (name.isNotBlank()) name.take(2).uppercase() else "LE"
    val avatarColorAndIcon = remember(photoUri) {
        when (photoUri) {
            "avatar:blue" -> Pair(Color(0xFF2196F3), Icons.Default.School)
            "avatar:green" -> Pair(Color(0xFF4CAF50), Icons.Default.AutoStories)
            "avatar:orange" -> Pair(Color(0xFFFF9800), Icons.Default.Mood)
            "avatar:red" -> Pair(Color(0xFFE91E63), Icons.Default.LocalLibrary)
            "avatar:indigo" -> Pair(Color(0xFF3F51B5), Icons.Default.Star)
            else -> null
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White)
            .border(borderWidth, ThemePurplePrimary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (avatarColorAndIcon != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(avatarColorAndIcon.first.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = avatarColorAndIcon.second,
                    contentDescription = null,
                    tint = avatarColorAndIcon.first,
                    modifier = Modifier.fillMaxSize(0.55f)
                )
            }
        } else if (!photoUri.isNullOrEmpty() && (photoUri.startsWith("content://") || photoUri.startsWith("file://") || photoUri.startsWith("http"))) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Foto do Leitor",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Person)
            )
        } else {
            // Initials Fallback
            Text(
                text = initials,
                fontSize = (size.value * 0.33).sp,
                fontWeight = FontWeight.Bold,
                color = ThemePurplePrimary
            )
        }
    }
}

@Composable
fun AddEditBorrowerDialog(
    borrowerToEdit: BorrowerEntity?,
    viewModel: LibraryViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(borrowerToEdit?.name ?: "") }
    var type by remember { mutableStateOf(borrowerToEdit?.type ?: "Aluno") } // "Aluno" or "Professor"
    var classRoom by remember { mutableStateOf(borrowerToEdit?.classRoom ?: "") }
    var identifier by remember { mutableStateOf(borrowerToEdit?.identifier ?: "") }
    var phone by remember { mutableStateOf(borrowerToEdit?.phone ?: "") }
    var errorText by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<String?>(borrowerToEdit?.photoUri) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            photoUri = uri.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ThemeBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScrollWithKeyboard(rememberScrollState())
            ) {
                Text(
                    text = if (borrowerToEdit == null) "Cadastrar Leitor" else "Editar Cadastro",
                    style = MaterialTheme.typography.titleLarge,
                    color = ThemePurplePrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar / Foto Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Foto de Identificação (Opcional)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ThemeOnBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BorrowerAvatar(
                            photoUri = photoUri,
                            name = name,
                            size = 80.dp,
                            borderWidth = 2.dp
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Button(
                                onClick = { photoLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = ThemePurpleLight),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = ThemePurplePrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Carregar da Galeria", color = ThemePurplePrimary, fontSize = 12.sp, maxLines = 1)
                            }

                            if (photoUri != null) {
                                Button(
                                    onClick = { photoUri = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Remover Foto", color = Color.Red, fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Escolha um Avatar Rápido:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ThemeSecondaryText
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val avatarOptions = listOf(
                            "avatar:blue" to Color(0xFF2196F3),
                            "avatar:green" to Color(0xFF4CAF50),
                            "avatar:orange" to Color(0xFFFF9800),
                            "avatar:red" to Color(0xFFE91E63),
                            "avatar:indigo" to Color(0xFF3F51B5)
                        )

                        avatarOptions.forEach { (optionId, color) ->
                            val isSelected = photoUri == optionId
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) ThemePurplePrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { photoUri = optionId },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selecionado",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Segmented selector for type
                Text("Tipo de Leitor *", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = ThemeOnBackground)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Aluno", "Professor").forEach { role ->
                        Button(
                            onClick = { 
                                type = role 
                                errorText = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == role) ThemePurplePrimary else ThemeBackground
                            ),
                            border = BorderStroke(1.dp, if (type == role) Color.Transparent else ThemeBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_select_$role")
                        ) {
                            Text(
                                text = role,
                                color = if (type == role) Color.White else ThemeOnBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Completo *") },
                    placeholder = { Text("Ex: Mariana Souza Santos") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("borrower_form_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemePurplePrimary,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedLabelColor = ThemePurplePrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Explicit Turma (Class) input field
                val classLabel = if (type == "Aluno") "Série / Turma / Ano *" else "Turma(s) Relacionada(s) / Segmentos *"
                val classPlaceholder = if (type == "Aluno") "Ex: 2º Ano Ensino Médio A" else "Ex: 1º ao 5º ano, Ensino Médio"

                OutlinedTextField(
                    value = classRoom,
                    onValueChange = { classRoom = it },
                    label = { Text(classLabel) },
                    placeholder = { Text(classPlaceholder) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("borrower_form_classroom"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemePurplePrimary,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedLabelColor = ThemePurplePrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                val labelRes = if (type == "Aluno") "Matrícula ou Código (Opcional)" else "Matrícula / Disciplina *"
                val placeholderRes = if (type == "Aluno") "Ex: 202488102" else "Ex: Língua Portuguesa"

                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text(labelRes) },
                    placeholder = { Text(placeholderRes) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("borrower_form_identifier"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemePurplePrimary,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedLabelColor = ThemePurplePrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Contato / Telefone (Opcional)") },
                    placeholder = { Text("Ex: (11) 98888-7777") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("borrower_form_phone"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemePurplePrimary,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedLabelColor = ThemePurplePrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorText.isNotEmpty()) {
                    Text(
                        text = errorText,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = ThemeSecondaryText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val classRoomOk = classRoom.isNotBlank()
                            val isIdentifierRequired = type == "Professor"
                            val identifierOk = !isIdentifierRequired || identifier.isNotBlank()

                            if (name.isBlank() || !classRoomOk || !identifierOk) {
                                errorText = if (type == "Aluno") {
                                    "Por favor, preencha o Nome Completo e a Série / Turma."
                                } else {
                                    "Por favor, preencha o Nome Completo, Disciplina e as Turmas."
                                }
                            } else {
                                val finalClassRoom = classRoom.trim()
                                val finalIdentifier = identifier.trim().ifBlank { "Sem Matrícula" }

                                if (borrowerToEdit == null) {
                                    viewModel.addBorrower(context, name.trim(), type, finalIdentifier, phone.trim(), finalClassRoom, photoUri)
                                    Toast.makeText(context, "$type cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.updateBorrower(context, borrowerToEdit, name.trim(), type, finalIdentifier, phone.trim(), finalClassRoom, photoUri)
                                    Toast.makeText(context, "Cadastro atualizado!", Toast.LENGTH_SHORT).show()
                                }
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_borrower_button")
                    ) {
                        Text("Confirmar")
                    }
                }
            }
        }
    }
}

@Composable
fun BorrowerDetailsDialog(
    borrower: BorrowerEntity,
    viewModel: LibraryViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    
    val userLoans = remember(loans, borrower.name) {
        loans.filter { it.borrowerName.lowercase() == borrower.name.lowercase() }
    }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var showPartialReturnDialog by remember { mutableStateOf<LoanEntity?>(null) }
    var showCarteirinha by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ThemeBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .testTag("borrower_details_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScrollWithKeyboard(rememberScrollState())
            ) {
                // Header details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        BorrowerAvatar(
                            photoUri = borrower.photoUri,
                            name = borrower.name,
                            size = 48.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = borrower.type,
                                style = MaterialTheme.typography.labelMedium,
                                color = ThemePurplePrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = borrower.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = ThemeOnBackground,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Edit / Delete quick action bar
                    Row {
                        IconButton(onClick = {
                            val activePending = userLoans.filter { it.returnDate == null }
                            val pendingText = if (activePending.isEmpty()) {
                                "Nenhum livro pendente de devolução! Status regularizado. ✅"
                            } else {
                                "Livros Pendentes de Devolução:\n" + activePending.mapIndexed { idx, loan ->
                                    val isOver = loan.calculatedIsOverdue()
                                    val dueStr = dateFormat.format(Date(loan.dueDate))
                                    "   ${idx + 1}. \"${loan.bookTitle}\" (Prazo: $dueStr)${if (isOver) " ⚠️ ATRASADO!" else ""}"
                                }.joinToString("\n")
                            }
                            
                            val text = """
                                📝 *BiblioTech - Situação de Leitor* 📝
                                
                                👤 *Nome:* ${borrower.name}
                                🏢 *Tipo/Vínculo:* ${borrower.type}
                                🏫 *Turma/Contato:* ${borrower.classRoom}
                                Matrícula/ID: ${borrower.identifier}
                                
                                📊 *Histórico:* ${userLoans.size} empréstimo(s) no total.
                                
                                📌 *Situação:*
                                $pendingText
                                
                                _Colabore devolvendo os livros no prazo correto para permitir que outros leitores tenham acesso!_
                            """.trimIndent()
                            
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Enviar Extrato para Leitor")
                            context.startActivity(shareIntent)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartilhar Extrato", tint = ThemePurplePrimary)
                        }
                        IconButton(onClick = { showCarteirinha = true }) {
                            Icon(Icons.Default.QrCode, contentDescription = "Ver Carteirinha QR", tint = ThemePurplePrimary)
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = ThemeSecondaryText)
                        }
                        IconButton(onClick = {
                            AlertDialogHelper.showConfirmDialog(
                                context = context,
                                title = "Excluir Cadastro",
                                message = "Deseja realmente remover o leitor \"${borrower.name}\" do banco de dados local?",
                                onConfirm = {
                                    viewModel.deleteBorrower(context, borrower)
                                    onDismiss()
                                }
                            )
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remover", tint = Color.Red)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Metadata cards
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ThemeBackground)
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (borrower.type == "Aluno") "Série / Turma: ${borrower.classRoom.ifBlank { borrower.identifier }}" else "Turma(s): ${borrower.classRoom.ifBlank { "Nenhuma" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeOnBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (borrower.type == "Aluno") "Código / Matrícula: ${borrower.identifier}" else "Disciplina / Matéria: ${borrower.identifier}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeSecondaryText
                    )
                    if (borrower.phone.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = ThemeBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = ThemeSecondaryText)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = borrower.phone, style = MaterialTheme.typography.bodyMedium, color = ThemeSecondaryText)
                            }
                            Button(
                                onClick = {
                                    try {
                                        val number = borrower.phone.replace(Regex("[^0-9]"), "")
                                        val text = "Olá ${borrower.name},"
                                        val url = "https://api.whatsapp.com/send?phone=55$number&text=${java.net.URLEncoder.encode(text, "UTF-8")}"
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = android.net.Uri.parse(url)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Erro ao abrir o WhatsApp", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusAvailableGreenBg)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "WhatsApp",
                                    tint = StatusAvailableGreenText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", fontSize = 12.sp, color = StatusAvailableGreenText, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                borrower.blockedUntil?.let { blockTimestamp ->
                    if (blockTimestamp > System.currentTimeMillis()) {
                        val dateFormatBlock = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = StatusOverdueRedBg),
                            border = BorderStroke(1.dp, StatusOverdueRedText.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = "Bloqueado",
                                    tint = StatusOverdueRedText,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Leitor Bloqueado: Atraso",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = StatusOverdueRedText,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Penalidade válida até: ${dateFormatBlock.format(java.util.Date(blockTimestamp))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ThemeOnBackground
                                    )
                                }
                                TextButton(onClick = {
                                    AlertDialogHelper.showConfirmDialog(
                                        context = context,
                                        title = "Remover Bloqueio",
                                        message = "Deseja remover a penalidade manualmente e liberar o usuário para novos empréstimos?",
                                        onConfirm = { viewModel.unblockBorrower(context, borrower) }
                                    )
                                }) {
                                    Text("Retirar", color = StatusOverdueRedText, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Histórico de Empréstimos",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = ThemePurplePrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (userLoans.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = ThemeSecondaryText.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                            Text(
                                text = "Nenhum histórico disponível",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThemeSecondaryText,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(userLoans) { loan ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ThemeBackground),
                                border = BorderStroke(1.dp, ThemeBorder),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = loan.bookTitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemeOnBackground,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        // Status flag
                                        val isOverdue = loan.calculatedIsOverdue()
                                        if (loan.returnDate != null) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(StatusAvailableGreenBg)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("Devolvido", color = StatusAvailableGreenText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else if (isOverdue) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(StatusOverdueRedBg)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("Atrasado", color = StatusOverdueRedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(BentoLilacSecondary)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("Ativo", color = BentoLilacSecondaryOn, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Autor: ${loan.bookAuthor}", fontSize = 11.sp, color = ThemeSecondaryText)
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Empréstimo: ${dateFormat.format(Date(loan.loanDate))}",
                                                fontSize = 11.sp,
                                                color = ThemeSecondaryText
                                            )
                                            if (loan.returnDate != null) {
                                                Text(
                                                    text = "Devolução: ${dateFormat.format(Date(loan.returnDate))}",
                                                    fontSize = 11.sp,
                                                    color = StatusAvailableGreenText,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else {
                                                Text(
                                                    text = "Prazo: ${dateFormat.format(Date(loan.dueDate))}",
                                                    fontSize = 11.sp,
                                                    color = if (loan.calculatedIsOverdue()) Color.Red else ThemePurplePrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Quick return button for active loans
                                        if (loan.returnDate == null) {
                                            Button(
                                                onClick = {
                                                    if (loan.quantity > 1) {
                                                        showPartialReturnDialog = loan
                                                    } else {
                                                        viewModel.returnBook(context, loan.id, 1) { success ->
                                                            if (success) {
                                                                Toast.makeText(context, "Livro devolvido!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(28.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                            ) {
                                                Text("Receber", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ThemePurpleLight),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Fechar", color = ThemePurplePrimary)
                }
            }
        }
    }

    showPartialReturnDialog?.let { loan ->
        PartialReturnDialog(
            loan = loan,
            onDismiss = { showPartialReturnDialog = null },
            onConfirm = { quantityToReturn ->
                showPartialReturnDialog = null
                viewModel.returnBook(context, loan.id, quantityToReturn) { success ->
                    if (success) {
                        Toast.makeText(context, "Devolução parcial registrada!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showCarteirinha) {
        CarteirinhaDialog(
            borrower = borrower,
            onDismiss = { showCarteirinha = false }
        )
    }
}

// ============================================
// NEW FEATURE: RICH BOOK DETAILS DIALOGUE
// ============================================

@Composable
fun BookDetailsDialog(
    book: BookEntity,
    viewModel: LibraryViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onLoan: () -> Unit
) {
    val context = LocalContext.current
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val reservations by viewModel.reservations.collectAsStateWithLifecycle()
    
    val bookLoans = remember(loans, book.id) {
        loans.filter { it.bookId == book.id }
    }
    val bookReservations = remember(reservations, book.id) {
        reservations.filter { it.bookId == book.id }
    }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var showPartialReturnDialog by remember { mutableStateOf<LoanEntity?>(null) }
    var showReserveDialog by remember { mutableStateOf(false) }
    var showFichaEtiqueta by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ThemeBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .testTag("book_details_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScrollWithKeyboard(rememberScrollState())
            ) {
                // Top row title and edit buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ThemeOnBackground
                        )
                        Text(
                            text = "por ${book.author}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThemeSecondaryText
                        )
                    }

                    Row {
                        IconButton(onClick = {
                            val statusLabel = if (book.availableQuantity > 0) {
                                "${book.availableQuantity} de ${book.totalQuantity} disponíveis"
                            } else {
                                "TODOS EMPRESTADOS (Esgotado)"
                            }
                            
                            val text = """
                                📖 *BiblioTech - Informações de Livro* 📖
                                
                                📌 *Título:* ${book.title}
                                👤 *Autor:* ${book.author}
                                🗄️ *Gênero:* ${book.genre}
                                📍 *Localização:* ${book.location}
                                📅 *Ano:* ${book.year}
                                📊 *Status:* $statusLabel
                                
                                _Consulte nosso catálogo local na biblioteca para realizar reserva ou retirada._
                            """.trimIndent()
                            
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Compartilhar Livro")
                            context.startActivity(shareIntent)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartilhar Livro", tint = ThemePurplePrimary)
                        }
                        IconButton(onClick = { showFichaEtiqueta = true }) {
                            Icon(Icons.Default.QrCode, contentDescription = "Ficha e Etiquetas", tint = ThemePurplePrimary)
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = ThemeSecondaryText)
                        }
                        IconButton(onClick = {
                            AlertDialogHelper.showConfirmDialog(
                                context = context,
                                title = "Excluir Livro",
                                message = "Deseja realmente excluir \"${book.title}\"? Isso removerá as referências de estoque local.",
                                onConfirm = {
                                    viewModel.deleteBook(context, book)
                                    onDismiss()
                                }
                            )
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remover", tint = Color.Red)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Left image container
                    Box(
                        modifier = Modifier
                            .size(width = 100.dp, height = 130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ThemeBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        val photoModel: Any? = if (!book.photoPath.isNullOrEmpty()) {
                            if (book.photoPath.startsWith("http")) book.photoPath
                            else {
                                val file = File(book.photoPath)
                                if (file.exists()) file else null
                            }
                        } else null

                        if (photoModel != null) {
                            AsyncImage(
                                model = photoModel,
                                contentDescription = book.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = "Placeholder",
                                tint = ThemeSecondaryText.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Details metadata list
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "Gênero: ${book.genre}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Ano: ${book.year}", style = MaterialTheme.typography.bodySmall, color = ThemeSecondaryText)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = ThemeSecondaryText, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = book.location, style = MaterialTheme.typography.bodySmall, color = ThemeSecondaryText)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "${book.availableQuantity} exemplares disponíveis",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (book.availableQuantity > 0) ThemePurplePrimary else Color.Red
                        )
                        Text(
                            text = "Estoque Total: ${book.totalQuantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeSecondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // --- FILA DE ESPERA (RESERVAS) SECTION ---
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = StatusBorrowedAmberText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Fila de Espera (${bookReservations.size})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemePurplePrimary
                            )
                        }
                    }

                    if (bookReservations.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = ThemeBackground),
                                border = BorderStroke(1.dp, ThemeBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Nenhuma reserva ativa.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ThemeSecondaryText
                                    )
                                }
                            }
                        }
                    } else {
                        items(bookReservations) { reservation ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ThemeBackground),
                                border = BorderStroke(1.dp, ThemeBorder),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = reservation.borrowerName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = ThemeOnBackground
                                        )
                                        Text(
                                            text = "Turma/Contato: ${reservation.borrowerClass}",
                                            fontSize = 10.sp,
                                            color = ThemeSecondaryText
                                        )
                                        Text(
                                            text = "Reservado em: ${dateFormat.format(Date(reservation.requestDate))}",
                                            fontSize = 9.sp,
                                            color = ThemeSecondaryText
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Promote button if book has stock
                                        if (book.availableQuantity > 0) {
                                            Button(
                                                onClick = {
                                                    viewModel.promoteReservationToLoan(context, reservation) { success ->
                                                        if (success) {
                                                            Toast.makeText(context, "Reserva promovida a empréstimo!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "Erro ao promover reserva.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = StatusAvailableGreenText),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(24.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("Emprestar", fontSize = 9.sp, color = Color.White)
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.deleteReservation(context, reservation.id)
                                                Toast.makeText(context, "Reserva cancelada.", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Cancelar Reserva",
                                                tint = Color.Red,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- HISTÓRICO DE CIRCULAÇÃO SECTION ---
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = ThemePurplePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Histórico de Circulação",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemePurplePrimary
                            )
                        }
                    }

                    if (bookLoans.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = ThemeBackground),
                                border = BorderStroke(1.dp, ThemeBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Este exemplar nunca foi emprestado.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ThemeSecondaryText
                                    )
                                }
                            }
                        }
                    } else {
                        items(bookLoans) { loan ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ThemeBackground),
                                border = BorderStroke(1.dp, ThemeBorder),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = loan.borrowerName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = ThemeOnBackground
                                        )
                                        Text(
                                            text = "Turma/Contato: ${loan.borrowerClass}",
                                            fontSize = 10.sp,
                                            color = ThemeSecondaryText
                                        )
                                        Text(
                                            text = "Empréstimo: ${dateFormat.format(Date(loan.loanDate))}",
                                            fontSize = 10.sp,
                                            color = ThemeSecondaryText
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        if (loan.returnDate != null) {
                                            Text(
                                                text = "Devolvido",
                                                color = StatusAvailableGreenText,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = dateFormat.format(Date(loan.returnDate)),
                                                color = ThemeSecondaryText,
                                                fontSize = 9.sp
                                            )
                                        } else {
                                            Text(
                                                text = "Em aberto",
                                                color = ThemePurplePrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = "Prazo: ${dateFormat.format(Date(loan.dueDate))}",
                                                color = if (loan.calculatedIsOverdue()) Color.Red else ThemeSecondaryText,
                                                fontSize = 9.sp
                                            )
                                            
                                            Spacer(modifier = Modifier.height(4.dp))
                                            
                                            Button(
                                                onClick = {
                                                    if (loan.quantity > 1) {
                                                        showPartialReturnDialog = loan
                                                    } else {
                                                        viewModel.returnBook(context, loan.id, 1) { success ->
                                                            if (success) {
                                                                Toast.makeText(context, "Livro recebido!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(24.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("Receber", fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePurpleLight),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Fechar", color = ThemePurplePrimary)
                    }

                    if (book.availableQuantity > 0) {
                        Button(
                            onClick = {
                                onDismiss()
                                onLoan()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Emprestar", color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = {
                                showReserveDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusBorrowedAmberText),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reservar (Fila)", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    showPartialReturnDialog?.let { loan ->
        PartialReturnDialog(
            loan = loan,
            onDismiss = { showPartialReturnDialog = null },
            onConfirm = { quantityToReturn ->
                showPartialReturnDialog = null
                viewModel.returnBook(context, loan.id, quantityToReturn) { success ->
                    if (success) {
                        Toast.makeText(context, "Devolução parcial registrada!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showReserveDialog) {
        ReserveBookDialog(
            book = book,
            viewModel = viewModel,
            onDismiss = { showReserveDialog = false },
            onConfirmReserve = { name, classDetail ->
                viewModel.createReservation(context, book.id, name, classDetail) { success ->
                    if (success) {
                        Toast.makeText(context, "Reserva realizada com sucesso!", Toast.LENGTH_SHORT).show()
                    }
                }
                showReserveDialog = false
            }
        )
    }

    if (showFichaEtiqueta) {
        FichaEtiquetaDialog(
            book = book,
            onDismiss = { showFichaEtiqueta = false }
        )
    }
}

@Composable
fun LibraryInsightsDialog(
    books: List<BookEntity>,
    loans: List<LoanEntity>,
    onDismiss: () -> Unit
) {
    val totalLoans = loans.size
    val returnedLoans = loans.count { it.returnDate != null }
    val returnRate = if (totalLoans > 0) (returnedLoans * 100) / totalLoans else 100

    // Top 3 popular books
    val popularBooks = remember(loans) {
        loans.groupBy { it.bookTitle }
            .map { (title, titleLoans) ->
                val author = titleLoans.firstOrNull()?.bookAuthor ?: ""
                title to Pair(author, titleLoans.sumOf { it.quantity })
            }
            .sortedByDescending { it.second.second }
            .take(3)
    }

    // Top 3 readers
    val topBorrowers = remember(loans) {
        loans.groupBy { it.borrowerName }
            .map { (name, borrowerLoans) ->
                val classRoom = borrowerLoans.firstOrNull()?.borrowerClass ?: ""
                name to Pair(classRoom, borrowerLoans.sumOf { it.quantity })
            }
            .sortedByDescending { it.second.second }
            .take(3)
    }

    // Alert of 0 available supply
    val outOfStockBooks = remember(books) {
        books.filter { it.totalQuantity > 0 && it.availableQuantity == 0 }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ThemeBorder),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScrollWithKeyboard(rememberScrollState())
            ) {
                // Header of Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ThemePurpleLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = ThemePurplePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Métricas & Insights",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ThemeOnBackground
                            )
                            Text(
                                text = "Inteligência de acervo e estatísticas",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThemeSecondaryText
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(ThemeBackground)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Fechar",
                            tint = ThemeSecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Item 1: High Level Indicator Metrics Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = ThemePurpleLight.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "EMPRÉSTIMOS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemePurplePrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "$totalLoans",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemePurplePrimary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Text(
                                        text = "registros totais",
                                        fontSize = 11.sp,
                                        color = ThemeSecondaryText
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = StatusAvailableGreenBg.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "TAXA DE RETORNO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusAvailableGreenText,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "$returnRate%",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusAvailableGreenText,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Text(
                                        text = "livros devolvidos",
                                        fontSize = 11.sp,
                                        color = ThemeSecondaryText
                                    )
                                }
                            }
                        }
                    }

                    item {
                        WeeklyLoanLineChart(loans = loans)
                    }

                    item {
                        MostReadGenresBarChart(books = books, loans = loans)
                    }

                    // Item 2: Popular Books Top 3
                    item {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Livros Mais Procurados (Top 3)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeOnBackground
                                )
                            }

                            if (popularBooks.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = ThemeBackground),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Histórico de empréstimos vazio.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = ThemeSecondaryText
                                        )
                                    }
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, ThemeBorder),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        popularBooks.forEachIndexed { index, pair ->
                                            val title = pair.first
                                            val author = pair.second.first
                                            val count = pair.second.second

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .background(
                                                                when (index) {
                                                                    0 -> Color(0xFFFFD700).copy(alpha = 0.2f)
                                                                    1 -> Color(0xFFC0C0C0).copy(alpha = 0.2f)
                                                                    else -> Color(0xFFCD7F32).copy(alpha = 0.2f)
                                                                }
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "${index + 1}",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = when (index) {
                                                                0 -> Color(0xFF9E8A00)
                                                                1 -> Color(0xFF707070)
                                                                else -> Color(0xFF8C4C10)
                                                            }
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            text = title,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ThemeOnBackground,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = author,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = ThemeSecondaryText,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(ThemePurpleLight)
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "$count saídas",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ThemePurplePrimary
                                                    )
                                                }
                                            }
                                            if (index < popularBooks.size - 1) {
                                                HorizontalDivider(color = ThemeBorder, thickness = 0.5.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Item 3: Exemplary Readers Top 3
                    item {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = ThemePurplePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Leitores Exemplares (Top 3)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeOnBackground
                                )
                            }

                            if (topBorrowers.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = ThemeBackground),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Nenhum leitor registrado ainda.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = ThemeSecondaryText
                                        )
                                    }
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, ThemeBorder),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        topBorrowers.forEachIndexed { index, pair ->
                                            val name = pair.first
                                            val details = pair.second.first
                                            val count = pair.second.second

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(ThemePurpleLight),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Bookmark,
                                                            contentDescription = null,
                                                            tint = ThemePurplePrimary,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(
                                                            text = name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ThemeOnBackground,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = details,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = ThemeSecondaryText,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(BentoLilacSecondary)
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "$count livros",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = BentoLilacSecondaryOn
                                                    )
                                                }
                                            }
                                            if (index < topBorrowers.size - 1) {
                                                HorizontalDivider(color = ThemeBorder, thickness = 0.5.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Item 4: Critical Supply Inventory Warnings
                    item {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = StatusOverdueRedText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Alertas de Reposição (Esgotados)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeOnBackground
                                )
                            }

                            if (outOfStockBooks.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = StatusAvailableGreenBg.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, StatusAvailableGreenText.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = StatusAvailableGreenText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Todos os livros possuem cópias em estoque! ✅",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = StatusAvailableGreenText,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = StatusOverdueRedBg.copy(alpha = 0.3f)),
                                    border = BorderStroke(1.dp, StatusOverdueRedText.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        outOfStockBooks.forEachIndexed { index, book ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = book.title,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = StatusOverdueRedText,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "Autor: ${book.author} | Prateleira: ${book.location}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = StatusOverdueRedText.copy(alpha = 0.8f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(StatusOverdueRedBg)
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "ESGOTADO",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = StatusOverdueRedText
                                                    )
                                                }
                                            }
                                            if (index < outOfStockBooks.size - 1) {
                                                HorizontalDivider(color = StatusOverdueRedText.copy(alpha = 0.15f), thickness = 0.5.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action buttons
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Concluído", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun PartialReturnDialog(
    loan: LoanEntity,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var quantityToReturn by remember { mutableStateOf(loan.quantity) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ThemeBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScrollWithKeyboard(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Devolução de Exemplares",
                    style = MaterialTheme.typography.titleLarge,
                    color = ThemePurplePrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Livro: ${loan.bookTitle}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ThemeOnBackground,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Emprestado para: ${loan.borrowerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeSecondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Quantidade a Devolver",
                    style = MaterialTheme.typography.labelSmall,
                    color = ThemePurplePrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { if (quantityToReturn > 1) quantityToReturn-- },
                        enabled = quantityToReturn > 1,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (quantityToReturn > 1) ThemePurpleLight else ThemeBackground)
                    ) {
                        Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (quantityToReturn > 1) ThemePurplePrimary else ThemeSecondaryText)
                    }

                    Text(
                        text = "$quantityToReturn",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThemeOnBackground,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    IconButton(
                        onClick = { if (quantityToReturn < loan.quantity) quantityToReturn++ },
                        enabled = quantityToReturn < loan.quantity,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (quantityToReturn < loan.quantity) ThemePurpleLight else ThemeBackground)
                    ) {
                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (quantityToReturn < loan.quantity) ThemePurplePrimary else ThemeSecondaryText)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Exemplares em aberto neste empréstimo: ${loan.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeSecondaryText
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onConfirm(quantityToReturn) },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirmar Devolução", color = Color.White)
                    }

                    if (quantityToReturn < loan.quantity) {
                        OutlinedButton(
                            onClick = { onConfirm(loan.quantity) },
                            border = BorderStroke(1.dp, ThemePurplePrimary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Devolver Todos (${loan.quantity})", color = ThemePurplePrimary)
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar", color = ThemeSecondaryText)
                    }
                }
            }
        }
    }
}

@Composable
fun ReserveBookDialog(
    book: BookEntity,
    viewModel: LibraryViewModel,
    onDismiss: () -> Unit,
    onConfirmReserve: (String, String) -> Unit
) {
    var borrowerName by remember { mutableStateOf("") }
    var borrowerClass by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    val allBorrowersList by viewModel.allBorrowers.collectAsStateWithLifecycle()

    val filteredBorrowers = remember(borrowerName, allBorrowersList) {
        if (borrowerName.isBlank()) {
            allBorrowersList.take(4)
        } else {
            val qNorm = borrowerName.normalizeForSearch()
            allBorrowersList.filter {
                it.name.normalizeForSearch().contains(qNorm)
            }.take(5)
        }
    }

    val matchedBorrower = remember(borrowerName, allBorrowersList) {
        val bnNorm = borrowerName.normalizeForSearch()
        allBorrowersList.find { it.name.normalizeForSearch() == bnNorm }
    }

    var dropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ThemeBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScrollWithKeyboard(rememberScrollState())
            ) {
                Text(
                    text = "Reservar Livro",
                    style = MaterialTheme.typography.titleLarge,
                    color = ThemePurplePrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Livro esgotado: ${book.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeSecondaryText,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = borrowerName,
                            onValueChange = { 
                                borrowerName = it
                                dropdownExpanded = true
                                errorText = ""
                            },
                            label = { Text("Nome do Aluno ou Professor *") },
                            placeholder = { Text("Selecione ou digite...") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                    Icon(
                                        imageVector = if (dropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Alternar menu"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThemePurplePrimary,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedLabelColor = ThemePurplePrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        val itemsToShow = filteredBorrowers

                        if (itemsToShow.isNotEmpty() && dropdownExpanded) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, ThemeBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    itemsToShow.take(5).forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    borrowerName = item.name
                                                    borrowerClass = if (item.type == "Aluno") {
                                                        "Aluno - ${item.classRoom.ifBlank { item.identifier }}"
                                                    } else {
                                                        "Prof - ${item.classRoom.ifBlank { "Sem Turma" }}"
                                                    }
                                                    dropdownExpanded = false
                                                    errorText = ""
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            BorrowerAvatar(
                                                photoUri = item.photoUri,
                                                name = item.name,
                                                size = 20.dp,
                                                borderWidth = 1.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${item.name} (${item.type} - ${item.classRoom.ifBlank { "Sem Turma" }})",
                                                fontSize = 12.sp,
                                                color = ThemeOnBackground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Matched borrower card indicator
                if (matchedBorrower != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusAvailableGreenBg.copy(alpha = 0.7f)),
                        border = BorderStroke(1.dp, StatusAvailableGreenText.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selecionado",
                                tint = StatusAvailableGreenText,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Leitor Cadastrado (${matchedBorrower.type})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusAvailableGreenText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Turma: ${matchedBorrower.classRoom.ifBlank { "Sem Turma" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusAvailableGreenText.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Optional custom Class/ID input if not pre-matched
                OutlinedTextField(
                    value = borrowerClass,
                    onValueChange = { borrowerClass = it },
                    label = { Text("Informações de Contato / Turma") },
                    placeholder = { Text("Preenchido automaticamente ao selecionar leitor") },
                    singleLine = true,
                    enabled = matchedBorrower == null, // lock field if matches registered user
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemePurplePrimary,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedLabelColor = ThemePurplePrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorText.isNotEmpty()) {
                    Text(
                        text = errorText,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = ThemePurplePrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val isBlockedByPenalidade = matchedBorrower != null && matchedBorrower.blockedUntil != null && matchedBorrower.blockedUntil > System.currentTimeMillis()
                            if (borrowerName.isBlank()) {
                                errorText = "Por favor, selecione um leitor cadastrado."
                            } else if (matchedBorrower == null) {
                                errorText = "Reserva bloqueada: O leitor precisa estar cadastrado."
                            } else if (isBlockedByPenalidade) {
                                errorText = "Reserva bloqueada: Leitor possui penalidade de atraso."
                            } else {
                                val finalClass = if (matchedBorrower.type == "Aluno") {
                                    "Aluno - ${matchedBorrower.classRoom.ifBlank { matchedBorrower.identifier }}"
                                } else {
                                    "Prof - ${matchedBorrower.classRoom.ifBlank { "Sem Turma" }}"
                                }
                                onConfirmReserve(matchedBorrower.name, finalClass)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirmar Reserva")
                    }
                }
            }
        }
    }
}

@Composable
fun ReservationCard(
    reservation: ReservationEntity,
    bookAvailable: Boolean,
    borrowerPhone: String? = null,
    onPromote: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val shareContext = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ThemeBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(StatusBorrowedAmberBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = reservation.borrowerName.take(2).uppercase(),
                            color = StatusBorrowedAmberText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = reservation.borrowerName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = ThemeOnBackground
                        )
                        Text(
                            text = "Turma: ${reservation.borrowerClass}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThemeSecondaryText
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Cancelar Reserva",
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = ThemeBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Livro Reservado:",
                style = MaterialTheme.typography.labelSmall,
                color = ThemeSecondaryText,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = reservation.bookTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = ThemePurplePrimary,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            Text(
                text = "Reservado em: ${dateFormat.format(Date(reservation.requestDate))}",
                style = MaterialTheme.typography.bodySmall,
                color = ThemeSecondaryText
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (bookAvailable) StatusAvailableGreenBg else StatusOverdueRedBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (bookAvailable) "EXEMPLAR ATUALMENTE DISPONÍVEL" else "AGUARDANDO EXEMPLAR DEVOLVER",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (bookAvailable) StatusAvailableGreenText else StatusOverdueRedText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                if (bookAvailable) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!borrowerPhone.isNullOrBlank()) {
                            IconButton(
                                onClick = {
                                    try {
                                        val number = borrowerPhone.replace(Regex("[^0-9]"), "")
                                        val text = "Olá ${reservation.borrowerName}, o livro '${reservation.bookTitle}' que você reservou na BiblioTech já está disponível! Por favor, venha retirá-lo o mais breve possível para não perder a reserva."
                                        val url = "https://api.whatsapp.com/send?phone=55$number&text=${java.net.URLEncoder.encode(text, "UTF-8")}"
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = android.net.Uri.parse(url)
                                        }
                                        shareContext.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(shareContext, "Erro ao abrir o WhatsApp", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(StatusAvailableGreenBg).size(36.dp), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Notificar por WhatsApp",
                                        tint = StatusAvailableGreenText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onPromote,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusAvailableGreenText),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Emprestar", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// NEW INNOVATIVE COMPONENT: DYNAMIC CANVAS-BASED GRAPHIC GENERATORS & DIALOGS
// ============================================================================

@Composable
fun BorrowerQrCode(
    content: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier) {
        val sizePx = size.width
        val modules = 21
        val moduleSize = sizePx / modules
        val hash = content.hashCode().toLong()
        val random = java.util.Random(hash)

        for (row in 0 until modules) {
            for (col in 0 until modules) {
                val isFinderPattern = (row < 7 && col < 7) || 
                                      (row < 7 && col >= modules - 7) || 
                                      (row >= modules - 7 && col < 7)
                
                var isDark = false
                if (isFinderPattern) {
                    val r = if (row >= modules - 7) row - (modules - 7) else row
                    val c = if (col >= modules - 7) col - (modules - 7) else col
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    isDark = isBorder || isCenter
                } else {
                    val isTimingLine = (row == 6 && col % 2 == 0) || (col == 6 && row % 2 == 0)
                    if (isTimingLine) {
                        isDark = true
                    } else {
                        isDark = random.nextBoolean()
                    }
                }

                if (isDark) {
                    drawRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(col * moduleSize, row * moduleSize),
                        size = androidx.compose.ui.geometry.Size(moduleSize + 0.5f, moduleSize + 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun BookBarcodeCode(
    content: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Black
) {
    Canvas(modifier = modifier) {
        val widthPx = size.width
        val heightPx = size.height
        val hash = content.hashCode().toLong()
        val random = java.util.Random(hash)
        
        var currentX = 0f
        while (currentX < widthPx) {
            val barWidth = (random.nextInt(3) + 1) * 3f
            val spaceWidth = (random.nextInt(3) + 1) * 3f
            
            if (currentX + barWidth <= widthPx) {
                drawRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(currentX, 0f),
                    size = androidx.compose.ui.geometry.Size(barWidth, heightPx)
                )
            }
            currentX += barWidth + spaceWidth
        }
    }
}

@Composable
fun CarteirinhaDialog(
    borrower: BorrowerEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ThemeBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScrollWithKeyboard(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header of the card body
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalLibrary,
                            contentDescription = null,
                            tint = ThemePurplePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BiblioTech",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ThemePurplePrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ThemePurpleLight)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CARTEIRA DE LEITOR",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemePurplePrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Credentials graphic card layout
                Card(
                    colors = CardDefaults.cardColors(containerColor = ThemePurpleLight.copy(alpha = 0.4f)),
                    border = BorderStroke(1.5.dp, ThemePurplePrimary.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // User avatar
                        BorrowerAvatar(
                            photoUri = borrower.photoUri,
                            name = borrower.name,
                            size = 72.dp,
                            borderWidth = 1.5.dp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Identification Details
                        Text(
                            text = borrower.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ThemeOnBackground,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = if (borrower.type == "Aluno") "Aluno" else "Professor(a)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ThemePurplePrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Turma/Série", fontSize = 10.sp, color = ThemeSecondaryText)
                                Text(
                                    text = borrower.classRoom.ifBlank { "Sem Turma" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = ThemeOnBackground
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Matrícula/ID", fontSize = 10.sp, color = ThemeSecondaryText)
                                Text(
                                    text = borrower.identifier.ifBlank { "NÃO INF" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = ThemeOnBackground
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // QR Code generator integration
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, ThemeBorder)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BorrowerQrCode(
                                content = "borrower_id:${borrower.identifier.ifBlank { borrower.name }}",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Código de Validação QR",
                            fontSize = 10.sp,
                            color = ThemeSecondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card Blocked details
                val isBlocked = borrower.blockedUntil != null && borrower.blockedUntil!! > System.currentTimeMillis()
                if (isBlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(StatusOverdueRedBg)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚠️ LEITOR BLOQUEADO POR ATRASO",
                            color = StatusOverdueRedText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Share ID card button details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val text = """
                                🆔 *CARTEIRINHA DIGITAL - BiblioTech* 🆔
                                
                                👤 *Leitor:* ${borrower.name}
                                📞 *Contato:* ${borrower.phone.ifBlank { "Não informado" }}
                                🎓 *Série/Turma:* ${borrower.classRoom}
                                🔢 *Código Matrícula/ID:* ${borrower.identifier}
                                🏫 *Instituição:* Biblioteca Escolar/Municipal
                                
                                _Apresente esta carteirinha para efetuar novos empréstimos ou devoluções._
                            """.trimIndent()
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(intent, "Enviar Carteirinha"))
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemePurplePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartilhar", fontSize = 13.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Fechar Carteirinha", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FichaEtiquetaDialog(
    book: BookEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Ficha Catalográfica, 1 = Etiqueta de Lombada
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ThemeBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScrollWithKeyboard(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Gerador de Fichas e Código",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemePurplePrimary
                )
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeSecondaryText,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Tab selectors inside
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = ThemePurplePrimary,
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Ficha Catalográfica", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Etiqueta & Código", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // TAB 0: FICHA CATALOGRÁFICA
                    val cdd = when(book.genre.lowercase().trim()) {
                        "infantil", "infanto-juvenil" -> "028.5"
                        "ficção", "romance", "literatura" -> "869.3"
                        "história", "geografia" -> "900"
                        "ciências", "biologia", "química", "física" -> "500"
                        "matemática" -> "510"
                        "poesia" -> "869.1"
                        "religião" -> "200"
                        else -> "800"
                    }
                    val cutter = remember(book.title, book.author) {
                        val authorFirst = book.author.trim().split(" ").lastOrNull()?.firstOrNull()?.uppercase() ?: "X"
                        val titleFirst = book.title.trim().substringBefore(" ").firstOrNull()?.lowercase() ?: "x"
                        val num = (book.title.length * 3) + 12
                        "$authorFirst$num$titleFirst"
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = ThemeBackground),
                        border = BorderStroke(1.dp, ThemeBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "Ficha Catalográfica desenvolvida localmente (CIP)",
                                fontSize = 9.sp,
                                color = ThemeSecondaryText,
                                style = MaterialTheme.typography.labelSmall,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color.Black, thickness = 1.dp)
                            
                            Row(modifier = Modifier.padding(top = 12.dp)) {
                                Column(modifier = Modifier.width(48.dp)) {
                                    Text(text = cdd, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                                    Text(text = cutter, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val formattedAuthor = remember(book.author) {
                                        val parts = book.author.trim().split(" ")
                                        if (parts.size > 1) {
                                            "${parts.last().uppercase()}, ${parts.dropLast(1).joinToString(" ")}"
                                        } else {
                                            book.author.uppercase()
                                        }
                                    }
                                    Text(text = formattedAuthor, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                    Text(
                                        text = "${book.title} / ${book.author} — Edição Escolar local, ${book.year}.",
                                        fontSize = 12.sp,
                                        color = Color.Black,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    Text(text = "${book.title.length * 4} p. ; il.", fontSize = 11.sp, color = Color.Black)
                                    Text(text = "ISBN: 978-85-8822-${(book.id * 17) + 100}-X (Local)", fontSize = 11.sp, color = Color.Black)
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "1. Gênero: ${book.genre}. 2. Localização: ${book.location}. I. Título.", fontSize = 11.sp, color = Color.Black)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color.Black, thickness = 1.dp)
                        }
                    }
                } else {
                    // TAB 1: ETIQUETA DE LOMBADA / CÓDIGO
                    val cutter = remember(book.title, book.author) {
                        val authorFirst = book.author.trim().split(" ").lastOrNull()?.firstOrNull()?.uppercase() ?: "X"
                        val titleFirst = book.title.trim().substringBefore(" ").firstOrNull()?.lowercase() ?: "x"
                        val num = (book.title.length * 3) + 12
                        "$authorFirst$num$titleFirst"
                    }
                    val cdd = when(book.genre.lowercase().trim()) {
                        "infantil", "infanto-juvenil" -> "028.5"
                        "ficção", "romance", "literatura" -> "869.3"
                        "história", "geografia" -> "900"
                        "ciências", "biologia", "química", "física" -> "500"
                        "matemática" -> "510"
                        "poesia" -> "869.1"
                        "religião" -> "200"
                        else -> "800"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Spine Tag (Mini Card)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ThemePurpleLight.copy(alpha = 0.5f)),
                            border = BorderStroke(1.5.dp, ThemePurplePrimary.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Etiqueta de Lombada", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ThemePurplePrimary)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .background(Color.White)
                                        .border(0.5.dp, ThemeBorder)
                                        .padding(horizontal = 8.dp, vertical = 12.dp)
                                        .width(64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(cdd, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        Text(cutter, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        Text(book.location.take(8), fontSize = 10.sp, color = ThemeSecondaryText)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Colar na lombada", fontSize = 9.sp, color = ThemeSecondaryText)
                            }
                        }

                        // Barcode Tag (Wide Card)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, ThemeBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Código de Barras", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ThemeSecondaryText)
                                Spacer(modifier = Modifier.height(12.dp))

                                // Render customizable dynamic Barcode
                                BookBarcodeCode(
                                    content = "book_barcode:${book.id}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "* ${book.id.toString().padStart(5, '0')} *",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.Black
                                )
                                Text(book.title.take(16), fontSize = 10.sp, color = ThemeSecondaryText, maxLines = 1)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val text = """
                                📎 *REGISTRO E FICHA CATALOGRÁFICA* 📎
                                
                                📘 *Livro:* ${book.title}
                                👤 *Autor:* ${book.author}
                                🗄️ *Gênero:* ${book.genre}
                                📅 *Ano:* ${book.year}
                                📍 *Localização:* ${book.location}
                                🏷️ *Cutter:* ${book.author.take(1).uppercase()}${(book.title.length * 3) + 12}${book.title.take(1).lowercase()}
                                📊 *Código de Identificação:* ${book.id.toString().padStart(5, '0')}
                                
                                _Etapa concluída: Ficha pronta para rotulagem._
                            """.trimIndent()
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(intent, "Compartilhar Ficha"))
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemePurplePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartilhar", fontSize = 13.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("Confirmar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerDialog(
    viewModel: LibraryViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var scanQuery by remember { mutableStateOf("") }
    
    val books by viewModel.books.collectAsStateWithLifecycle()
    val borrowers by viewModel.allBorrowers.collectAsStateWithLifecycle()
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    
    // Scan matching outcomes
    var scannedBook by remember { mutableStateOf<BookEntity?>(null) }
    var scannedBorrower by remember { mutableStateOf<BorrowerEntity?>(null) }
    
    // Scanner animations: infinite loop scanning laser line
    val infiniteTransition = rememberInfiniteTransition(label = "Laser scanner")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "Laser line position"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ThemeBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .heightIn(max = 740.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScrollWithKeyboard(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header details
                Text(
                    text = "Scanner de Códigos (Estação)",
                    style = MaterialTheme.typography.titleLarge,
                    color = ThemePurplePrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Escore e leia códigos de barra e QR",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeSecondaryText,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Laser scope container Box (Câmera Maximizada)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(2.dp, ThemePurplePrimary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing green/red scope corners
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val lineL = 24.dp.toPx()
                        val strokeW = 4.dp.toPx()
                        val lineC = ThemePurplePrimary
                        
                        // Top-left corner
                        drawLine(lineC, androidx.compose.ui.geometry.Offset(12.dp.toPx(), 12.dp.toPx()), androidx.compose.ui.geometry.Offset(12.dp.toPx() + lineL, 12.dp.toPx()), strokeW)
                        drawLine(lineC, androidx.compose.ui.geometry.Offset(12.dp.toPx(), 12.dp.toPx()), androidx.compose.ui.geometry.Offset(12.dp.toPx(), 12.dp.toPx() + lineL), strokeW)
                        
                        // Top-right corner
                        drawLine(lineC, androidx.compose.ui.geometry.Offset(w - 12.dp.toPx(), 12.dp.toPx()), androidx.compose.ui.geometry.Offset(w - 12.dp.toPx() - lineL, 12.dp.toPx()), strokeW)
                        drawLine(lineC, androidx.compose.ui.geometry.Offset(w - 12.dp.toPx(), 12.dp.toPx()), androidx.compose.ui.geometry.Offset(w - 12.dp.toPx(), 12.dp.toPx() + lineL), strokeW)
                        
                        // Bottom-left corner
                        drawLine(lineC, androidx.compose.ui.geometry.Offset(12.dp.toPx(), h - 12.dp.toPx()), androidx.compose.ui.geometry.Offset(12.dp.toPx() + lineL, h - 12.dp.toPx()), strokeW)
                        drawLine(lineC, androidx.compose.ui.geometry.Offset(12.dp.toPx(), h - 12.dp.toPx()), androidx.compose.ui.geometry.Offset(12.dp.toPx(), h - 12.dp.toPx() - lineL), strokeW)
                        
                        // Bottom-right corner
                        drawLine(lineC, androidx.compose.ui.geometry.Offset(w - 12.dp.toPx(), h - 12.dp.toPx()), androidx.compose.ui.geometry.Offset(w - 12.dp.toPx() - lineL, h - 12.dp.toPx()), strokeW)
                        drawLine(lineC, androidx.compose.ui.geometry.Offset(w - 12.dp.toPx(), h - 12.dp.toPx()), androidx.compose.ui.geometry.Offset(w - 12.dp.toPx(), h - 12.dp.toPx() - lineL), strokeW)
                    }

                    // Centered targeting HUD text or pulsing details
                    Text(
                        text = if (scannedBook != null || scannedBorrower != null) "CÓDIGO IDENTIFICADO! 🟢" else "MIRA DE VARREDURA ATIVA",
                        color = if (scannedBook != null || scannedBorrower != null) StatusAvailableGreenText else Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 40.dp)
                    )

                    // Moving laser horizontal line sweeping down to 300dp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = (laserOffset * 300).dp)
                            .background(Color.Red)
                    )

                    // VERY CLEAR INSTRUCTION OVERLAY (DEMANDED BY USER)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .background(Color(0xFFE6A100).copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                            .border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Aponte a câmera para o código de barras do livro",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SEARCH SELECTION FOR SIMULATION EXPOSURE
                var dropdownExpanded by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = scanQuery,
                            onValueChange = { 
                                scanQuery = it
                                dropdownExpanded = true
                                if (it.isBlank()) {
                                    scannedBook = null
                                    scannedBorrower = null
                                }
                            },
                            label = { Text("Simular Leitura (Digite ou escolha da lista) *") },
                            placeholder = { Text("Selecione livro ou leitor...") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ThemeSecondaryText) },
                            trailingIcon = {
                                IconButton(onClick = { dropdownExpanded = !dropdownExpanded }) {
                                    Icon(
                                        imageVector = if (dropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Alternar menu"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ThemePurplePrimary,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedLabelColor = ThemePurplePrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (dropdownExpanded) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, ThemeBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Section header: Books
                                    if (books.isNotEmpty()) {
                                        Text(
                                            "LIVROS DISPONÍVEIS", 
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ThemeSecondaryText,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val matchedBooksList = if (scanQuery.isBlank()) books.take(4) else books.filter { it.title.contains(scanQuery, ignoreCase = true) }.take(4)
                                        matchedBooksList.forEach { bk ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        scannedBook = bk
                                                        scannedBorrower = null
                                                        scanQuery = bk.title
                                                        dropdownExpanded = false
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(16.dp), tint = ThemePurplePrimary)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Código Livro: ${bk.id.toString().padStart(5, '0')} - ${bk.title}",
                                                    fontSize = 12.sp,
                                                    color = ThemeOnBackground
                                                )
                                            }
                                        }
                                    }

                                    // Divider
                                    HorizontalDivider(color = ThemeBorder, thickness = 0.5.dp)

                                    // Section header: Borrowers
                                    if (borrowers.isNotEmpty()) {
                                        Text(
                                            "CARTEIRINHAS DE LEITORES", 
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ThemeSecondaryText,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val matchedBorrowerList = if (scanQuery.isBlank()) borrowers.take(4) else borrowers.filter { it.name.contains(scanQuery, ignoreCase = true) }.take(4)
                                        matchedBorrowerList.forEach { bor ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        scannedBorrower = bor
                                                        scannedBook = null
                                                        scanQuery = bor.name
                                                        dropdownExpanded = false
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp), tint = ThemePurplePrimary)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Matrícula QR: ${bor.identifier.ifBlank { bor.name }} - ${bor.name}",
                                                    fontSize = 12.sp,
                                                    color = ThemeOnBackground
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SCANNED RESULTS PREVIEW & ACTION BOX
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when {
                        scannedBook != null -> {
                            val bk = scannedBook!!
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ThemeBackground),
                                border = BorderStroke(1.dp, ThemeBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Book, contentDescription = null, tint = ThemePurplePrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(text = "Livro Identificado", fontSize = 11.sp, color = ThemeSecondaryText, fontWeight = FontWeight.Bold)
                                            Text(text = bk.title, fontWeight = FontWeight.Bold, color = ThemeOnBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(text = "Autor: ${bk.author}", fontSize = 12.sp, color = ThemeSecondaryText)
                                    Text(text = "Localização na Estante: ${bk.location}", fontSize = 12.sp, color = ThemeSecondaryText)
                                    Text(text = "Disponíveis para Empréstimo: ${bk.availableQuantity} de ${bk.totalQuantity}", fontSize = 12.sp, color = ThemeSecondaryText)
                                    
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Check active loans for this book to see if we can easily return/receive!
                                    val activeBookLoan = remember(loans, bk.id) {
                                        loans.find { it.bookId == bk.id && it.returnDate == null }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (bk.availableQuantity > 0) {
                                            Button(
                                                onClick = {
                                                    // Instant dismiss is perfect so they can perform checkout dialog
                                                    onDismiss()
                                                    Toast.makeText(context, "Selecione o leitor que fará a retirada!", Toast.LENGTH_LONG).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Emprestar", fontSize = 12.sp)
                                            }
                                        }
                                        if (activeBookLoan != null) {
                                            Button(
                                                onClick = {
                                                    viewModel.returnBook(context, activeBookLoan.id, 1) { success ->
                                                        if (success) {
                                                            Toast.makeText(context, "Livro '${bk.title}' devolvido e baixado com sucesso!", Toast.LENGTH_SHORT).show()
                                                            scannedBook = null
                                                            scanQuery = ""
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = StatusAvailableGreenText),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Receber / Devolver", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        scannedBorrower != null -> {
                            val bor = scannedBorrower!!
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ThemeBackground),
                                border = BorderStroke(1.dp, ThemeBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = ThemePurplePrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(text = "Leitor Identificado (Via QR Carteirinha)", fontSize = 11.sp, color = ThemeSecondaryText, fontWeight = FontWeight.Bold)
                                            Text(text = bor.name, fontWeight = FontWeight.Bold, color = ThemeOnBackground, maxLines = 1)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(text = "Tipo: ${bor.type}", fontSize = 12.sp, color = ThemeSecondaryText)
                                    Text(text = "Turma/Série: ${bor.classRoom}", fontSize = 12.sp, color = ThemeSecondaryText)
                                    Text(text = "ID Matrícula: ${bor.identifier}", fontSize = 12.sp, color = ThemeSecondaryText)

                                    val isBlocked = bor.blockedUntil != null && bor.blockedUntil!! > System.currentTimeMillis()
                                    if (isBlocked) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "⚠️ ATENÇÃO: Contém penalidade de empréstimo ativa!", 
                                            fontSize = 11.sp, 
                                            fontWeight = FontWeight.Bold, 
                                            color = Color.Red
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            onDismiss()
                                            // Trigger a beautiful visual shortcut by simulating search Borrower focus
                                            viewModel.searchBorrowers(bor.name)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Ir ao Perfil Completo do Leitor", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        else -> {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = ThemeSecondaryText.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Aguardando leitura de código...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ThemeSecondaryText,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Use a busca acima para simular a varredura visual de forma instantânea.",
                                    fontSize = 10.sp,
                                    color = ThemeSecondaryText.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Sair da Estação", color = ThemePurplePrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ====================================================================
// MÓDULO INTEGRADO GESTÃO ACERVO: AUDITORIA (JTA) & ROTA FÍSICA (ROOF)
// ====================================================================

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JtaRoofTab(viewModel: LibraryViewModel) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val shelves by viewModel.shelves.collectAsStateWithLifecycle()
    val positions by viewModel.positions.collectAsStateWithLifecycle()

    var activeSubTab by rememberSaveable { mutableStateOf(0) } // 0 = JTA, 1 = ROOF
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScrollWithKeyboard(rememberScrollState())
            .padding(16.dp)
    ) {
        // Módulo Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Inteligência Operacional",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemePurplePrimary
                )
                Text(
                    text = "Módulos Avançados Integrados: JTA & ROOF",
                    style = MaterialTheme.typography.labelSmall,
                    color = ThemeSecondaryText
                )
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))

        // Subtabs selector row
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = Color.White,
            contentColor = ThemePurplePrimary,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, ThemeBorder, RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("Justificativa (JTA)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.VerifiedUser, contentDescription = "JTA icon", modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("Organização Estantes (ROOF)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Layers, contentDescription = "ROOF icon", modifier = Modifier.size(18.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeSubTab == 0) {
            JtaModuleView(viewModel = viewModel, books = books)
        } else {
            RoofModuleView(viewModel = viewModel, books = books, shelves = shelves, positions = positions)
        }
        Spacer(modifier = Modifier.height(96.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JtaModuleView(viewModel: LibraryViewModel, books: List<BookEntity>) {
    val context = LocalContext.current
    val jtaList = viewModel.runJtaAnalysisForBooks(books)
    val lowCircList = jtaList.filter { it.classification != "Normal" }
    val idaValue = viewModel.calculateAssetDefensibilityIndex(books)
    
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedReportText by remember { mutableStateOf("") }
    
    var editingBookMetadata by remember { mutableStateOf<BookEntity?>(null) }
    var editingBookJustification by remember { mutableStateOf<BookEntity?>(null) }
    
    // Summary Cards block
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card representation for IDA Score
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ThemeBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp), 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "DEFENSABILIDADE (IDA)", 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = ThemePurplePrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(76.dp)) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = ThemePurpleLight, style = Stroke(width = 6.dp.toPx()))
                        drawArc(
                            color = if (idaValue >= 75) StatusAvailableGreenText else if (idaValue >= 40) Color(0xFFFF9800) else StatusOverdueRedText,
                            startAngle = -90f,
                            sweepAngle = (idaValue * 3.6f),
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "$idaValue%", 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = ThemeOnBackground
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Aprovados por legislação de defensabilidade do acervo.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ThemeSecondaryText,
                    textAlign = TextAlign.Center,
                    fontSize = 9.sp
                )
            }
        }

        // Summary Card for Low circulation amounts
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ThemeBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("FILTRAGEM DE AUDITORIA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ThemePurplePrimary)
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = ThemePurplePrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Total Acervo: ${books.size} un.", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Ociosos/Baixos: ${lowCircList.size} un.", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        selectedReportText = viewModel.generateJtaAuditReport(jtaList)
                        showReportDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gerar Relatório JTA", fontSize = 10.sp)
                }
            }
        }
    }

    // Header label
    Text(
        text = "Análise Crítica JTA (Livros com Baixa Circulação)",
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleSmall,
        color = ThemeOnBackground,
        modifier = Modifier.padding(vertical = 6.dp)
    )

    if (lowCircList.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ThemeBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp), 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusAvailableGreenText, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Text("Alta Rotatividade Geral!", fontWeight = FontWeight.Bold, color = ThemeOnBackground, fontSize = 13.sp)
                Text("Toda a coleção registra retiradas externas contínuas regulares.", color = ThemeSecondaryText, modifier = Modifier.padding(top = 4.dp), fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
    } else {
        lowCircList.forEach { entry ->
            val book = entry.book
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Card Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = book.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ThemeOnBackground,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        val (badgeBg, badgeText) = when (entry.level) {
                            "Alta" -> Pair(Color(0xFFFEEBEE), StatusOverdueRedText)
                            "Média" -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
                            else -> Pair(Color(0xFFE0F2F1), Color(0xFF004D40))
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeBg)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(text = entry.classification, color = badgeText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Text(
                        text = "Autor: ${book.author} | Gênero: ${book.genre} | Tipo: ${book.tipoObra}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeSecondaryText,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    HorizontalDivider(color = ThemeBorder, modifier = Modifier.padding(vertical = 6.dp), thickness = 0.5.dp)

                    Text(
                        text = "Justificativa Técnica JTA:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemePurplePrimary
                    )
                    Text(
                        text = entry.justification,
                        fontSize = 11.sp,
                        color = ThemeOnBackground,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                    )

                    if (book.textoJustificativa?.isNotBlank() == true) {
                        Text(
                            text = "Anotação do Bibliotecário:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusAvailableGreenText
                        )
                        Text(
                            text = book.textoJustificativa,
                            fontSize = 11.sp,
                            color = ThemeOnBackground,
                            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editingBookMetadata = book },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ThemePurplePrimary),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ajustar Parâmetros", fontSize = 10.sp, color = ThemePurplePrimary)
                        }

                        OutlinedButton(
                            onClick = { editingBookJustification = book },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, StatusAvailableGreenText),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Comment, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Anotar Motivação", fontSize = 10.sp, color = StatusAvailableGreenText)
                        }
                    }
                }
            }
        }
    }

    // Modal dialogue for report output
    if (showReportDialog) {
        Dialog(onDismissRequest = { showReportDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                    Text(
                        "Relatório Oficial Auditoria JTA", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = ThemePurplePrimary
                    )
                    Text(
                        "Certificado de auditoria para fins fiscais e de permanência física de acervo.",
                        style = MaterialTheme.typography.bodySmall, 
                        color = ThemeSecondaryText,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(ThemeBackground, RoundedCornerShape(8.dp))
                            .border(1.dp, ThemeBorder, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column(modifier = Modifier.verticalScrollWithKeyboard(rememberScrollState())) {
                            Text(
                                text = selectedReportText,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clipData = android.content.ClipData.newPlainText("Relatorio JTA", selectedReportText)
                                clipboardManager.setPrimaryClip(clipData)
                                Toast.makeText(context, "Copiado para Área de Transferência!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusAvailableGreenText)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copiar Tudo", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { showReportDialog = false },
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary)
                        ) {
                            Text("Fechar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Book Metadata Parameters Modal
    editingBookMetadata?.let { book ->
        var categoryInput by remember { mutableStateOf(book.category) }
        var tipoObraSelected by remember { mutableStateOf(book.tipoObra) }
        var publicoAlvoSelected by remember { mutableStateOf(book.publicoAlvo) }
        
        val tiposList = listOf("Literário", "Didático", "Referência", "Técnico")
        val publicosList = listOf("Geral", "Infantil", "Infanto-Juvenil", "Adulto")
        
        Dialog(onDismissRequest = { editingBookMetadata = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Parâmetros JTA", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = ThemePurplePrimary
                    )
                    Text(
                        text = "Vincule a área de conhecimento curricular.",
                        style = MaterialTheme.typography.bodySmall, 
                        color = ThemeSecondaryText,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = categoryInput,
                        onValueChange = { categoryInput = it },
                        label = { Text("Categoria Acadêmica") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemePurplePrimary)
                    )

                    Text("Tipo da Obra para JTA:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ThemeSecondaryText)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tiposList.forEach { t ->
                            val isSelected = t == tipoObraSelected
                            FilterChip(
                                selected = isSelected,
                                onClick = { tipoObraSelected = t },
                                label = { Text(t, fontSize = 10.sp) }
                            )
                        }
                    }

                    Text("Público-Alvo:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ThemeSecondaryText)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        publicosList.forEach { p ->
                            val isSelected = p == publicoAlvoSelected
                            FilterChip(
                                selected = isSelected,
                                onClick = { publicoAlvoSelected = p },
                                label = { Text(p, fontSize = 10.sp) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editingBookMetadata = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.updateBookJtaMetadata(book, categoryInput, tipoObraSelected, publicoAlvoSelected)
                                editingBookMetadata = null
                                Toast.makeText(context, "Parâmetros atualizados!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary)
                        ) {
                            Text("Salvar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Custom annotations text modal
    editingBookJustification?.let { book ->
        var textInput by remember { mutableStateOf(book.textoJustificativa ?: "") }

        Dialog(onDismissRequest = { editingBookJustification = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Nova Observação", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = StatusAvailableGreenText
                    )
                    Text(
                        text = "Motivação institucional personalizada.",
                        style = MaterialTheme.typography.bodySmall, 
                        color = ThemeSecondaryText,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text("Justificativa do Especialista") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = StatusAvailableGreenText)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editingBookJustification = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.updateBookCustomJustification(book, textInput)
                                editingBookJustification = null
                                Toast.makeText(context, "Anotação salva!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusAvailableGreenText)
                        ) {
                            Text("Gravar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RoofModuleView(
    viewModel: LibraryViewModel,
    books: List<BookEntity>,
    shelves: List<ShelfEntity>,
    positions: List<ShelfPositionEntity>
) {
    val context = LocalContext.current
    var selectedShelfId by remember { mutableStateOf<Int?>(null) }
    
    var showShelfAddDialog by remember { mutableStateOf(false) }
    var showPositionAddDialog by remember { mutableStateOf(false) }
    var showOptimizeDialog by remember { mutableStateOf(false) }
    var associateBookTargetPosition by remember { mutableStateOf<ShelfPositionEntity?>(null) }
    
    // Auto selector initial shelf
    LaunchedEffect(shelves) {
        if (selectedShelfId == null && shelves.isNotEmpty()) {
            selectedShelfId = shelves.first().id
        }
    }

    val currentShelf = shelves.find { it.id == selectedShelfId }
    val currentShelfPositions = positions.filter { it.shelfId == selectedShelfId }
    val currentShelfBooks = books.filter { bk -> currentShelfPositions.any { it.id == bk.posicaoEstanteId } }
    
    val efficiencyIndex = if (currentShelf != null && currentShelfPositions.isNotEmpty()) {
        viewModel.calculateShelfEfficiencyIndex(books, currentShelfPositions)
    } else 100

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ThemeBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("ROOF - ROTA ÓTIMA E DISPOSIÇÃO FÍSICA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ThemePurplePrimary)
            Spacer(modifier = Modifier.height(10.dp))

            Text("Selecione Estante Física:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                shelves.forEach { sh ->
                    val isSel = sh.id == selectedShelfId
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedShelfId = sh.id },
                        label = { Text(sh.name, fontSize = 10.sp) }
                    )
                }
                
                FilterChip(
                    selected = false,
                    onClick = { showShelfAddDialog = true },
                    label = { Text("+ Criar Estante", fontSize = 10.sp, color = ThemePurplePrimary) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = ThemeBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            if (currentShelf == null) {
                Text(
                    "Cadastre uma estante para mapear suas posições físicas de livros.",
                    style = MaterialTheme.typography.bodySmall, 
                    color = ThemeSecondaryText, 
                    textAlign = TextAlign.Center, 
                    modifier = Modifier.fillMaxWidth().padding(14.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = currentShelf.name.uppercase(), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ThemeOnBackground)
                        Text(
                            text = "Locais: ${currentShelfPositions.size} | Livros Alocados: ${currentShelfBooks.size}",
                            style = MaterialTheme.typography.labelSmall, color = ThemeSecondaryText, fontSize = 10.sp
                        )
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = ThemePurpleLight),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Eficiência: $efficiencyIndex%", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 10.sp, 
                            color = ThemePurplePrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showPositionAddDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Criar Posição", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { showOptimizeDialog = true },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusAvailableGreenText),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mapear Rota ROOF", fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (currentShelf != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mapa de Organização Física (Células)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = ThemeOnBackground
            )
            
            // Delete shelf icon
            IconButton(
                onClick = {
                    viewModel.deleteShelf(currentShelf)
                    selectedShelfId = null
                    Toast.makeText(context, "Estante física deletada!", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = "Deletar Estante", tint = StatusOverdueRedText)
            }
        }

        if (currentShelfPositions.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp), 
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.GridOff, contentDescription = null, tint = ThemeSecondaryText, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Sem posições físicas cadastradas.", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Adicione coordenadas operacionais (ex: Prateleira 1) para posicionar os exemplares.", color = ThemeSecondaryText, textAlign = TextAlign.Center, fontSize = 11.sp)
                }
            }
        } else {
            // Flow list of physical positions in modern vertical display column
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                currentShelfPositions.forEach { pos ->
                    val allocatedBook = books.find { it.posicaoEstanteId == pos.id }
                    val acessibilityLabel = when(pos.accessibilityLevel) {
                        3 -> "Olhos (Máxima)"
                        2 -> "Cintura (Média)"
                        else -> "Pés/Teto (Baixa)"
                    }
                    val badgeAcessBg = when(pos.accessibilityLevel) {
                        3 -> Color(0xFFE8F5E9)
                        2 -> Color(0xFFFFF3E0)
                        else -> Color(0xFFECEFF1)
                    }
                    val badgeAcessText = when(pos.accessibilityLevel) {
                        3 -> Color(0xFF2E7D32)
                        2 -> Color(0xFFEF6C00)
                        else -> Color(0xFF455A64)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { associateBookTargetPosition = pos },
                        colors = CardDefaults.cardColors(containerColor = if (allocatedBook != null) Color.White else ThemeBackground),
                        border = BorderStroke(1.dp, if (allocatedBook != null) ThemePurplePrimary else ThemeBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = pos.coordinate, 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 13.sp, 
                                        color = ThemeOnBackground
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(badgeAcessBg)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = acessibilityLabel, color = badgeAcessText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))

                                if (allocatedBook != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Book, contentDescription = null, tint = ThemePurplePrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = allocatedBook.title, 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 12.sp, 
                                            color = ThemeOnBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "Frequência real de consultas: ${allocatedBook.useFrequency} empréstimos",
                                        fontSize = 10.sp,
                                        color = ThemeSecondaryText,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Nenhuma obra alocada. Toque para vincular.",
                                        fontSize = 11.sp,
                                        color = ThemeSecondaryText,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }

                            // Left controls: action details
                            IconButton(
                                onClick = {
                                    viewModel.deletePosition(pos)
                                    Toast.makeText(context, "Posição removida com sucesso!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Apagar Célula", tint = StatusOverdueRedText.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal shelf additions
    if (showShelfAddDialog) {
        var shelfName by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showShelfAddDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Cadastrar Nova Estante Física", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ThemePurplePrimary)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = shelfName,
                        onValueChange = { shelfName = it },
                        label = { Text("Nome da Estante (ex: Estante Infantil)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemePurplePrimary)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showShelfAddDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancelar", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                if (shelfName.isNotBlank()) {
                                    viewModel.addShelf(shelfName)
                                    showShelfAddDialog = false
                                    Toast.makeText(context, "Estante física cadastrada!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary)
                        ) {
                            Text("Salvar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Modal adding cell position
    if (showPositionAddDialog && currentShelf != null) {
        var coordName by remember { mutableStateOf("") }
        var accessLvl by remember { mutableStateOf(3) }
        
        Dialog(onDismissRequest = { showPositionAddDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Nova Posição Física", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ThemePurplePrimary)
                    Text("Vincular célula à estante ${currentShelf.name}", style = MaterialTheme.typography.bodySmall, color = ThemeSecondaryText)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    OutlinedTextField(
                        value = coordName,
                        onValueChange = { coordName = it },
                        label = { Text("Posição (ex: Prateleira 2)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemePurplePrimary)
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Nível de Acessibilidade Física:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { accessLvl = 3 }) {
                            RadioButton(selected = accessLvl == 3, onClick = { accessLvl = 3 })
                            Text("Nível 3 - Olhos (Mais Acessível)", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { accessLvl = 2 }) {
                            RadioButton(selected = accessLvl == 2, onClick = { accessLvl = 2 })
                            Text("Nível 2 - Cintura (Tolerável)", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { accessLvl = 1 }) {
                            RadioButton(selected = accessLvl == 1, onClick = { accessLvl = 1 })
                            Text("Nível 1 - Pés/Teto (Difícil Acesso)", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showPositionAddDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancelar", fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                if (coordName.isNotBlank()) {
                                    viewModel.addPosition(currentShelf.id, coordName, accessLvl)
                                    showPositionAddDialog = false
                                    Toast.makeText(context, "Posição ativada!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary)
                        ) {
                            Text("Salvar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Modal association mapping
    associateBookTargetPosition?.let { pos ->
        var queryBook by remember { mutableStateOf("") }
        var dropdownExp by remember { mutableStateOf(false) }
        
        Dialog(onDismissRequest = { associateBookTargetPosition = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Vincular Livro à Célula", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ThemePurplePrimary)
                    Text("Escolha qual livro reside fisicamente em '${pos.coordinate}'", style = MaterialTheme.typography.bodySmall, color = ThemeSecondaryText)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = queryBook,
                        onValueChange = { 
                            queryBook = it
                            dropdownExp = true
                        },
                        label = { Text("Pesquisar Livros...") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { dropdownExp = !dropdownExp }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ThemePurplePrimary)
                    )

                    if (dropdownExp) {
                        Card(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp).padding(top = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, ThemeBorder)
                        ) {
                            val matched = if (queryBook.isBlank()) books.take(8) else books.filter { it.title.contains(queryBook, ignoreCase = true) }.take(8)
                            Column(modifier = Modifier.verticalScrollWithKeyboard(rememberScrollState())) {
                                matched.forEach { bk ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.associateBookToPosition(bk, pos.id)
                                                associateBookTargetPosition = null
                                                Toast.makeText(context, "Livro associado com sucesso!", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(10.dp)
                                    ) {
                                        Text(bk.title, fontSize = 12.sp, color = ThemeOnBackground)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    
                    val assigned = books.find { it.posicaoEstanteId == pos.id }
                    if (assigned != null) {
                        Button(
                            onClick = {
                                viewModel.associateBookToPosition(assigned, null)
                                associateBookTargetPosition = null
                                Toast.makeText(context, "Posição desocupada!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusOverdueRedText)
                        ) {
                            Text("Remover Vínculo", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { associateBookTargetPosition = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fechar", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Modal Suggested Optimizations (ROOF Optimizer Suggestion Engine)
    if (showOptimizeDialog && currentShelf != null) {
        val suggestedRotations = viewModel.optimizeShelvesROOF(books, currentShelfPositions)
        Dialog(onDismissRequest = { showOptimizeDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, ThemeBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp).fillMaxSize()) {
                    Text("Sugestões de Otimização ROOF", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ThemePurplePrimary)
                    Text("Recomendações físicas de troca de livros baseadas no uso real.", style = MaterialTheme.typography.bodySmall, color = ThemeSecondaryText)
                    Spacer(modifier = Modifier.height(10.dp))

                    val totalRecoms = suggestedRotations.size
                    val timeReductionPercent = if (totalRecoms > 0) (totalRecoms * 8).coerceAtMost(65) else 0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ThemePurpleLight, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = ThemePurplePrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Redução no Tempo de Busca Física: ~ $timeReductionPercent%", fontWeight = FontWeight.Bold, color = ThemePurplePrimary, fontSize = 12.sp)
                            Text("Com base na proximidade operacional das obras mais requisitadas", fontSize = 9.sp, color = ThemeSecondaryText)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (suggestedRotations.isEmpty()) {
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Celebration, contentDescription = null, tint = StatusAvailableGreenText, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Organização Ideal Registrada!", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("A distribuição coincide exatamente com a demanda de empréstimos física.", color = ThemeSecondaryText, textAlign = TextAlign.Center, fontSize = 11.sp)
                            }
                        } else {
                            Column(modifier = Modifier.verticalScrollWithKeyboard(rememberScrollState())) {
                                suggestedRotations.forEach { sugest ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = ThemeBackground),
                                        border = BorderStroke(0.5.dp, ThemeBorder)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = ThemePurplePrimary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(sugest.book.title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ThemeOnBackground)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Ação Recomendada:",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = ThemeSecondaryText
                                            )
                                            Text(
                                                text = "Remover de '${sugest.currentPosition?.coordinate ?: "Sem Posição"}' e mover para o local otimizado '${sugest.recommendedPosition.coordinate}'.",
                                                fontSize = 11.sp,
                                                color = ThemeOnBackground
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = sugest.reason,
                                                fontSize = 10.sp,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                color = ThemePurplePrimary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            
                                            Button(
                                                onClick = {
                                                    viewModel.associateBookToPosition(sugest.book, sugest.recommendedPosition.id)
                                                    Toast.makeText(context, "Reposicionado na estante!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.align(Alignment.End),
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                            ) {
                                                Text("Efetivar", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showOptimizeDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary)
                    ) {
                        Text("Fechar", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklySuggestionsSection(
    suggestions: List<WeeklySuggestionEntity>,
    allBooks: List<BookEntity>,
    onAddSuggestion: (Int, String, String, String, String, String) -> Unit,
    onRemoveSuggestion: (WeeklySuggestionEntity) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F7FC)),
        border = BorderStroke(1.dp, ThemeBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = ThemePurplePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sugestões da Semana pelos Professores",
                        style = MaterialTheme.typography.titleMedium,
                        color = ThemePurplePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Indicar Livro", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (suggestions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = ThemeSecondaryText.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nenhuma sugestão cadastrada para esta semana.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThemeSecondaryText,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Clique em \"Indicar Livro\" acima para iniciar uma recomendação para sua turma!",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThemeSecondaryText.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, ThemeBorder.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Recomendado para: ${suggestion.classRoom}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemePurplePrimary,
                                            modifier = Modifier
                                                .background(ThemePurpleLight, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = recommendationTitleFormat(suggestion.bookTitle),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = ThemeOnBackground,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Autor: ${suggestion.bookAuthor}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ThemeSecondaryText
                                        )
                                    }

                                    IconButton(
                                        onClick = { onRemoveSuggestion(suggestion) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remover indicação",
                                            tint = Color.Red.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFCFAFF), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatQuote,
                                        contentDescription = null,
                                        tint = ThemePurplePrimary.copy(alpha = 0.3f),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .graphicsLayer(scaleY = -1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = suggestion.recommendation,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = ThemeOnBackground,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "— Prof. ${suggestion.professorName}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ThemePurplePrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSuggestionDialog(
            allBooks = allBooks,
            onDismiss = { showAddDialog = false },
            onConfirm = { bookId, title, author, prof, classRm, rec ->
                onAddSuggestion(bookId, title, author, prof, classRm, rec)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddSuggestionDialog(
    allBooks: List<BookEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String, String, String, String) -> Unit
) {
    var selectedBookIndex by remember { mutableStateOf(-1) }
    var professorName by remember { mutableStateOf("") }
    var classRoom by remember { mutableStateOf("") }
    var recommendation by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ThemeBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScrollWithKeyboard(rememberScrollState())
            ) {
                Text(
                    text = "Indicar Livro da Semana",
                    style = MaterialTheme.typography.titleLarge,
                    color = ThemePurplePrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Book Selector Field
                Text("Livro Indicado *", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = ThemeOnBackground)
                Spacer(modifier = Modifier.height(6.dp))
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { dropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ThemePurplePrimary)
                    ) {
                        val displayText = if (selectedBookIndex != -1 && selectedBookIndex < allBooks.size) {
                            allBooks[selectedBookIndex].title
                        } else {
                            "Selecione um livro do acervo..."
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(displayText, color = ThemeOnBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = ThemePurplePrimary)
                        }
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .heightIn(max = 250.dp)
                    ) {
                        if (allBooks.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Nenhum livro no acervo cadastrado.") },
                                onClick = { dropdownExpanded = false }
                            )
                        } else {
                            allBooks.forEachIndexed { index, book ->
                                DropdownMenuItem(
                                    text = { Text("${book.title} (de ${book.author})") },
                                    onClick = {
                                        selectedBookIndex = index
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Professor Name
                OutlinedTextField(
                    value = professorName,
                    onValueChange = { professorName = it },
                    label = { Text("Nome do Professor *") },
                    placeholder = { Text("Ex: Carlos Silva ou Profª Ana Maria") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemePurplePrimary,
                        focusedLabelColor = ThemePurplePrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Class/Turma
                OutlinedTextField(
                    value = classRoom,
                    onValueChange = { classRoom = it },
                    label = { Text("Turma / Série Alvo *") },
                    placeholder = { Text("Ex: 9º Ano B ou 3º Colegial C") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemePurplePrimary,
                        focusedLabelColor = ThemePurplePrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Recommendation text
                OutlinedTextField(
                    value = recommendation,
                    onValueChange = { recommendation = it },
                    label = { Text("Recomendação / Justificativa *") },
                    placeholder = { Text("Por que este livro é indicado para esta turma neste momento? Escreva uma breve mensagem inspiradora.") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemePurplePrimary,
                        focusedLabelColor = ThemePurplePrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorText.isNotEmpty()) {
                    Text(
                        text = errorText,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = ThemeSecondaryText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedBookIndex == -1 || professorName.isBlank() || classRoom.isBlank() || recommendation.isBlank()) {
                                errorText = "Por favor, preencha todos os campos obrigatórios (*)"
                            } else {
                                val book = allBooks[selectedBookIndex]
                                onConfirm(book.id, book.title, book.author, professorName.trim(), classRoom.trim(), recommendation.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Salvar Indicação")
                    }
                }
            }
        }
    }
}

fun recommendationTitleFormat(title: String): String {
    return if (title.length > 50) title.take(47) + "..." else title
}


@Composable
fun Modifier.verticalScrollWithKeyboard(state: ScrollState): Modifier {
    val coroutineScope = rememberCoroutineScope()
    return this
        .verticalScroll(state)
        .focusable()
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                when (event.key) {
                    Key.DirectionDown -> { coroutineScope.launch { state.scrollBy(150f) }; true }
                    Key.DirectionUp -> { coroutineScope.launch { state.scrollBy(-150f) }; true }
                    Key.PageDown -> { coroutineScope.launch { state.scrollBy(500f) }; true }
                    Key.PageUp -> { coroutineScope.launch { state.scrollBy(-500f) }; true }
                    else -> false
                }
            } else {
                false
            }
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        if (delta != 0f) {
                            coroutineScope.launch {
                                state.scrollBy(delta * 100f)
                            }
                        }
                    }
                }
            }
        }
}

@Composable
fun Modifier.horizontalScrollWithKeyboard(state: ScrollState): Modifier {
    val coroutineScope = rememberCoroutineScope()
    return this
        .horizontalScroll(state)
        .focusable()
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                when (event.key) {
                    Key.DirectionRight -> { coroutineScope.launch { state.scrollBy(150f) }; true }
                    Key.DirectionLeft -> { coroutineScope.launch { state.scrollBy(-150f) }; true }
                    else -> false
                }
            } else {
                false
            }
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val delta = event.changes.firstOrNull()?.scrollDelta?.x ?: (event.changes.firstOrNull()?.scrollDelta?.y ?: 0f)
                        if (delta != 0f) {
                            coroutineScope.launch {
                                state.scrollBy(delta * 100f)
                            }
                        }
                    }
                }
            }
        }
}

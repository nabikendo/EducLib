package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LibraryApp(viewModel: LibraryViewModel) {
    MyApplicationTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // Inside exclusive application content (no login screens)
            MainLibraryAppContent(viewModel)
        }
    }
}

@Composable
fun SetupPasswordScreen(onPasswordSaved: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeBackground)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ThemeBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(ThemeBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Logo",
                        tint = ThemePurplePrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Configurar Acesso",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ThemePurplePrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Defina a senha mestre para uso local exclusivo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeSecondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Nova Senha Mestre") },
                    placeholder = { Text("Mínimo 4 caracteres") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_password_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemePurplePrimary,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedLabelColor = ThemePurplePrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar Senha") },
                    placeholder = { Text("Repita a senha") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_password_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemePurplePrimary,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedLabelColor = ThemePurplePrimary
                    )
                )

                if (errorText.isNotEmpty()) {
                    Text(
                        text = errorText,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (password.length < 4) {
                            errorText = "A senha deve conter no mínimo 4 caracteres."
                        } else if (password != confirmPassword) {
                            errorText = "Controle falhou! Senhas não coincidem."
                        } else {
                            onPasswordSaved(password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_setup_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Salvar")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar e Entrar")
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onValidatePassword: suspend (String) -> Boolean) {
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeBackground)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, ThemeBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(ThemeBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalLibrary,
                        contentDescription = "Library Logo",
                        tint = ThemePurplePrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Acesso Seguro",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ThemePurplePrimary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Insira a senha mestre para acessar o sistema.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeSecondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha Mestre") },
                    placeholder = { Text("Sua senha de acesso") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ThemePurplePrimary,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedLabelColor = ThemePurplePrimary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        scope.launch {
                            onValidatePassword(password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_login_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Login, contentDescription = "Logar")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Entrar")
                }
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
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWide = maxWidth > 600.dp
            
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
    val context = LocalContext.current
    var showDialogForLoanBook by remember { mutableStateOf<BookEntity?>(null) }
    var showBookDetailsDialog by remember { mutableStateOf<BookEntity?>(null) }
    var showInsightsDialog by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // Tab Header details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
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

            // Quick Floating Action Button on Desktop
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

        Spacer(modifier = Modifier.height(16.dp))

        // Bento style stats overview
        BentoStats(books = books, loans = loans, onInsightsClick = { showInsightsDialog = true })

        Spacer(modifier = Modifier.height(4.dp))

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
                .fillMaxWidth()
                .testTag("search_books_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ThemePurplePrimary,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedLabelColor = ThemePurplePrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Premium Compact Sort Row
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

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal scrollable genre filter chips
        if (genres.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
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
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (filteredBooks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
        } else {
            // Adaptive Grid based on device width
            val gridCells = if (LocalContext.current.resources.configuration.smallestScreenWidthDp >= 600) {
                GridCells.Adaptive(minSize = 300.dp)
            } else {
                GridCells.Fixed(1)
            }

            LazyVerticalGrid(
                columns = gridCells,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredBooks) { book ->
                    BookCard(
                        book = book,
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
}

@Composable
fun BookCard(
    book: BookEntity,
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
                if (!book.photoPath.isNullOrEmpty() && File(book.photoPath).exists()) {
                    AsyncImage(
                        model = File(book.photoPath),
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
                            text = book.location,
                            fontSize = 11.sp,
                            color = ThemeSecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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

    val filteredBorrowers = remember(borrowerName, allBorrowersList) {
        if (borrowerName.isBlank()) {
            allBorrowersList.take(4)
        } else {
            allBorrowersList.filter {
                it.name.lowercase().contains(borrowerName.lowercase())
            }.take(5)
        }
    }

    val matchedBorrower = remember(borrowerName, allBorrowersList) {
        allBorrowersList.find { it.name.trim().lowercase() == borrowerName.trim().lowercase() }
    }

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
                    .verticalScroll(rememberScrollState())
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

                OutlinedTextField(
                    value = borrowerName,
                    onValueChange = { 
                        borrowerName = it 
                        errorText = ""
                    },
                    label = { Text("Nome do Aluno ou Professor *") },
                    placeholder = { Text("Digite para buscar...") },
                    singleLine = true,
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
                                    Icon(
                                        imageVector = if (borrower.type == "Professor") Icons.Default.School else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = ThemePurplePrimary,
                                        modifier = Modifier.size(18.dp)
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
                            if (borrowerName.isBlank()) {
                                errorText = "Por favor, selecione um leitor cadastrado."
                            } else if (matchedBorrower == null) {
                                errorText = "Empréstimo bloqueado: O leitor precisa estar cadastrado."
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
    val context = LocalContext.current
    
    // "Todos", "Pendentes", "Atrasados"
    var loanFilter by rememberSaveable { mutableStateOf("Pendentes") }

    val filteredLoans = remember(loans, loanFilter) {
        when (loanFilter) {
            "Atrasados" -> loans.filter { it.calculatedIsOverdue() }
            "Pendentes" -> loans.filter { it.returnDate == null }
            else -> loans
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Movimentações & Empréstimos",
            style = MaterialTheme.typography.headlineLarge,
            color = ThemePurplePrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Definição automática de prazo de 14 dias para devolução. Atrasados marcados em tempo real.",
            style = MaterialTheme.typography.bodyMedium,
            color = ThemeSecondaryText,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        // Statistics
        BentoStats(books = books, loans = loans)

        Spacer(modifier = Modifier.height(8.dp))

        // Dynamic Filter Chips instead of a single checkbox
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filterOptions = listOf("Pendentes", "Atrasados", "Todos")
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
                                else -> "Histórico Geral"
                            }
                        ) 
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when (option) {
                            "Atrasados" -> BentoPurpleActive
                            "Pendentes" -> BentoLilacSecondary
                            else -> ThemePurplePrimary
                        },
                        selectedLabelColor = when (option) {
                            "Atrasados" -> BentoPurpleActiveOn
                            "Pendentes" -> BentoLilacSecondaryOn
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

        if (filteredLoans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                            "Pendentes" -> "Todos os livros devolvidos com sucesso!"
                            else -> "Realize empréstimos a partir do Acervo Geral."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeSecondaryText,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredLoans, key = { it.id }) { loan ->
                    LoanCard(
                        loan = loan,
                        onRenew = {
                            viewModel.renewLoan(context, loan.id) { success ->
                                if (success) {
                                    Toast.makeText(context, "Empréstimo renovado com sucesso! (+1 semana)", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onReturn = {
                            viewModel.returnBook(context, loan.id) { success ->
                                if (success) {
                                    Toast.makeText(context, "Livro devolvido com sucesso!", Toast.LENGTH_SHORT).show()
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
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoanCard(
    loan: LoanEntity,
    onRenew: () -> Unit,
    onReturn: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val isOverdue = loan.calculatedIsOverdue()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isOverdue) StatusOverdueRedText else ThemeBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Configurações e Backup",
                style = MaterialTheme.typography.headlineLarge,
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
                            colors = ButtonDefaults.buttonColors(containerColor = ThemePurplePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("export_backup_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exportar Backup")
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
                            Text("Restaurar Backup", color = ThemePurplePrimary)
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
            .verticalScroll(rememberScrollState())
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
                if (!path.isNullOrEmpty() && File(path).exists()) {
                    AsyncImage(
                        model = File(path),
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

        Spacer(modifier = Modifier.height(32.dp))
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
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

        Spacer(modifier = Modifier.height(16.dp))

        // Large search bar
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

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips row
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

        Spacer(modifier = Modifier.height(16.dp))

        // Filtering list
        val filteredList = remember(borrowers, filterType) {
            borrowers.filter {
                filterType == "Todos" || it.type.lowercase() == filterType.lowercase()
            }
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
        } else {
            // Adaptive grid
            val gridCells = if (LocalContext.current.resources.configuration.smallestScreenWidthDp >= 600) {
                GridCells.Adaptive(minSize = 260.dp)
            } else {
                GridCells.Fixed(1)
            }

            val groupedByTurma = remember(filteredList) {
                filteredList.groupBy { it.classRoom.trim().ifBlank { "Sem Turma" } }
                    .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            }

            LazyVerticalGrid(
                columns = gridCells,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (borrower.type == "Professor") BentoLilacSecondary else ThemeBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (borrower.type == "Professor") Icons.Default.School else Icons.Default.Person,
                    contentDescription = null,
                    tint = ThemePurplePrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

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
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (borrowerToEdit == null) "Cadastrar Leitor" else "Editar Cadastro",
                    style = MaterialTheme.typography.titleLarge,
                    color = ThemePurplePrimary,
                    fontWeight = FontWeight.Bold
                )

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
                                    viewModel.addBorrower(context, name.trim(), type, finalIdentifier, phone.trim(), finalClassRoom)
                                    Toast.makeText(context, "$type cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.updateBorrower(context, borrowerToEdit, name.trim(), type, finalIdentifier, phone.trim(), finalClassRoom)
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
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(ThemeBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (borrower.type == "Professor") Icons.Default.School else Icons.Default.Person,
                                contentDescription = null,
                                tint = ThemePurplePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
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
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = ThemeSecondaryText)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = borrower.phone, style = MaterialTheme.typography.bodySmall, color = ThemeSecondaryText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                                                    viewModel.returnBook(context, loan.id) { success ->
                                                        if (success) {
                                                            Toast.makeText(context, "Livro devolvido!", Toast.LENGTH_SHORT).show()
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
    
    val bookLoans = remember(loans, book.id) {
        loans.filter { it.bookId == book.id }
    }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

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
                        if (!book.photoPath.isNullOrEmpty() && File(book.photoPath).exists()) {
                            AsyncImage(
                                model = File(book.photoPath),
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

                Text(
                    text = "Histórico de Circulação",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemePurplePrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (bookLoans.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Este exemplar nunca foi emprestado.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeSecondaryText
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                                    viewModel.returnBook(context, loan.id) { success ->
                                                        if (success) {
                                                            Toast.makeText(context, "Livro recebido!", Toast.LENGTH_SHORT).show()
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
                    }
                }
            }
        }
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

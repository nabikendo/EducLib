package com.example.ui

  import androidx.compose.animation.*
  import androidx.compose.foundation.*
  import androidx.compose.foundation.layout.*
  import androidx.compose.foundation.shape.CircleShape
  import androidx.compose.foundation.shape.RoundedCornerShape
  import androidx.compose.foundation.text.KeyboardOptions
  import androidx.compose.material.icons.Icons
  import androidx.compose.material.icons.filled.*
  import androidx.compose.material3.*
  import androidx.compose.runtime.*
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.draw.clip
  import androidx.compose.ui.draw.shadow
  import androidx.compose.ui.graphics.Brush
  import androidx.compose.ui.graphics.Color
  import androidx.compose.ui.platform.testTag
  import androidx.compose.ui.text.font.FontWeight
  import androidx.compose.ui.text.input.PasswordVisualTransformation
  import androidx.compose.ui.text.input.VisualTransformation
  import androidx.compose.ui.text.style.TextAlign
  import androidx.compose.ui.unit.dp
  import androidx.compose.ui.unit.sp
  import androidx.compose.ui.window.Dialog
  import com.example.ui.theme.*
  import kotlinx.coroutines.launch

  @OptIn(ExperimentalAnimationApi::class)
  @Composable
  fun UnifiedAuthScreen(viewModel: LibraryViewModel, isSetupMode: Boolean) {
      var isSignUp by remember { mutableStateOf(isSetupMode) }
      var name by remember { mutableStateOf("") }
      var email by remember { mutableStateOf("") }
      var password by remember { mutableStateOf("") }
      var confirmPassword by remember { mutableStateOf("") }
      var masterPassword by remember { mutableStateOf("") }

      var passwordVisible by remember { mutableStateOf(false) }
      var confirmPasswordVisible by remember { mutableStateOf(false) }
      var masterPasswordVisible by remember { mutableStateOf(false) }

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
                      kotlinx.coroutines.delay(1000)
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
                          Color(0xFFFFFFFF),
                          Color(0xFFFBFBFC),
                          Color(0xFFF4F4F7)
                      )
                  )
              )
              .padding(24.dp),
          contentAlignment = Alignment.Center
      ) {
          if (isLoadingGoogle) {
              LoadingOverlay(text = "Conectando...")
          } else {
              Column(
                  modifier = Modifier
                      .widthIn(max = 380.dp)
                      .fillMaxWidth()
                      .verticalScroll(rememberScrollState()),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center
              ) {
                  AuthLogoHeader()

                  Spacer(modifier = Modifier.height(32.dp))

                  Card(
                      colors = CardDefaults.cardColors(containerColor = Color.White),
                      border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                      shape = RoundedCornerShape(24.dp),
                      modifier = Modifier.fillMaxWidth()
                  ) {
                      Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                          if (!isSetupMode) {
                              AuthTabSelector(isSignUp) { isSignUp = it }
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

                          if (isSignUp) {
                              SignUpForm(
                                  name, { name = it },
                                  email, { email = it },
                                  password, { password = it },
                                  confirmPassword, { confirmPassword = it },
                                  passwordVisible, { passwordVisible = it },
                                  confirmPasswordVisible, { confirmPasswordVisible = it }
                              )
                          } else {
                              LoginForm(
                                  loginByMasterPassword, { loginByMasterPassword = it },
                                  masterPassword, { masterPassword = it },
                                  masterPasswordVisible, { masterPasswordVisible = it },
                                  email, { email = it },
                                  password, { password = it },
                                  passwordVisible, { passwordVisible = it }
                              )
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

                          Button(
                              onClick = {
                                  if (isSignUp) {
                                      if (name.isBlank() || email.isBlank() ||
  password.isBlank()) {
                                          errorText = "Favor preencher todos os campos
  obrigatórios."
                                      } else if (!email.contains("@")) {
                                          errorText = "Insira um endereço de e-mail válido."
                                      } else if (password.length < 4) {
                                          errorText = "A senha deve conter no mínimo 4
  caracteres."
                                      } else if (password != confirmPassword) {
                                          errorText = "Senhas não coincidem."
                                      } else {
                                          errorText = ""
                                          viewModel.registerEmailAndPassword(email, password,
  name)
                                      }
                                  } else {
                                      if (loginByMasterPassword) {
                                          if (masterPassword.isBlank()) {
                                              errorText = "A senha não pode estar em branco."
                                          } else {
                                              scope.launch {
                                                  val success =
  viewModel.login(masterPassword)
                                                  errorText = if (success) "" else "Senha
  mestre incorreta!"
                                              }
                                          }
                                      } else {
                                          if (email.isBlank() || password.isBlank()) {
                                              errorText = "Preencha o e-mail e a senha
  cadastrados."
                                          } else {
                                              scope.launch {
                                                  val success =
  viewModel.loginWithEmail(email, password)
                                                  errorText = if (success) "" else "E-mail ou
  senha incorretos!"
                                              }
                                          }
                                      }
                                  }
                              },
                              modifier =
  Modifier.fillMaxWidth().height(46.dp).testTag("submit_setup_button"),
                              colors = ButtonDefaults.buttonColors(containerColor =
  ThemePurplePrimary),
                              shape = RoundedCornerShape(12.dp)
                          ) {
                              Text(
                                  text = if (isSignUp) "Criar Minha Conta" else "Entrar",
                                  fontWeight = FontWeight.ExtraBold,
                                  fontSize = 14.sp
                              )
                          }

                          Spacer(modifier = Modifier.height(16.dp))
                          AuthDivider("ou")
                          Spacer(modifier = Modifier.height(16.dp))

                          OutlinedButton(
                              onClick = { showGoogleChooser = true },
                              modifier =
  Modifier.fillMaxWidth().height(46.dp).testTag("google_login_button"),
                              border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                              colors = ButtonDefaults.outlinedButtonColors(containerColor =
  Color.White),
                              shape = RoundedCornerShape(12.dp)
                          ) {
                              Row(verticalAlignment = Alignment.CenterVertically,
  horizontalArrangement = Arrangement.Center) {
                                  GoogleIcon()
                                  Spacer(modifier = Modifier.width(8.dp))
                                  Text(
                                      text = if (isSignUp) "Cadastrar com o Google" else
  "Entrar com o Google",
                                      color = ThemeSecondaryText.copy(alpha = 0.8f),
                                      fontWeight = FontWeight.Bold,
                                      fontSize = 13.sp
                                  )
                              }
                          }

                          Spacer(modifier = Modifier.height(12.dp))

                          Button(
                              onClick = { viewModel.guestLogin() },
                              modifier =
  Modifier.fillMaxWidth().height(46.dp).testTag("guest_login_button"),
                              colors = ButtonDefaults.buttonColors(containerColor =
  Color(0xFF10B981)),
                              shape = RoundedCornerShape(12.dp)
                          ) {
                              Row(verticalAlignment = Alignment.CenterVertically,
  horizontalArrangement = Arrangement.Center) {
                                  Icon(Icons.Default.Bolt, contentDescription = null, tint =
  Color.White, modifier = Modifier.size(18.dp))
                                  Spacer(modifier = Modifier.width(8.dp))
                                  Text("Entrar Direto (Acesso de Teste)", color =
  Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
  fun AuthLogoHeader() {
      Box(
          modifier = Modifier
              .size(60.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(Color.White)
              .shadow(2.dp, RoundedCornerShape(16.dp))
              .border(BorderStroke(0.5.dp, Color(0xFFE2E8F0)), RoundedCornerShape(16.dp)),
          contentAlignment = Alignment.Center
      ) {
          Icon(Icons.Default.LocalLibrary, contentDescription = "BiblioTech", tint =
  ThemePurplePrimary, modifier = Modifier.size(30.dp))
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
  }

  @Composable
  fun AuthTabSelector(isSignUp: Boolean, onTabSelected: (Boolean) -> Unit) {
      Row(
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color
  (0xFFF1F5F9)).padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
          AuthTabItem("Acessar", !isSignUp) { onTabSelected(false) }
          AuthTabItem("Criar Conta", isSignUp) { onTabSelected(true) }
      }
  }

  @Composable
  fun AuthTabItem(text: String, isActive: Boolean, onClick: () -> Unit) {
      Box(
          modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(if (isActive) Color.White else Color.Transparent)
              .then(if (isActive) Modifier.shadow(1.dp, RoundedCornerShape(10.dp)) else
  Modifier)
              .clickable { onClick() }
              .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center
      ) {
          Text(
              text = text,
              fontWeight = FontWeight.Bold,
              color = if (isActive) ThemePurplePrimary else ThemeSecondaryText.copy(alpha =
  0.6f),
              fontSize = 13.sp
          )
      }
  }

  @Composable
  fun SignUpForm(
      name: String, onNameChange: (String) -> Unit,
      email: String, onEmailChange: (String) -> Unit,
      password: String, onPasswordChange: (String) -> Unit,
      confirmPassword: String, onConfirmPasswordChange: (String) -> Unit,
      passwordVisible: Boolean, onPasswordVisibleChange: (Boolean) -> Unit,
      confirmPasswordVisible: Boolean, onConfirmPasswordVisibleChange: (Boolean) -> Unit
  ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          AuthTextField(value = name, onValueChange = onNameChange, label = "Nome Completo",
  icon = Icons.Default.Person, placeholder = "Nome de usuário")
          AuthTextField(value = email, onValueChange = onEmailChange, label = "E-mail", icon
  = Icons.Default.Email, placeholder = "escola@sistema.com")
          AuthTextField(
              value = password, onValueChange = onPasswordChange, label = "Senha", icon =
  Icons.Default.Lock,
              placeholder = "Mínimo 4 caracteres",
              isPassword = true,
              visible = passwordVisible,
              onVisibleChange = onPasswordVisibleChange
          )
          AuthTextField(
              value = confirmPassword, onValueChange = onConfirmPasswordChange, label =
  "Confirmar Senha", icon = Icons.Default.CheckCircle,
              placeholder = "Repita a sua senha",
              isPassword = true,
              visible = confirmPasswordVisible,
              onVisibleChange = onConfirmPasswordVisibleChange
          )
      }
  }

  @Composable
  fun LoginForm(
      loginByMasterPassword: Boolean, onLoginByMasterPasswordChange: (Boolean) -> Unit,
      masterPassword: String, onMasterPasswordChange: (String) -> Unit,
      masterPasswordVisible: Boolean, onMasterPasswordVisibleChange: (Boolean) -> Unit,
      email: String, onEmailChange: (String) -> Unit,
      password: String, onPasswordChange: (String) -> Unit,
      passwordVisible: Boolean, onPasswordVisibleChange: (Boolean) -> Unit
  ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          if (loginByMasterPassword) {
              AuthTextField(
                  value = masterPassword, onValueChange = onMasterPasswordChange, label =
  "Senha Mestre", icon = Icons.Default.Key,
                  placeholder = "Sua senha de acesso local",
                  isPassword = true,
                  visible = masterPasswordVisible,
                  onVisibleChange = onMasterPasswordVisibleChange
              )
              TextButton(onClick = { onLoginByMasterPasswordChange(false) }, modifier =
  Modifier.align(Alignment.End)) {
                  Text("Entrar com E-mail Escolar", fontSize = 11.sp, color =
  ThemePurplePrimary, fontWeight = FontWeight.SemiBold)
              }
          } else {
              AuthTextField(value = email, onValueChange = onEmailChange, label = "E-mail",
  icon = Icons.Default.Email, placeholder = "escola@sistema.com")
              AuthTextField(
                  value = password, onValueChange = onPasswordChange, label = "Senha", icon =
  Icons.Default.Lock,
                  placeholder = "Digite sua senha",
                  isPassword = true,
                  visible = passwordVisible,
                  onVisibleChange = onPasswordVisibleChange
              )
              TextButton(onClick = { onLoginByMasterPasswordChange(true) }, modifier =
  Modifier.align(Alignment.End)) {
                  Text("Acessar via Senha Mestre", fontSize = 11.sp, color =
  ThemePurplePrimary, fontWeight = FontWeight.SemiBold)
              }
          }
      }
  }

  @Composable
  fun AuthTextField(
      value: String, onValueChange: (String) -> Unit,
      label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
      placeholder: String,
      isPassword: Boolean = false,
      visible: Boolean = false,
      onVisibleChange: (Boolean) -> Unit = {}
  ) {
      OutlinedTextField(
          value = value,
          onValueChange = onValueChange,
          label = { Text(label) },
          placeholder = { Text(placeholder) },
          leadingIcon = { Icon(icon, contentDescription = null, tint = Color(0xFF64748B)) },
          trailingIcon = if (isPassword) {
              {
                  IconButton(onClick = { onVisibleChange(!visible) }) {
                      Icon(
                          imageVector = if (visible) Icons.Default.Visibility else
  Icons.Default.VisibilityOff,
                          contentDescription = null,
                          tint = Color(0xFF64748B)
                      )
                  }
              }
          } else null,
          visualTransformation = if (isPassword && !visible) PasswordVisualTransformation()
  else VisualTransformation.None,
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
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
  }

  @Composable
  fun AuthDivider(text: String) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically)
  {
          HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
          Text(
              text = text,
              modifier = Modifier.padding(horizontal = 12.dp),
              style = MaterialTheme.typography.bodySmall,
              color = ThemeSecondaryText.copy(alpha = 0.5f)
          )
          HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
      }
  }

  @Composable
  fun GoogleIcon() {
      Box(
          modifier = Modifier.size(18.dp).clip(CircleShape).background(ThemePurpleLight),
          contentAlignment = Alignment.Center
      ) {
          Text("G", fontWeight = FontWeight.ExtraBold, color = ThemePurplePrimary, fontSize =
  10.sp)
      }
  }

  @Composable
  fun LoadingOverlay(text: String) {
      Card(
          colors = CardDefaults.cardColors(containerColor = Color.White),
          border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth()
      ) {
          Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
          ) {
              CircularProgressIndicator(color = ThemePurplePrimary, strokeWidth = 3.dp)
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                  text = text,
                  fontWeight = FontWeight.Bold,
                  color = ThemePurplePrimary,
                  style = MaterialTheme.typography.bodyLarge
              )
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
              modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth()
          ) {
              Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                  Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                  ) {
                      Box(
                          modifier =
  Modifier.size(24.dp).clip(CircleShape).background(ThemePurpleLight),
                          contentAlignment = Alignment.Center
                      ) {
                          Text("G", fontWeight = FontWeight.Bold, color = ThemePurplePrimary,
  fontSize = 12.sp)
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
                                  modifier = Modifier.size(36.dp).clip(CircleShape).background(ThemePurplePrimary),
                                  contentAlignment = Alignment.Center
                              ) {
                                  Text(text = alphabet, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                              }
                              Spacer(modifier = Modifier.width(12.dp))
                              Column {
                                  Text(text = name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
  color = ThemeOnBackground)
                                  Text(text = email, style = MaterialTheme.typography.bodySmall, color = ThemeSecondaryText)
                              }
                          }
                      }
                  }
              }
          }
      }
  }

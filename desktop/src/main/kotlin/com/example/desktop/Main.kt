package com.example.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ImportContacts
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.data.BackupFormat
import com.example.data.BookEntity
import com.example.data.BorrowerEntity
import com.example.data.LoanEntity
import com.example.data.ShelfEntity
import com.example.data.isBlocked
import com.example.map.LibraryMap3DDialog
import com.example.map.describeCell
import com.example.map.formatCellCoordinate
import com.example.map.locateBook
import com.example.ui.isFuzzyMatch
import com.example.ui.theme.LibraryForest
import com.example.ui.theme.LibraryForestDeep
import com.example.ui.theme.LibraryInk
import com.example.ui.theme.LibraryMuted
import com.example.ui.theme.LibraryOutline
import com.example.ui.theme.LibraryPaper
import com.example.ui.theme.LibrarySage
import com.example.ui.theme.LibraryTerracotta
import com.example.ui.theme.StatusOverdueRedText
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Pasta de dados: %APPDATA%\EducLib no Windows, $XDG_DATA_HOME/educlib (ou ~/.local/share/educlib) no Linux. */
fun defaultDataDir(): Path {
    val os = System.getProperty("os.name").lowercase()
    val home = System.getProperty("user.home")
    return when {
        os.contains("win") -> Path.of(System.getenv("APPDATA") ?: "$home\\AppData\\Roaming", "EducLib")
        os.contains("mac") -> Path.of(home, "Library", "Application Support", "EducLib")
        else -> Path.of(System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() } ?: "$home/.local/share", "educlib")
    }
}

private val EducLibColors = lightColorScheme(
    primary = LibraryForest,
    onPrimary = Color.White,
    primaryContainer = LibrarySage,
    onPrimaryContainer = LibraryForestDeep,
    secondary = LibraryTerracotta,
    secondaryContainer = LibrarySage,
    onSecondaryContainer = LibraryForestDeep,
    background = LibraryPaper,
    surface = Color.White,
    onSurface = LibraryInk,
    onSurfaceVariant = LibraryMuted,
    outline = LibraryOutline,
    error = StatusOverdueRedText
)

private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
private fun Long.asDate() = dateFormat.format(Date(this))

fun main() {
    val dataDir = defaultDataDir()
    val startup = runCatching { LibraryStore(dataDir.resolve("biblioteca.json")) }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "EducLib — Biblioteca Escolar",
            state = rememberWindowState(width = 1280.dp, height = 820.dp)
        ) {
            MaterialTheme(colorScheme = EducLibColors) {
                startup.fold(
                    onSuccess = { App(it, dataDir) },
                    onFailure = { e ->
                        Box(Modifier.fillMaxSize().background(LibraryPaper).padding(32.dp)) {
                            Text("Não foi possível abrir os dados da biblioteca.\n\n${e.message}\n\nPasta: $dataDir", color = LibraryInk)
                        }
                    }
                )
            }
        }
    }
}

private enum class Section(val label: String) { BOOKS("Acervo"), READERS("Leitores"), LOANS("Empréstimos"), SHELVES("Estantes"), SETTINGS("Backup") }

@Composable
fun App(store: LibraryStore, dataDir: Path) {
    val data by store.data.collectAsState()
    var section by remember { mutableStateOf(Section.BOOKS) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var mapBookId by remember { mutableStateOf<Int?>(null) }
    var showMap by remember { mutableStateOf(false) }

    /** Executa uma ação da loja e mostra o erro (regra de negócio ou disco) sem derrubar o app. */
    fun act(success: String? = null, block: () -> Unit) {
        val message = try {
            block(); success
        } catch (e: StoreException) {
            e.message
        } catch (e: Exception) {
            "Erro: ${e.message ?: e.javaClass.simpleName}"
        }
        if (message != null) scope.launch { snackbar.showSnackbar(message) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = LibraryPaper) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            NavigationRail(containerColor = Color.White) {
                Spacer(Modifier.height(12.dp))
                Text("EducLib", fontWeight = FontWeight.Bold, color = LibraryForestDeep, modifier = Modifier.padding(8.dp))
                Section.entries.forEach { s ->
                    NavigationRailItem(
                        selected = section == s,
                        onClick = { section = s },
                        label = { Text(s.label) },
                        icon = {
                            Icon(
                                when (s) {
                                    Section.BOOKS -> Icons.AutoMirrored.Outlined.LibraryBooks
                                    Section.READERS -> Icons.Outlined.People
                                    Section.LOANS -> Icons.Outlined.ImportContacts
                                    Section.SHELVES -> Icons.Outlined.ViewModule
                                    Section.SETTINGS -> Icons.Outlined.Settings
                                },
                                contentDescription = null
                            )
                        }
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxSize().padding(24.dp)) {
                when (section) {
                    Section.BOOKS -> BooksScreen(data, store, ::act, onShowOnMap = { mapBookId = it; showMap = true })
                    Section.READERS -> ReadersScreen(data, store, ::act)
                    Section.LOANS -> LoansScreen(data, store, ::act)
                    Section.SHELVES -> ShelvesScreen(data, store, ::act, onOpenMap = { mapBookId = null; showMap = true })
                    Section.SETTINGS -> SettingsScreen(store, dataDir, ::act)
                }
            }
        }
    }

    if (showMap) {
        LibraryMap3DDialog(
            shelves = data.shelves,
            positions = data.positions,
            books = data.books,
            initialBookId = mapBookId,
            onSaveShelf = { shelf -> act("Medidas salvas.") { store.saveShelf(shelf) } },
            onScanRequest = null,
            onDismiss = { showMap = false }
        )
    }
}

private typealias Act = (String?, () -> Unit) -> Unit

@Composable
private fun ScreenHeader(title: String, subtitle: String, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = LibraryInk)
            Text(subtitle, color = LibraryMuted, fontSize = 13.sp)
        }
        action?.invoke()
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String) = OutlinedTextField(
    value = value,
    onValueChange = onChange,
    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
    placeholder = { Text(placeholder) },
    singleLine = true,
    modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()
)

@Composable
private fun RowCard(content: @Composable () -> Unit) = Card(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    border = BorderStroke(1.dp, LibraryOutline.copy(alpha = 0.35f))
) { Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) { content() } }

// ==================================================================== Acervo

@Composable
private fun BooksScreen(data: LibraryData, store: LibraryStore, act: Act, onShowOnMap: (Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<BookEntity?>(null) }
    var lending by remember { mutableStateOf<BookEntity?>(null) }
    var deleting by remember { mutableStateOf<BookEntity?>(null) }
    val positionsById = remember(data.positions) { data.positions.associateBy { it.id } }
    val shelvesById = remember(data.shelves) { data.shelves.associateBy { it.id } }
    val filtered = remember(query, data.books) {
        data.books.filter { query.isBlank() || isFuzzyMatch(query, it.title) || isFuzzyMatch(query, it.author) || it.isbn.contains(query.trim()) }
            .sortedBy { it.title.lowercase() }
    }

    Column {
        ScreenHeader("Acervo", "${data.books.size} títulos · ${data.books.sumOf { it.availableQuantity }} exemplares disponíveis") {
            Button(onClick = { editing = BookEntity(title = "", author = "", genre = "", year = 0, location = "", photoPath = null, totalQuantity = 1, availableQuantity = 1) }) {
                Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text("Novo livro")
            }
        }
        SearchField(query, { query = it }, "Buscar por título, autor ou ISBN")
        Spacer(Modifier.height(12.dp))
        if (filtered.isEmpty()) Text(if (data.books.isEmpty()) "Nenhum livro cadastrado. Cadastre o primeiro ou importe um backup do celular em Backup." else "Nada encontrado.", color = LibraryMuted)
        LazyColumn {
            items(filtered, key = { it.id }) { book ->
                val loc = locateBook(book, positionsById)
                RowCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(book.title, fontWeight = FontWeight.SemiBold, color = LibraryInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOfNotNull(book.author, book.genre.takeIf { it.isNotBlank() }, book.year.takeIf { it > 0 }?.toString(), book.isbn.takeIf { it.isNotBlank() }?.let { "ISBN $it" }).joinToString(" · "),
                                fontSize = 12.sp, color = LibraryMuted, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                loc?.let { "${shelvesById[it.shelfId]?.name ?: "Estante"} · ${describeCell(it.cell)}" } ?: book.location.ifBlank { "Sem posição na estante" },
                                fontSize = 12.sp, color = LibraryForestDeep
                            )
                        }
                        Text(
                            "${book.availableQuantity}/${book.totalQuantity} disponíveis",
                            fontSize = 12.sp,
                            color = if (book.availableQuantity == 0) StatusOverdueRedText else LibraryMuted,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        TextButton(onClick = { lending = book }, enabled = book.availableQuantity > 0) { Text("Emprestar") }
                        IconButton(onClick = { onShowOnMap(book.id) }) { Icon(Icons.Outlined.ViewInAr, contentDescription = "Ver no mapa 3D") }
                        IconButton(onClick = { editing = book }) { Icon(Icons.Outlined.Edit, contentDescription = "Editar") }
                        IconButton(onClick = { deleting = book }) { Icon(Icons.Outlined.Delete, contentDescription = "Excluir") }
                    }
                }
            }
        }
    }

    editing?.let { book -> BookEditor(book, data, store, act) { editing = null } }
    lending?.let { book -> LendDialog(book, data, store, act) { lending = null } }
    deleting?.let { book ->
        ConfirmDialog(
            title = "Excluir “${book.title}”?",
            text = "O histórico de empréstimos devolvidos é mantido e as reservas deste livro são removidas. Livros com exemplares emprestados não podem ser excluídos.",
            confirm = "Excluir",
            onConfirm = { act("Livro excluído.") { store.deleteBook(book.id) }; deleting = null },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun BookEditor(book: BookEntity, data: LibraryData, store: LibraryStore, act: Act, onClose: () -> Unit) {
    var title by remember { mutableStateOf(book.title) }
    var author by remember { mutableStateOf(book.author) }
    var isbn by remember { mutableStateOf(book.isbn) }
    var genre by remember { mutableStateOf(book.genre) }
    var year by remember { mutableStateOf(if (book.year > 0) book.year.toString() else "") }
    var location by remember { mutableStateOf(book.location) }
    var total by remember { mutableStateOf(book.totalQuantity.toString()) }
    var positionId by remember { mutableStateOf(book.posicaoEstanteId) }
    var positionMenu by remember { mutableStateOf(false) }
    val shelvesById = data.shelves.associateBy { it.id }
    fun positionLabel(id: Int?) = data.positions.firstOrNull { it.id == id }?.let { "${shelvesById[it.shelfId]?.name ?: "Estante"} — ${it.coordinate}" } ?: "Sem posição"

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (book.id == 0) "Novo livro" else "Editar livro") },
        text = {
            Column(Modifier.widthIn(min = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(author, { author = it }, label = { Text("Autoria") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(isbn, { isbn = it.filter { c -> c.isDigit() || c == 'X' || c == 'x' || c == '-' } }, label = { Text("ISBN") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, label = { Text("Ano") }, singleLine = true, modifier = Modifier.weight(0.5f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(genre, { genre = it }, label = { Text("Gênero") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(total, { total = it.filter(Char::isDigit).take(4) }, label = { Text("Exemplares") }, singleLine = true, modifier = Modifier.weight(0.5f))
                }
                OutlinedTextField(location, { location = it }, label = { Text("Localização (texto livre)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Box {
                    OutlinedButton(onClick = { positionMenu = true }, modifier = Modifier.fillMaxWidth()) { Text("Posição na estante: ${positionLabel(positionId)}") }
                    DropdownMenu(expanded = positionMenu, onDismissRequest = { positionMenu = false }) {
                        DropdownMenuItem(text = { Text("Sem posição") }, onClick = { positionId = null; positionMenu = false })
                        data.positions.sortedWith(compareBy({ shelvesById[it.shelfId]?.name }, { it.coordinate })).forEach { p ->
                            DropdownMenuItem(text = { Text(positionLabel(p.id)) }, onClick = { positionId = p.id; positionMenu = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                act(if (book.id == 0) "Livro cadastrado." else "Livro atualizado.") {
                    val newTotal = total.toIntOrNull() ?: throw StoreException("Informe a quantidade de exemplares.")
                    val available = if (book.id == 0) newTotal else store.availableFor(book.id, newTotal)
                    store.saveBook(
                        book.copy(
                            title = title, author = author, isbn = isbn.trim(), genre = genre.trim(),
                            year = year.toIntOrNull() ?: 0, location = location.trim(),
                            totalQuantity = newTotal, availableQuantity = available, posicaoEstanteId = positionId
                        )
                    )
                    onClose()
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } }
    )
}

@Composable
private fun LendDialog(book: BookEntity, data: LibraryData, store: LibraryStore, act: Act, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf<BorrowerEntity?>(null) }
    var quantity by remember { mutableStateOf("1") }
    val now = System.currentTimeMillis()
    val matches = data.borrowers.filter { query.isBlank() || isFuzzyMatch(query, it.name) || isFuzzyMatch(query, it.classRoom) }.sortedBy { it.name }.take(8)
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Emprestar “${book.title}”") },
        text = {
            Column(Modifier.widthIn(min = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchField(query, { query = it; chosen = null }, "Buscar leitor por nome ou turma")
                if (data.borrowers.isEmpty()) Text("Cadastre leitores na seção Leitores.", color = LibraryMuted, fontSize = 13.sp)
                matches.forEach { b ->
                    val blocked = b.isBlocked(now)
                    Row(
                        Modifier.fillMaxWidth().clickable { chosen = b }
                            .background(if (chosen?.id == b.id) LibrarySage else Color.Transparent, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = chosen?.id == b.id, onClick = { chosen = b })
                        Column(Modifier.weight(1f)) {
                            Text(b.name, color = LibraryInk)
                            Text("${b.type} · ${b.classRoom.ifBlank { b.identifier }}" + if (blocked) " · bloqueado até ${b.blockedUntil!!.asDate()}" else "", fontSize = 12.sp, color = if (blocked) StatusOverdueRedText else LibraryMuted)
                        }
                    }
                }
                if (book.availableQuantity > 1) {
                    OutlinedTextField(quantity, { quantity = it.filter(Char::isDigit).take(3) }, label = { Text("Quantidade (máx. ${book.availableQuantity})") }, singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(enabled = chosen != null, onClick = {
                act("Empréstimo registrado. Boa leitura!") {
                    store.lend(book.id, chosen!!.id, quantity.toIntOrNull() ?: 0)
                    onClose()
                }
            }) { Text("Emprestar") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } }
    )
}

// ==================================================================== Leitores

@Composable
private fun ReadersScreen(data: LibraryData, store: LibraryStore, act: Act) {
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<BorrowerEntity?>(null) }
    var deleting by remember { mutableStateOf<BorrowerEntity?>(null) }
    val now = System.currentTimeMillis()
    val filtered = data.borrowers.filter { query.isBlank() || isFuzzyMatch(query, it.name) || isFuzzyMatch(query, it.classRoom) }.sortedBy { it.name.lowercase() }
    Column {
        ScreenHeader("Leitores", "${data.borrowers.size} cadastrados") {
            Button(onClick = { editing = BorrowerEntity(name = "", type = "Aluno", identifier = "") }) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text("Novo leitor") }
        }
        SearchField(query, { query = it }, "Buscar por nome ou turma")
        Spacer(Modifier.height(12.dp))
        LazyColumn {
            items(filtered, key = { it.id }) { b ->
                val active = data.loans.count { it.borrowerName == b.name && it.returnDate == null }
                RowCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(b.name, fontWeight = FontWeight.SemiBold, color = LibraryInk)
                            Text(
                                listOf(b.type, b.classRoom.ifBlank { b.identifier }, b.phone).filter { it.isNotBlank() }.joinToString(" · ") +
                                    " · $active empréstimo(s) ativo(s)",
                                fontSize = 12.sp, color = LibraryMuted
                            )
                            if (b.isBlocked(now)) Text("Bloqueado por atraso até ${b.blockedUntil!!.asDate()}", fontSize = 12.sp, color = StatusOverdueRedText)
                        }
                        if (b.isBlocked(now)) TextButton(onClick = { act("Bloqueio removido.") { store.clearBlock(b.id) } }) { Text("Desbloquear") }
                        IconButton(onClick = { editing = b }) { Icon(Icons.Outlined.Edit, contentDescription = "Editar") }
                        IconButton(onClick = { deleting = b }) { Icon(Icons.Outlined.Delete, contentDescription = "Excluir") }
                    }
                }
            }
        }
    }
    editing?.let { b -> ReaderEditor(b, store, act) { editing = null } }
    deleting?.let { b ->
        ConfirmDialog("Excluir ${b.name}?", "O histórico de empréstimos é mantido.", "Excluir",
            onConfirm = { act("Leitor excluído.") { store.deleteBorrower(b.id) }; deleting = null }, onDismiss = { deleting = null })
    }
}

@Composable
private fun ReaderEditor(borrower: BorrowerEntity, store: LibraryStore, act: Act, onClose: () -> Unit) {
    var name by remember { mutableStateOf(borrower.name) }
    var type by remember { mutableStateOf(borrower.type.ifBlank { "Aluno" }) }
    var classRoom by remember { mutableStateOf(borrower.classRoom) }
    var identifier by remember { mutableStateOf(borrower.identifier) }
    var phone by remember { mutableStateOf(borrower.phone) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (borrower.id == 0) "Novo leitor" else "Editar leitor") },
        text = {
            Column(Modifier.widthIn(min = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf("Aluno", "Professor").forEach { t ->
                        RadioButton(selected = type == t, onClick = { type = t }); Text(t, Modifier.padding(end = 16.dp))
                    }
                }
                OutlinedTextField(classRoom, { classRoom = it }, label = { Text("Turma") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(identifier, { identifier = it }, label = { Text(if (type == "Aluno") "Matrícula" else "Matéria") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text("Telefone (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                act("Leitor salvo.") {
                    store.saveBorrower(borrower.copy(name = name, type = type, classRoom = classRoom.trim(), identifier = identifier.trim(), phone = phone.trim()))
                    onClose()
                }
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } }
    )
}

// ==================================================================== Empréstimos

private enum class LoanFilter(val label: String) { ACTIVE("Ativos"), OVERDUE("Atrasados"), HISTORY("Histórico") }

@Composable
private fun LoansScreen(data: LibraryData, store: LibraryStore, act: Act) {
    var filter by remember { mutableStateOf(LoanFilter.ACTIVE) }
    var query by remember { mutableStateOf("") }
    var returning by remember { mutableStateOf<LoanEntity?>(null) }
    val now = System.currentTimeMillis()
    val loans = data.loans.filter {
        when (filter) {
            LoanFilter.ACTIVE -> it.returnDate == null
            LoanFilter.OVERDUE -> it.returnDate == null && now > it.dueDate
            LoanFilter.HISTORY -> it.returnDate != null
        }
    }.filter { query.isBlank() || isFuzzyMatch(query, it.bookTitle) || isFuzzyMatch(query, it.borrowerName) }
        .sortedBy { if (filter == LoanFilter.HISTORY) -(it.returnDate ?: 0) else it.dueDate }
    val overdueCount = data.loans.count { it.returnDate == null && now > it.dueDate }

    Column {
        ScreenHeader("Empréstimos", "${data.loans.count { it.returnDate == null }} ativos · $overdueCount atrasados · prazo ${store.studentLoanDays} dias (alunos) e ${store.teacherLoanDays} (professores)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LoanFilter.entries.forEach { f -> FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) }) }
            Spacer(Modifier.width(12.dp))
            SearchField(query, { query = it }, "Buscar por livro ou leitor")
        }
        Text("Para um novo empréstimo, use Emprestar no Acervo.", fontSize = 12.sp, color = LibraryMuted, modifier = Modifier.padding(vertical = 8.dp))
        LazyColumn {
            items(loans, key = { it.id }) { loan ->
                val late = loan.returnDate == null && now > loan.dueDate
                RowCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(loan.bookTitle + if (loan.quantity > 1) " (${loan.quantity} exemplares)" else "", fontWeight = FontWeight.SemiBold, color = LibraryInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${loan.borrowerName} · ${loan.borrowerClass}", fontSize = 12.sp, color = LibraryMuted)
                            Text(
                                if (loan.returnDate != null) "Emprestado em ${loan.loanDate.asDate()} · devolvido em ${loan.returnDate.asDate()}"
                                else "Emprestado em ${loan.loanDate.asDate()} · devolver até ${loan.dueDate.asDate()}" + if (late) " · ATRASADO" else "",
                                fontSize = 12.sp,
                                color = if (late) StatusOverdueRedText else LibraryMuted,
                                fontWeight = if (late) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                        if (loan.returnDate == null) {
                            TextButton(onClick = { act("Prazo renovado por 7 dias.") { store.renewLoan(loan.id) } }) { Text("Renovar") }
                            Button(onClick = { if (loan.quantity > 1) returning = loan else act("Devolução registrada.") { store.returnLoan(loan.id) } }) { Text("Devolver") }
                        }
                    }
                }
            }
        }
    }

    returning?.let { loan ->
        var qty by remember(loan.id) { mutableStateOf(loan.quantity.toString()) }
        AlertDialog(
            onDismissRequest = { returning = null },
            title = { Text("Devolver “${loan.bookTitle}”") },
            text = { OutlinedTextField(qty, { qty = it.filter(Char::isDigit).take(3) }, label = { Text("Quantos exemplares? (de ${loan.quantity})") }, singleLine = true) },
            confirmButton = { Button(onClick = { act("Devolução registrada.") { store.returnLoan(loan.id, qty.toIntOrNull() ?: loan.quantity) }; returning = null }) { Text("Devolver") } },
            dismissButton = { TextButton(onClick = { returning = null }) { Text("Cancelar") } }
        )
    }
}

// ==================================================================== Estantes

@Composable
private fun ShelvesScreen(data: LibraryData, store: LibraryStore, act: Act, onOpenMap: () -> Unit) {
    var newShelf by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<ShelfEntity?>(null) }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        ScreenHeader("Estantes", "${data.shelves.size} estantes · ${data.positions.size} posições") {
            Button(onClick = onOpenMap, enabled = data.shelves.isNotEmpty()) { Icon(Icons.Outlined.ViewInAr, null); Spacer(Modifier.width(6.dp)); Text("Mapa 3D") }
        }
        Text("A medição com realidade aumentada é feita no app Android. Aqui você pode digitar as medidas no mapa 3D (toque na estante → Medidas).", fontSize = 12.sp, color = LibraryMuted)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(newShelf, { newShelf = it }, label = { Text("Nova estante") }, singleLine = true, modifier = Modifier.width(320.dp))
            Spacer(Modifier.width(8.dp))
            Button(enabled = newShelf.isNotBlank(), onClick = { act("Estante criada.") { store.saveShelf(ShelfEntity(name = newShelf)); newShelf = "" } }) { Text("Criar") }
        }
        Spacer(Modifier.height(12.dp))
        data.shelves.sortedBy { it.name.lowercase() }.forEach { shelf ->
            val positions = data.positions.filter { it.shelfId == shelf.id }.sortedBy { it.coordinate }
            var level by remember(shelf.id) { mutableStateOf("") }
            var column by remember(shelf.id) { mutableStateOf("") }
            RowCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(shelf.name, fontWeight = FontWeight.Bold, color = LibraryInk)
                            Text("${shelf.levelCount} prateleiras · ${shelf.columnCount} coluna(s) · " + if (shelf.mapX == null) "sem posição na planta" else "posicionada na planta (${if (shelf.layoutSource == "ar") "RA" else "manual"})", fontSize = 12.sp, color = LibraryMuted)
                        }
                        IconButton(onClick = { deleting = shelf }) { Icon(Icons.Outlined.Delete, contentDescription = "Excluir estante") }
                    }
                    positions.forEach { p ->
                        val books = data.books.filter { it.posicaoEstanteId == p.id }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
                            Text("• ${p.coordinate}", modifier = Modifier.weight(1f), color = LibraryInk, fontSize = 13.sp)
                            Text(if (books.isEmpty()) "vazia" else books.joinToString { it.title }, fontSize = 12.sp, color = LibraryMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(2f))
                            IconButton(onClick = { act("Posição removida; livros desvinculados.") { store.deletePosition(p.id) } }) { Icon(Icons.Outlined.Delete, contentDescription = "Excluir posição", modifier = Modifier.size(18.dp)) }
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = LibraryOutline.copy(alpha = 0.2f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(level, { level = it.filter(Char::isDigit).take(2) }, label = { Text("Prateleira nº") }, singleLine = true, modifier = Modifier.width(140.dp))
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(column, { column = it.filter(Char::isDigit).take(2) }, label = { Text("Coluna nº") }, singleLine = true, modifier = Modifier.width(140.dp))
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(enabled = level.toIntOrNull() != null, onClick = {
                            val l = level.toInt(); val c = column.toIntOrNull()
                            act("Posição criada.") {
                                // Amplia a grade da estante para a célula aparecer certa no mapa 3D.
                                shelf.expandedToFit(l, c).let { grown -> if (grown != shelf) store.saveShelf(grown) }
                                store.addPosition(shelf.id, formatCellCoordinate(l, c, maxOf(shelf.columnCount, c ?: 1)), accessibilityLevel = 2)
                                level = ""; column = ""
                            }
                        }) { Text("Adicionar posição") }
                    }
                }
            }
        }
    }
    deleting?.let { s ->
        ConfirmDialog("Excluir “${s.name}”?", "As posições desta estante serão removidas e os livros ficarão sem posição (o cadastro dos livros é mantido).", "Excluir",
            onConfirm = { act("Estante excluída.") { store.deleteShelf(s.id) }; deleting = null }, onDismiss = { deleting = null })
    }
}

// ==================================================================== Backup e configurações

@Composable
private fun SettingsScreen(store: LibraryStore, dataDir: Path, act: Act) {
    var pending by remember { mutableStateOf<Pair<File, com.example.data.BackupPayload>?>(null) }
    var student by remember { mutableStateOf(store.studentLoanDays.toString()) }
    var teacher by remember { mutableStateOf(store.teacherLoanDays.toString()) }
    Column(Modifier.verticalScroll(rememberScrollState()).widthIn(max = 760.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader("Backup e configurações", "Os dados ficam só neste computador, em $dataDir")
        RowCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Trocar dados com o celular", fontWeight = FontWeight.Bold)
                Text(
                    "No app Android, exporte o backup JSON e copie o arquivo para este computador; depois use Importar. " +
                        "Para levar os dados daqui para o celular, use Exportar e restaure o arquivo no app Android. " +
                        "A importação substitui todos os dados atuais. Fotos não são transferidas pelo JSON.",
                    fontSize = 13.sp, color = LibraryMuted
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val file = chooseFile(save = true, suggested = "educlib-backup-${SimpleDateFormat("yyyy-MM-dd").format(Date())}.json") ?: return@Button
                        act("Backup exportado para ${file.name}.") { Files.writeString(file.toPath(), store.exportJson()) }
                    }) { Text("Exportar backup") }
                    OutlinedButton(onClick = {
                        val file = chooseFile(save = false) ?: return@OutlinedButton
                        act(null) {
                            if (file.length() > BackupFormat.MAX_BACKUP_BYTES) throw StoreException("O arquivo é maior que 10 MB.")
                            val payload = BackupFormat.fromJson(file.readText()) ?: throw StoreException("O arquivo não é um backup JSON do EducLib.")
                            BackupFormat.validate(payload)?.let { throw StoreException(it) }
                            pending = file to payload
                        }
                    }) { Text("Importar backup") }
                }
            }
        }
        RowCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Prazos de empréstimo (dias)", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(student, { student = it.filter(Char::isDigit).take(3) }, label = { Text("Alunos") }, singleLine = true, modifier = Modifier.width(140.dp))
                    OutlinedTextField(teacher, { teacher = it.filter(Char::isDigit).take(3) }, label = { Text("Professores") }, singleLine = true, modifier = Modifier.width(140.dp))
                    Button(onClick = { act("Prazos salvos.") { store.setLoanDays(student.toLongOrNull() ?: 0, teacher.toLongOrNull() ?: 0) } }) { Text("Salvar") }
                }
            }
        }
        Text("EducLib é software livre sob a licença GPL-3.0-or-later.", fontSize = 12.sp, color = LibraryMuted)
    }

    pending?.let { (file, payload) ->
        val preview = BackupFormat.preview(payload)
        ConfirmDialog(
            title = "Importar ${file.name}?",
            text = "O backup tem ${preview.bookCount} livros, ${preview.borrowerCount} leitores, ${preview.loanCount} empréstimos e ${preview.shelfCount} estantes" +
                (preview.createdAt?.let { ", criado em ${it.asDate()}" } ?: "") +
                ". Todos os dados atuais deste computador serão substituídos. Exporte um backup antes, se quiser guardar os atuais." +
                if (preview.isLegacy) " (Backup antigo: só livros e empréstimos serão substituídos.)" else "",
            confirm = "Substituir dados",
            onConfirm = { act("Backup importado.") { store.importBackup(payload) }; pending = null },
            onDismiss = { pending = null }
        )
    }
}

private fun chooseFile(save: Boolean, suggested: String? = null): File? {
    val dialog = FileDialog(null as java.awt.Frame?, if (save) "Exportar backup do EducLib" else "Importar backup do EducLib", if (save) FileDialog.SAVE else FileDialog.LOAD)
    if (suggested != null) dialog.file = suggested
    if (!save) dialog.setFilenameFilter { _, name -> name.endsWith(".json", ignoreCase = true) }
    dialog.isVisible = true
    val name = dialog.file ?: return null
    return File(dialog.directory, name)
}

@Composable
private fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = { Text(text) },
    confirmButton = { Button(onClick = onConfirm) { Text(confirm) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
)

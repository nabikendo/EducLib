package com.example.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.BookEntity
import com.example.data.ShelfEntity
import com.example.data.ShelfPositionEntity
import com.example.ui.isFuzzyMatch
import com.example.ui.theme.LibraryForestDeep
import com.example.ui.theme.LibraryInk
import com.example.ui.theme.LibraryMuted
import com.example.ui.theme.LibraryOutline
import com.example.ui.theme.LibraryPaper
import java.util.Locale

private val WoodFront = Color(0xFFC9A27E)
private val WoodSide = Color(0xFFA9825F)
private val WoodTop = Color(0xFFE0C6A8)
private val EstimatedFront = Color(0xFFC8D6CD)
private val EstimatedSide = Color(0xFFAFC2B6)
private val EstimatedTop = Color(0xFFDCE6DF)
private val BookFill = Color(0xFF6E4F3A)
private val HighlightFill = Color(0xFFE0703A)
private val FloorFill = Color(0xFFEFEDE6)
private val GridLine = Color(0xFFD9D5CA)

/** Onde um livro está: estante + célula, ou nulo se não há posição cadastrada. */
data class BookLocation(val shelfId: Int, val cell: CellCoordinate, val position: ShelfPositionEntity)

fun locateBook(book: BookEntity, positionsById: Map<Int, ShelfPositionEntity>): BookLocation? {
    val position = book.posicaoEstanteId?.let { positionsById[it] } ?: return null
    return BookLocation(position.shelfId, parseCellCoordinate(position.coordinate), position)
}

fun describeCell(cell: CellCoordinate): String = listOfNotNull(
    cell.level?.let { "Prateleira $it" },
    cell.column?.let { "Coluna $it" }
).joinToString(" · ").ifEmpty { "posição sem prateleira numerada" }

private fun Double.fmt(decimals: Int = 2) = String.format(Locale.forLanguageTag("pt-BR"), "%.${decimals}f", this)

/**
 * Mapa 3D da biblioteca em tela cheia: estantes, células ocupadas e o livro procurado em destaque.
 * Funciona sem AR (estantes sem medidas aparecem em posição estimada).
 */
@Composable
fun LibraryMap3DDialog(
    shelves: List<ShelfEntity>,
    positions: List<ShelfPositionEntity>,
    books: List<BookEntity>,
    initialBookId: Int? = null,
    onSaveShelf: (ShelfEntity) -> Unit,
    /** Abre a varredura com RA; nulo onde não há RA (desktop). */
    onScanRequest: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val boxes = remember(shelves) { layoutShelves(shelves) }
    val positionsById = remember(positions) { positions.associateBy { it.id } }
    val shelvesById = remember(shelves) { shelves.associateBy { it.id } }
    val locations = remember(books, positionsById) {
        books.mapNotNull { book -> locateBook(book, positionsById)?.let { book to it } }
    }
    val occupiedCells = remember(locations) {
        locations.groupBy({ it.second.shelfId }, { it.second.cell }).mapValues { (_, cells) -> cells.filter { it.level != null }.distinct() }
    }

    var camera by remember { mutableStateOf(fitCamera(boxes)) }
    LaunchedEffect(boxes.size) { camera = fitCamera(boxes, camera.yawDeg, camera.pitchDeg) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedBookId by remember { mutableStateOf(initialBookId) }
    var selectedShelfId by remember { mutableStateOf<Int?>(null) }
    var query by remember { mutableStateOf("") }
    var editingShelf by remember { mutableStateOf<ShelfEntity?>(null) }

    val selectedBook = books.firstOrNull { it.id == selectedBookId }
    val selectedLocation = selectedBook?.let { locateBook(it, positionsById) }
    val currentLocation by rememberUpdatedState(selectedLocation)
    LaunchedEffect(selectedLocation?.shelfId) {
        val loc = selectedLocation ?: return@LaunchedEffect
        selectedShelfId = loc.shelfId
        // Gira a câmera para a frente da estante onde o livro está.
        boxes.firstOrNull { it.shelfId == loc.shelfId }?.let { box ->
            camera = camera.copy(
                target = Vec3(box.centerX, box.height / 2, box.centerZ),
                yawDeg = box.rotationDeg,
                pitchDeg = 25.0,
                distance = kotlin.math.max(box.width, box.height) * 1.8 + 2.0
            )
        }
    }
    val results = remember(query, books) {
        if (query.isBlank()) emptyList()
        else books.filter { isFuzzyMatch(query, it.title) || isFuzzyMatch(query, it.author) || it.isbn.contains(query.trim()) }.take(5)
    }
    val textMeasurer = rememberTextMeasurer()
    val estimatedCount = boxes.count { it.estimated }
    val sceneDescription = buildString {
        append("Mapa 3D com ${boxes.size} estantes.")
        if (selectedBook != null) {
            val shelfName = selectedLocation?.let { shelvesById[it.shelfId]?.name }
            append(if (shelfName != null) " ${selectedBook.title} está na estante $shelfName, ${describeCell(selectedLocation.cell)}." else " ${selectedBook.title} não tem posição cadastrada.")
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = LibraryPaper) {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Fechar mapa") }
                    Text(
                        "Mapa 3D da biblioteca",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LibraryInk,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { camera = fitCamera(boxes, camera.yawDeg, camera.pitchDeg) }) {
                        Icon(Icons.Outlined.CenterFocusStrong, contentDescription = "Centralizar mapa")
                    }
                    if (onScanRequest != null) {
                        IconButton(onClick = onScanRequest) {
                            Icon(Icons.Outlined.ViewInAr, contentDescription = "Escanear a biblioteca com realidade aumentada")
                        }
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    placeholder = { Text("Onde está o livro? Título, autor ou ISBN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                )
                results.forEach { book ->
                    val loc = locateBook(book, positionsById)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedBookId = book.id
                                query = ""
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(book.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = LibraryInk)
                            Text(
                                loc?.let { "${shelvesById[it.shelfId]?.name ?: "Estante"} · ${describeCell(it.cell)}" } ?: "Sem posição na estante",
                                fontSize = 12.sp,
                                color = LibraryMuted
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = camera.pitchDeg < 80,
                        onClick = { camera = camera.copy(pitchDeg = 40.0) },
                        label = { Text("3D") },
                        leadingIcon = { Icon(Icons.Outlined.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = camera.pitchDeg >= 80,
                        onClick = { camera = camera.copy(pitchDeg = 89.0) },
                        label = { Text("Planta") },
                        leadingIcon = { Icon(Icons.Outlined.Map, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    if (estimatedCount > 0) {
                        Text(
                            "$estimatedCount em posição estimada",
                            fontSize = 11.sp,
                            color = LibraryMuted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        // Sem o recorte, o piso e as estantes eram desenhados por cima do painel de informações.
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                ) {
                    if (boxes.isEmpty()) {
                        Text(
                            "Cadastre estantes para montar o mapa.",
                            color = LibraryMuted,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp)
                        )
                    } else {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { canvasSize = it }
                                .semantics { contentDescription = sceneDescription }
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        camera = camera.copy(
                                            yawDeg = camera.yawDeg - pan.x * 0.3,
                                            pitchDeg = (camera.pitchDeg + pan.y * 0.3).coerceIn(8.0, 89.0),
                                            distance = (camera.distance / zoom).coerceIn(1.0, 300.0)
                                        )
                                    }
                                }
                                .pointerInput(Unit) {
                                    // Roda do mouse/touchpad: aproxima e afasta (a pinça não existe com mouse).
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.type != PointerEventType.Scroll) continue
                                            val dy = event.changes.sumOf { it.scrollDelta.y.toDouble() }
                                            if (dy != 0.0) {
                                                camera = camera.copy(distance = (camera.distance * (1 + 0.12 * dy)).coerceIn(1.0, 300.0))
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    }
                                }
                                .pointerInput(boxes) {
                                    detectTapGestures { offset ->
                                        val hit = pickShelf(boxes, camera, canvasSize.width.toDouble(), canvasSize.height.toDouble(), offset.x.toDouble(), offset.y.toDouble())
                                        selectedShelfId = hit
                                        if (hit != null && currentLocation?.shelfId != hit) selectedBookId = null
                                    }
                                }
                        ) {
                            drawLibraryScene(
                                boxes = boxes,
                                camera = camera,
                                occupiedCells = occupiedCells,
                                highlightShelfId = selectedLocation?.shelfId,
                                highlightCell = selectedLocation?.cell,
                                selectedShelfId = selectedShelfId,
                                textMeasurer = textMeasurer
                            )
                        }
                        Column(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilledTonalIconButton(onClick = { camera = camera.copy(distance = (camera.distance / 1.25).coerceIn(1.0, 300.0)) }) {
                                Icon(Icons.Outlined.Add, contentDescription = "Aproximar")
                            }
                            FilledTonalIconButton(onClick = { camera = camera.copy(distance = (camera.distance * 1.25).coerceIn(1.0, 300.0)) }) {
                                Icon(Icons.Outlined.Remove, contentDescription = "Afastar")
                            }
                        }
                    }
                }

                MapInfoPanel(
                    selectedBook = selectedBook,
                    selectedLocation = selectedLocation,
                    selectedShelf = selectedShelfId?.let { shelvesById[it] },
                    selectedBox = boxes.firstOrNull { it.shelfId == selectedShelfId },
                    shelfBooks = selectedShelfId?.let { id -> locations.filter { it.second.shelfId == id } }.orEmpty(),
                    shelvesById = shelvesById,
                    onPickBook = { selectedBookId = it.id },
                    onEditShelf = { editingShelf = it },
                    canScan = onScanRequest != null
                )
            }
        }
    }

    editingShelf?.let { shelf ->
        ShelfLayoutEditorDialog(
            shelf = shelf,
            box = boxes.firstOrNull { it.shelfId == shelf.id },
            onDismiss = { editingShelf = null },
            onSave = {
                onSaveShelf(it)
                editingShelf = null
            }
        )
    }
}

@Composable
private fun MapInfoPanel(
    selectedBook: BookEntity?,
    selectedLocation: BookLocation?,
    selectedShelf: ShelfEntity?,
    selectedBox: ShelfBox?,
    shelfBooks: List<Pair<BookEntity, BookLocation>>,
    shelvesById: Map<Int, ShelfEntity>,
    onPickBook: (BookEntity) -> Unit,
    onEditShelf: (ShelfEntity) -> Unit,
    canScan: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp).heightIn(max = 220.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, LibraryOutline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp).verticalScroll(rememberScrollState())) {
            when {
                selectedBook != null -> {
                    Text(selectedBook.title, fontWeight = FontWeight.Bold, color = LibraryInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(selectedBook.author, fontSize = 12.sp, color = LibraryMuted)
                    Spacer(Modifier.height(6.dp))
                    if (selectedLocation != null) {
                        val shelfName = shelvesById[selectedLocation.shelfId]?.name ?: "Estante"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp).background(HighlightFill, RoundedCornerShape(3.dp)))
                            Spacer(Modifier.width(6.dp))
                            Text("$shelfName · ${describeCell(selectedLocation.cell)}", fontWeight = FontWeight.SemiBold, color = LibraryForestDeep)
                        }
                        Text("Posição cadastrada: “${selectedLocation.position.coordinate}”", fontSize = 11.sp, color = LibraryMuted)
                    } else {
                        Text("Este livro ainda não foi vinculado a uma posição de estante (aba ROOF).", fontSize = 12.sp, color = LibraryMuted)
                    }
                    Text(
                        "${selectedBook.availableQuantity} de ${selectedBook.totalQuantity} exemplares disponíveis",
                        fontSize = 12.sp,
                        color = LibraryMuted
                    )
                }
                selectedShelf != null -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(selectedShelf.name, fontWeight = FontWeight.Bold, color = LibraryInk)
                            val source = when {
                                selectedBox?.estimated == true -> "posição estimada — escaneie ou edite as medidas"
                                selectedShelf.layoutSource == "ar" -> "medida com AR"
                                else -> "medidas informadas manualmente"
                            }
                            Text(
                                "${selectedShelf.widthM.fmt()} × ${selectedShelf.heightM.fmt()} m · ${selectedShelf.levelCount} prateleiras · $source",
                                fontSize = 11.sp,
                                color = LibraryMuted
                            )
                        }
                        TextButton(onClick = { onEditShelf(selectedShelf) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Medidas")
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (shelfBooks.isEmpty()) {
                        Text("Nenhum livro vinculado a esta estante.", fontSize = 12.sp, color = LibraryMuted)
                    } else {
                        shelfBooks.sortedWith(compareBy({ it.second.cell.level ?: Int.MAX_VALUE }, { it.first.title })).forEach { (book, loc) ->
                            Text(
                                "• ${book.title} — ${describeCell(loc.cell)}",
                                fontSize = 12.sp,
                                color = LibraryInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth().clickable { onPickBook(book) }.padding(vertical = 3.dp)
                            )
                        }
                    }
                }
                else -> Text(
                    "Arraste para girar e aproxime com pinça, roda do mouse ou os botões + e −. " +
                        "Toque numa estante para ver seus livros e use Medidas para posicioná-la na planta." +
                        (if (canScan) " O botão de RA mede as estantes com a câmera." else ""),
                    fontSize = 12.sp,
                    color = LibraryMuted
                )
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawLibraryScene(
    boxes: List<ShelfBox>,
    camera: OrbitCamera,
    occupiedCells: Map<Int, List<CellCoordinate>>,
    highlightShelfId: Int?,
    highlightCell: CellCoordinate?,
    selectedShelfId: Int?,
    textMeasurer: TextMeasurer
) {
    val w = size.width.toDouble()
    val h = size.height.toDouble()
    fun p(v: Vec3): Offset? = project(v, camera, w, h)?.let { Offset(it.x.toFloat(), it.y.toFloat()) }
    fun polygon(points: List<Vec3>, fill: Color, stroke: Color? = null, strokeWidth: Float = 1.5f) {
        val projected = points.map { p(it) ?: return }
        val path = Path().apply {
            moveTo(projected[0].x, projected[0].y)
            projected.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        drawPath(path, fill)
        if (stroke != null) drawPath(path, stroke, style = Stroke(width = strokeWidth))
    }
    fun line(a: Vec3, b: Vec3, color: Color, width: Float = 1f) {
        val pa = p(a) ?: return
        val pb = p(b) ?: return
        drawLine(color, pa, pb, strokeWidth = width)
    }

    // Piso e grade de 1 m ao redor das estantes.
    val margin = 1.5
    val corners = boxes.flatMap { b -> listOf(b.local(-b.width / 2, 0.0, -b.depth / 2), b.local(b.width / 2, 0.0, b.depth / 2), b.local(-b.width / 2, 0.0, b.depth / 2), b.local(b.width / 2, 0.0, -b.depth / 2)) }
    val minX = kotlin.math.floor(corners.minOf { it.x } - margin)
    val maxX = kotlin.math.ceil(corners.maxOf { it.x } + margin)
    val minZ = kotlin.math.floor(corners.minOf { it.z } - margin)
    val maxZ = kotlin.math.ceil(corners.maxOf { it.z } + margin)
    polygon(listOf(Vec3(minX, 0.0, minZ), Vec3(maxX, 0.0, minZ), Vec3(maxX, 0.0, maxZ), Vec3(minX, 0.0, maxZ)), FloorFill)
    if ((maxX - minX) + (maxZ - minZ) < 400) {
        var gx = minX
        while (gx <= maxX) { line(Vec3(gx, 0.0, minZ), Vec3(gx, 0.0, maxZ), GridLine); gx += 1.0 }
        var gz = minZ
        while (gz <= maxZ) { line(Vec3(minX, 0.0, gz), Vec3(maxX, 0.0, gz), GridLine); gz += 1.0 }
    }

    for (face in visibleFaces(boxes, camera)) {
        val b = face.box
        val (front, side, top) = if (b.estimated) Triple(EstimatedFront, EstimatedSide, EstimatedTop) else Triple(WoodFront, WoodSide, WoodTop)
        val fill = when (face.kind) {
            FaceKind.FRONT -> front
            FaceKind.TOP -> top
            else -> side
        }
        val outline = if (b.shelfId == selectedShelfId || b.shelfId == highlightShelfId) LibraryForestDeep else LibraryOutline.copy(alpha = 0.7f)
        polygon(face.corners, fill, outline, if (outline == LibraryForestDeep) 3f else 1.2f)
        if (face.kind == FaceKind.FRONT) {
            occupiedCells[b.shelfId].orEmpty().forEach { cell -> polygon(inset(b.cellQuad(cell)), BookFill.copy(alpha = 0.55f)) }
            for (i in 1 until b.levels) {
                val y = b.height * i / b.levels
                line(b.local(-b.width / 2, y, b.depth / 2 + 0.005), b.local(b.width / 2, y, b.depth / 2 + 0.005), LibraryForestDeep.copy(alpha = 0.45f), 1.5f)
            }
            for (i in 1 until b.columns) {
                val x = -b.width / 2 + b.width * i / b.columns
                line(b.local(x, 0.0, b.depth / 2 + 0.005), b.local(x, b.height, b.depth / 2 + 0.005), LibraryForestDeep.copy(alpha = 0.45f), 1.5f)
            }
            if (b.shelfId == highlightShelfId) {
                polygon(inset(b.cellQuad(highlightCell)), HighlightFill.copy(alpha = 0.9f), Color.White, 2f)
            }
        }
    }

    // Nomes das estantes.
    for (b in boxes) {
        val anchor = p(b.topCenter() + Vec3(0.0, 0.12, 0.0)) ?: continue
        val label = textMeasurer.measure(
            b.name,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = if (b.shelfId == selectedShelfId) FontWeight.Bold else FontWeight.Medium,
                color = if (b.estimated) LibraryMuted else LibraryInk
            ),
            maxLines = 1
        )
        val topLeft = Offset(anchor.x - label.size.width / 2f, anchor.y - label.size.height)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = topLeft - Offset(4f, 2f),
            size = Size(label.size.width + 8f, label.size.height + 4f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawText(label, topLeft = topLeft)
    }

    // Marcador do livro procurado: sempre por cima, visível mesmo atrás da estante.
    val target = boxes.firstOrNull { it.shelfId == highlightShelfId } ?: return
    val quad = target.cellQuad(highlightCell)
    val cellCenter = quad.reduce { acc, v -> acc + v } * 0.25
    val pinTop = Vec3(cellCenter.x, target.height + 0.6, cellCenter.z)
    val base = p(cellCenter) ?: return
    val head = p(pinTop) ?: return
    drawLine(HighlightFill, base, head, strokeWidth = 4f)
    drawCircle(HighlightFill, radius = 14f, center = head)
    drawCircle(Color.White, radius = 6f, center = head)
}

/** Encolhe um quadrilátero em direção ao centro (margem visual entre células). */
private fun inset(quad: List<Vec3>, factor: Double = 0.88): List<Vec3> {
    val c = quad.reduce { acc, v -> acc + v } * 0.25
    return quad.map { c + (it - c) * factor }
}

@Composable
fun ShelfLayoutEditorDialog(
    shelf: ShelfEntity,
    box: ShelfBox?,
    onDismiss: () -> Unit,
    onSave: (ShelfEntity) -> Unit
) {
    fun Double.field() = String.format(Locale.US, "%.2f", this)
    var x by remember { mutableStateOf((shelf.mapX ?: box?.centerX ?: 0.0).field()) }
    var z by remember { mutableStateOf((shelf.mapZ ?: box?.centerZ ?: 0.0).field()) }
    var rotation by remember { mutableStateOf(shelf.rotationDeg.field()) }
    var width by remember { mutableStateOf(shelf.widthM.field()) }
    var depth by remember { mutableStateOf(shelf.depthM.field()) }
    var height by remember { mutableStateOf(shelf.heightM.field()) }
    var levels by remember { mutableStateOf(shelf.levelCount.toString()) }
    var columns by remember { mutableStateOf(shelf.columnCount.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    // Só números finitos: "Infinity", "NaN" ou "1e309" passariam pelo toDoubleOrNull e impediriam o backup (o JSON não os representa).
    fun num(s: String) = s.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Medidas de ${shelf.name}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Metros, a partir do ponto de referência da planta. Rotação: direção da frente da estante, em graus.",
                    fontSize = 12.sp,
                    color = LibraryMuted
                )
                @Composable
                fun field(label: String, value: String, onChange: (String) -> Unit, integer: Boolean = false) = OutlinedTextField(
                    value = value,
                    onValueChange = onChange,
                    label = { Text(label) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { field("Posição X", x, { x = it }) }
                    Box(Modifier.weight(1f)) { field("Posição Z", z, { z = it }) }
                }
                field("Rotação (°)", rotation, { rotation = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { field("Largura", width, { width = it }) }
                    Box(Modifier.weight(1f)) { field("Profund.", depth, { depth = it }) }
                    Box(Modifier.weight(1f)) { field("Altura", height, { height = it }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { field("Prateleiras", levels, { levels = it }, integer = true) }
                    Box(Modifier.weight(1f)) { field("Colunas", columns, { columns = it }, integer = true) }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val vx = num(x); val vz = num(z); val vr = num(rotation)
                val vw = num(width); val vd = num(depth); val vh = num(height)
                val vl = levels.trim().toIntOrNull(); val vc = columns.trim().toIntOrNull()
                val max = ShelfEntity.MAX_DIMENSION_M
                error = when {
                    vx == null || vz == null || vr == null || kotlin.math.abs(vx) > 1000 || kotlin.math.abs(vz) > 1000 -> "Informe X, Z e rotação como números."
                    vw == null || vw <= 0.05 || vw > max -> "Largura deve estar entre 0,05 e ${max.toInt()} m."
                    vd == null || vd <= 0.05 || vd > max -> "Profundidade deve estar entre 0,05 e ${max.toInt()} m."
                    vh == null || vh <= 0.1 || vh > max -> "Altura deve estar entre 0,1 e ${max.toInt()} m."
                    vl == null || vl !in 1..ShelfEntity.MAX_LEVELS -> "Prateleiras: de 1 a ${ShelfEntity.MAX_LEVELS}."
                    vc == null || vc !in 1..ShelfEntity.MAX_COLUMNS -> "Colunas: de 1 a ${ShelfEntity.MAX_COLUMNS}."
                    else -> null
                }
                if (error == null) {
                    onSave(
                        shelf.copy(
                            mapX = vx, mapZ = vz, rotationDeg = vr!! % 360.0, widthM = vw!!, depthM = vd!!, heightM = vh!!,
                            levelCount = vl!!, columnCount = vc!!, layoutSource = "manual"
                        )
                    )
                }
            }) { Text("Salvar") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}


package com.example.map

import com.example.data.ShelfEntity
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/*
 * Geometria do mapa 3D da biblioteca. Código puro (sem Android) para ser testável em JVM.
 *
 * Convenção de eixos (igual à do ARCore, sem espelhamento): Y para cima, planta no plano XZ.
 * Uma estante com rotação θ tem eixo local X (esquerda→direita, vista de frente) = (cos θ, 0, -sin θ)
 * e eixo local Z (frente) = (sin θ, 0, cos θ). O ponto (mapX, mapZ) é o centro da base.
 */

data class Vec3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Double) = Vec3(x * s, y * s, z * s)
    infix fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    fun length() = sqrt(this dot this)

    companion object {
        val ZERO = Vec3(0.0, 0.0, 0.0)
    }
}

/** Ponto 2D na planta (metros). */
data class Vec2(val x: Double, val z: Double) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, z + o.z)
    operator fun minus(o: Vec2) = Vec2(x - o.x, z - o.z)
    operator fun times(s: Double) = Vec2(x * s, z * s)
    infix fun dot(o: Vec2) = x * o.x + z * o.z
    fun length() = sqrt(this dot this)
}

/** Célula dentro de uma estante; nulo = a estante inteira naquela dimensão. */
data class CellCoordinate(val level: Int?, val column: Int?)

private val LEVEL_REGEX = Regex("""(?i)\b(?:prateleira|prat\.?|n[ií]vel|andar|level|shelf|p)\s*[:#nº°o.]*\s*(\d{1,2})\b""")
private val COLUMN_REGEX = Regex("""(?i)\b(?:coluna|col\.?|fileira|m[oó]dulo|se[cç][aã]o|bloco|column|c)\s*[:#nº°o.]*\s*(\d{1,2})\b""")

/**
 * Interpreta o texto livre de uma posição ("Fileira 2, Prateleira 3", "Prat. 4", "Coluna 1 - P2").
 * Números que não estão acompanhados de uma palavra reconhecida são ignorados.
 */
fun parseCellCoordinate(text: String): CellCoordinate {
    val level = LEVEL_REGEX.find(text)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > 0 }
    val column = COLUMN_REGEX.find(text)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > 0 }
    return CellCoordinate(level, column)
}

/** Texto padronizado gravado ao criar posições pelos seletores estruturados. */
fun formatCellCoordinate(level: Int, column: Int?, columnCount: Int): String =
    if (column != null && columnCount > 1) "Coluna $column, Prateleira $level" else "Prateleira $level"

/** Estante pronta para desenhar. `estimated` = sem medidas reais, colocada automaticamente. */
data class ShelfBox(
    val shelfId: Int,
    val name: String,
    val centerX: Double,
    val centerZ: Double,
    val rotationDeg: Double,
    val width: Double,
    val depth: Double,
    val height: Double,
    val levels: Int,
    val columns: Int,
    val estimated: Boolean
) {
    private val theta get() = Math.toRadians(rotationDeg)
    val axisX: Vec3 get() = Vec3(cos(theta), 0.0, -sin(theta))
    val axisZ: Vec3 get() = Vec3(sin(theta), 0.0, cos(theta))

    /** Converte coordenadas locais (lx, ly, lz) em mundo; ly = 0 é o piso. */
    fun local(lx: Double, ly: Double, lz: Double): Vec3 =
        Vec3(centerX, 0.0, centerZ) + axisX * lx + axisZ * lz + Vec3(0.0, ly, 0.0)

    /** Quadrilátero da célula na face frontal, levemente à frente para não "brigar" com a face. */
    fun cellQuad(cell: CellCoordinate?): List<Vec3> {
        val level = cell?.level?.takeIf { it in 1..levels }
        val column = cell?.column?.takeIf { it in 1..columns }
        val levelH = height / levels
        val colW = width / columns
        val y0 = if (level != null) (level - 1) * levelH else 0.0
        val y1 = if (level != null) level * levelH else height
        val x0 = if (column != null) -width / 2 + (column - 1) * colW else -width / 2
        val x1 = if (column != null) x0 + colW else width / 2
        val z = depth / 2 + 0.01
        return listOf(local(x0, y0, z), local(x1, y0, z), local(x1, y1, z), local(x0, y1, z))
    }

    fun topCenter(): Vec3 = local(0.0, height, 0.0)
}

private fun Double.sane(min: Double, fallback: Double) = if (isNaN() || isInfinite() || this < min) fallback else this

/**
 * Monta as caixas do mapa. Estantes posicionadas usam suas medidas; as demais são enfileiradas
 * atrás delas (6 por fileira, corredores de 1 m), para o mapa já ser útil antes de qualquer varredura.
 */
fun layoutShelves(shelves: List<ShelfEntity>): List<ShelfBox> {
    fun box(s: ShelfEntity, x: Double, z: Double, estimated: Boolean) = ShelfBox(
        shelfId = s.id,
        name = s.name,
        centerX = x,
        centerZ = z,
        rotationDeg = if (estimated) 0.0 else s.rotationDeg.sane(-1e6, 0.0),
        width = s.widthM.sane(0.1, ShelfEntity.DEFAULT_WIDTH_M),
        depth = s.depthM.sane(0.05, ShelfEntity.DEFAULT_DEPTH_M),
        height = s.heightM.sane(0.1, ShelfEntity.DEFAULT_HEIGHT_M),
        levels = s.levelCount.coerceIn(1, ShelfEntity.MAX_LEVELS),
        columns = s.columnCount.coerceIn(1, ShelfEntity.MAX_COLUMNS),
        estimated = estimated
    )

    val placed = shelves.filter { it.mapX != null && it.mapZ != null }
        .map { box(it, it.mapX!!, it.mapZ!!, estimated = false) }
    val unplaced = shelves.filter { it.mapX == null || it.mapZ == null }
    if (unplaced.isEmpty()) return placed

    val startZ = if (placed.isEmpty()) 0.0 else placed.maxOf { it.centerZ + max(it.width, it.depth) } + 2.0
    val startX = if (placed.isEmpty()) 0.0 else placed.minOf { it.centerX - max(it.width, it.depth) / 2 }
    val perRow = 6
    val estimated = mutableListOf<ShelfBox>()
    var cursorX = startX
    unplaced.forEachIndexed { index, shelf ->
        if (index % perRow == 0) cursorX = startX
        val row = index / perRow
        val width = shelf.widthM.sane(0.1, ShelfEntity.DEFAULT_WIDTH_M)
        val b = box(shelf, cursorX + width / 2, startZ + row * 2.0, estimated = true)
        estimated += b
        cursorX += width + 1.0
    }
    return placed + estimated
}

/** Câmera orbital: gira em torno de `target`; pitch 90° = vista de cima (planta). */
data class OrbitCamera(
    val target: Vec3,
    val yawDeg: Double,
    val pitchDeg: Double,
    val distance: Double,
    val fovDeg: Double = 50.0
) {
    fun position(): Vec3 {
        val yaw = Math.toRadians(yawDeg)
        val pitch = Math.toRadians(pitchDeg)
        return target + Vec3(distance * cos(pitch) * sin(yaw), distance * sin(pitch), distance * cos(pitch) * cos(yaw))
    }
}

data class ScreenPoint(val x: Double, val y: Double, val depth: Double)

const val NEAR_PLANE = 0.05

/** Projeção em perspectiva para uma tela `width`×`height` (px). Nulo = atrás da câmera. */
fun project(p: Vec3, camera: OrbitCamera, width: Double, height: Double): ScreenPoint? {
    val yaw = Math.toRadians(camera.yawDeg)
    val pitch = Math.toRadians(camera.pitchDeg)
    val d = p - camera.target
    val x1 = d.x * cos(yaw) - d.z * sin(yaw)
    val z1 = d.x * sin(yaw) + d.z * cos(yaw)
    val y2 = d.y * cos(pitch) - z1 * sin(pitch)
    val z2 = d.y * sin(pitch) + z1 * cos(pitch)
    val depth = camera.distance - z2
    if (depth < NEAR_PLANE) return null
    val focal = (height / 2) / tan(Math.toRadians(camera.fovDeg) / 2)
    return ScreenPoint(width / 2 + focal * x1 / depth, height / 2 - focal * y2 / depth, depth)
}

/** Enquadra todas as estantes com uma vista 3D inclinada. */
fun fitCamera(boxes: List<ShelfBox>, yawDeg: Double = 30.0, pitchDeg: Double = 40.0): OrbitCamera {
    if (boxes.isEmpty()) return OrbitCamera(Vec3.ZERO, yawDeg, pitchDeg, 6.0)
    val corners = boxes.flatMap { b ->
        listOf(-1.0, 1.0).flatMap { sx -> listOf(-1.0, 1.0).map { sz -> b.local(sx * b.width / 2, 0.0, sz * b.depth / 2) } }
    }
    val minX = corners.minOf { it.x }
    val maxX = corners.maxOf { it.x }
    val minZ = corners.minOf { it.z }
    val maxZ = corners.maxOf { it.z }
    val maxH = boxes.maxOf { it.height }
    val extent = max(max(maxX - minX, maxZ - minZ), maxH)
    return OrbitCamera(
        target = Vec3((minX + maxX) / 2, maxH / 3, (minZ + maxZ) / 2),
        yawDeg = yawDeg,
        pitchDeg = pitchDeg,
        distance = extent * 1.3 + 2.5
    )
}

enum class FaceKind { FRONT, BACK, LEFT, RIGHT, TOP }

data class BoxFace(val box: ShelfBox, val kind: FaceKind, val corners: List<Vec3>, val distance: Double)

/** Faces voltadas para a câmera, da mais distante para a mais próxima (algoritmo do pintor). */
fun visibleFaces(boxes: List<ShelfBox>, camera: OrbitCamera): List<BoxFace> {
    val eye = camera.position()
    val faces = mutableListOf<BoxFace>()
    for (b in boxes) {
        val w = b.width / 2
        val d = b.depth / 2
        val h = b.height
        val up = Vec3(0.0, 1.0, 0.0)
        val candidates = listOf(
            Triple(FaceKind.FRONT, b.axisZ, listOf(b.local(-w, 0.0, d), b.local(w, 0.0, d), b.local(w, h, d), b.local(-w, h, d))),
            Triple(FaceKind.BACK, b.axisZ * -1.0, listOf(b.local(w, 0.0, -d), b.local(-w, 0.0, -d), b.local(-w, h, -d), b.local(w, h, -d))),
            Triple(FaceKind.LEFT, b.axisX * -1.0, listOf(b.local(-w, 0.0, -d), b.local(-w, 0.0, d), b.local(-w, h, d), b.local(-w, h, -d))),
            Triple(FaceKind.RIGHT, b.axisX, listOf(b.local(w, 0.0, d), b.local(w, 0.0, -d), b.local(w, h, -d), b.local(w, h, d))),
            Triple(FaceKind.TOP, up, listOf(b.local(-w, h, d), b.local(w, h, d), b.local(w, h, -d), b.local(-w, h, -d)))
        )
        for ((kind, normal, corners) in candidates) {
            val center = corners.reduce { acc, v -> acc + v } * 0.25
            if (normal dot (eye - center) > 0) faces += BoxFace(b, kind, corners, (eye - center).length())
        }
    }
    return faces.sortedByDescending { it.distance }
}

/** Estante sob o ponto tocado (px), considerando a face visível mais próxima. */
fun pickShelf(boxes: List<ShelfBox>, camera: OrbitCamera, width: Double, height: Double, x: Double, y: Double): Int? =
    visibleFaces(boxes, camera).asReversed().firstOrNull { face ->
        val projected = face.corners.map { project(it, camera, width, height) ?: return@firstOrNull false }
        pointInConvexPolygon(x, y, projected.map { it.x to it.y })
    }?.box?.shelfId

/** Teste de ponto em polígono convexo (qualquer orientação). */
fun pointInConvexPolygon(px: Double, py: Double, polygon: List<Pair<Double, Double>>): Boolean {
    if (polygon.size < 3) return false
    var sign = 0
    for (i in polygon.indices) {
        val (ax, ay) = polygon[i]
        val (bx, by) = polygon[(i + 1) % polygon.size]
        val cross = (bx - ax) * (py - ay) - (by - ay) * (px - ax)
        if (abs(cross) < 1e-9) continue
        val s = if (cross > 0) 1 else -1
        if (sign == 0) sign = s else if (s != sign) return false
    }
    return true
}

// ---------------------------------------------------------------------------------------------
// Varredura AR: converte pontos medidos no espaço do ARCore em coordenadas da planta.
// ---------------------------------------------------------------------------------------------

/**
 * Referencial da planta definido por dois pontos fixos no piso (ex.: os dois lados da porta).
 * Origem em [a], eixo X da planta de [a] para [b]. Repetindo os mesmos pontos em outra sessão,
 * estantes medidas em dias diferentes ficam no mesmo mapa.
 */
class MapReference private constructor(private val origin: Vec2, private val u: Vec2) {
    private val v = Vec2(-u.z, u.x)

    fun toMap(world: Vec3): Vec2 {
        val d = Vec2(world.x, world.z) - origin
        return Vec2(d dot u, d dot v)
    }

    companion object {
        const val MIN_REFERENCE_DISTANCE_M = 0.3

        fun fromPoints(a: Vec3, b: Vec3): MapReference? {
            val ab = Vec2(b.x - a.x, b.z - a.z)
            val len = ab.length()
            if (len < MIN_REFERENCE_DISTANCE_M) return null
            return MapReference(Vec2(a.x, a.z), ab * (1.0 / len))
        }
    }
}

data class ArShelfMeasurement(
    val mapX: Double,
    val mapZ: Double,
    val rotationDeg: Double,
    val widthM: Double,
    val heightM: Double?
)

sealed interface ArMeasureResult {
    data class Ok(val measurement: ArShelfMeasurement) : ArMeasureResult
    data class Error(val message: String) : ArMeasureResult
}

const val MIN_SHELF_WIDTH_M = 0.2
const val MAX_SHELF_WIDTH_M = 20.0

/**
 * Calcula a estante a partir dos cantos inferiores frontais (esquerdo e direito, vistos de frente)
 * e da posição do aparelho no momento da marcação. A frente é o lado em que a pessoa estava, então
 * marcar os cantos na ordem trocada não espelha a estante.
 */
fun measureShelf(
    reference: MapReference,
    leftBottom: Vec3,
    rightBottom: Vec3,
    devicePosition: Vec3,
    depthM: Double,
    floorY: Double,
    topY: Double?
): ArMeasureResult {
    val l = reference.toMap(leftBottom)
    val r = reference.toMap(rightBottom)
    val cam = reference.toMap(devicePosition)
    val lr = r - l
    val width = lr.length()
    if (width < MIN_SHELF_WIDTH_M) return ArMeasureResult.Error("Os cantos estão muito próximos (${"%.2f".format(width)} m). Marque as duas pontas da estante.")
    if (width > MAX_SHELF_WIDTH_M) return ArMeasureResult.Error("Largura de ${"%.1f".format(width)} m parece errada. Refaça a marcação dos cantos.")
    val mid = (l + r) * 0.5
    var front = Vec2(-lr.z / width, lr.x / width)
    if (front dot (cam - mid) < 0) front = front * -1.0
    val rotation = Math.toDegrees(atan2(front.x, front.z))
    val center = mid - front * (depthM / 2)
    val height = topY?.let { it - floorY }?.takeIf { it > 0.1 && it <= ShelfEntity.MAX_DIMENSION_M }
    return ArMeasureResult.Ok(ArShelfMeasurement(center.x, center.z, rotation, width, height))
}

/** Interseção do raio (origem, direção) com o plano horizontal y = [planeY]; nulo se não cruza à frente. */
fun intersectHorizontalPlane(origin: Vec3, direction: Vec3, planeY: Double): Vec3? {
    if (abs(direction.y) < 1e-6) return null
    val t = (planeY - origin.y) / direction.y
    if (t <= 0 || t > 30) return null
    return origin + direction * t
}

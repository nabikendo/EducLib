package com.example.map

import com.example.data.ShelfEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LibraryMapMathTest {
    private val eps = 1e-6

    @Test
    fun parsesFreeTextCellCoordinates() {
        assertEquals(CellCoordinate(3, 2), parseCellCoordinate("Fileira 2, Prateleira 3"))
        assertEquals(CellCoordinate(4, null), parseCellCoordinate("Prat. 4"))
        assertEquals(CellCoordinate(2, 1), parseCellCoordinate("Coluna 1 - P2"))
        assertEquals(CellCoordinate(1, null), parseCellCoordinate("nível 1"))
        assertEquals(CellCoordinate(null, null), parseCellCoordinate("Canto da janela 7"))
        val formatted = formatCellCoordinate(level = 5, column = 3, columnCount = 4)
        assertEquals(CellCoordinate(5, 3), parseCellCoordinate(formatted))
        assertEquals("Prateleira 2", formatCellCoordinate(level = 2, column = 1, columnCount = 1))
    }

    @Test
    fun unplacedShelvesGetDistinctEstimatedSpotsBehindPlacedOnes() {
        val placed = ShelfEntity(id = 1, name = "A", mapX = 0.0, mapZ = 0.0, widthM = 2.0)
        val loose = (2..9).map { ShelfEntity(id = it, name = "S$it") }
        val boxes = layoutShelves(listOf(placed) + loose)
        assertEquals(9, boxes.size)
        assertFalse(boxes.first { it.shelfId == 1 }.estimated)
        val estimated = boxes.filter { it.estimated }
        assertEquals(8, estimated.size)
        assertTrue(estimated.all { it.centerZ > 1.0 })
        val spots = estimated.map { it.centerX to it.centerZ }.toSet()
        assertEquals("estantes estimadas não podem se sobrepor", 8, spots.size)
    }

    @Test
    fun invalidStoredDimensionsFallBackToDefaults() {
        val box = layoutShelves(listOf(ShelfEntity(id = 1, name = "X", mapX = 1.0, mapZ = 1.0, widthM = Double.NaN, levelCount = 0))).single()
        assertEquals(ShelfEntity.DEFAULT_WIDTH_M, box.width, eps)
        assertEquals(1, box.levels)
    }

    @Test
    fun cameraTargetProjectsToScreenCenterAndBehindIsHidden() {
        val camera = OrbitCamera(target = Vec3(1.0, 0.5, -2.0), yawDeg = 37.0, pitchDeg = 25.0, distance = 5.0)
        val center = project(camera.target, camera, 800.0, 600.0)!!
        assertEquals(400.0, center.x, eps)
        assertEquals(300.0, center.y, eps)
        assertEquals(5.0, center.depth, eps)
        // Ponto atrás da câmera (além da posição dela, na mesma direção) não é projetado.
        val behind = camera.target + (camera.position() - camera.target) * 1.5
        assertNull(project(behind, camera, 800.0, 600.0))
        // Um ponto acima do alvo aparece mais alto na tela.
        val above = project(camera.target + Vec3(0.0, 1.0, 0.0), camera, 800.0, 600.0)!!
        assertTrue(above.y < center.y)
    }

    @Test
    fun cellQuadSitsOnTheRequestedLevelOfTheFrontFace() {
        val box = ShelfBox(1, "A", 0.0, 0.0, 0.0, width = 2.0, depth = 0.4, height = 2.0, levels = 4, columns = 2, estimated = false)
        val quad = box.cellQuad(CellCoordinate(level = 2, column = 2))
        assertEquals(0.5, quad.minOf { it.y }, eps)
        assertEquals(1.0, quad.maxOf { it.y }, eps)
        assertEquals(0.0, quad.minOf { it.x }, eps)
        assertEquals(1.0, quad.maxOf { it.x }, eps)
        assertTrue(quad.all { it.z > 0.2 })
        // Nível fora do intervalo destaca a estante toda.
        val whole = box.cellQuad(CellCoordinate(level = 9, column = null))
        assertEquals(2.0, whole.maxOf { it.y }, eps)
    }

    @Test
    fun arMeasurementFacesTheDeviceAndIsIndependentOfCornerOrder() {
        val reference = MapReference.fromPoints(Vec3(0.0, 0.0, 0.0), Vec3(1.0, 0.0, 0.0))!!
        val left = Vec3(-0.5, 0.0, -2.0)
        val right = Vec3(0.5, 0.0, -2.0)
        val device = Vec3(0.0, 1.4, 0.0)
        val ok = measureShelf(reference, left, right, device, depthM = 0.4, floorY = 0.0, topY = 1.8) as ArMeasureResult.Ok
        assertEquals(1.0, ok.measurement.widthM, eps)
        assertEquals(0.0, ok.measurement.rotationDeg, eps)
        assertEquals(0.0, ok.measurement.mapX, eps)
        assertEquals(-2.2, ok.measurement.mapZ, eps) // centro fica atrás da frente
        assertEquals(1.8, ok.measurement.heightM!!, eps)

        val swapped = measureShelf(reference, right, left, device, depthM = 0.4, floorY = 0.0, topY = null) as ArMeasureResult.Ok
        assertEquals(0.0, swapped.measurement.rotationDeg, eps)
        assertNull(swapped.measurement.heightM)

        // A frente da estante, no mapa, aponta para onde a pessoa estava.
        val box = ShelfBox(1, "A", ok.measurement.mapX, ok.measurement.mapZ, ok.measurement.rotationDeg, 1.0, 0.4, 1.8, 5, 1, false)
        val frontCenter = box.local(0.0, 0.0, 0.2)
        assertEquals(-2.0, frontCenter.z, eps)
    }

    @Test
    fun arReferenceRotatesAndTranslatesWithoutMirroring() {
        val a = Vec3(2.0, 0.0, 3.0)
        val b = Vec3(2.0, 0.0, 1.0) // eixo X da planta aponta para -Z do mundo
        val reference = MapReference.fromPoints(a, b)!!
        val p = reference.toMap(Vec3(2.0, 0.0, 2.0))
        assertEquals(1.0, p.x, eps)
        assertEquals(0.0, p.z, eps)
        // Distâncias preservadas e orientação mantida (rotação, não reflexão).
        val q = reference.toMap(Vec3(3.0, 0.0, 3.0))
        assertEquals(1.0, q.length(), eps)
        assertTrue(abs(q.z - 1.0) < eps)
        assertNull(MapReference.fromPoints(a, a + Vec3(0.1, 0.0, 0.0)))
    }

    @Test
    fun arMeasurementRejectsDegenerateCorners() {
        val reference = MapReference.fromPoints(Vec3.ZERO, Vec3(1.0, 0.0, 0.0))!!
        val result = measureShelf(reference, Vec3(0.0, 0.0, -1.0), Vec3(0.05, 0.0, -1.0), Vec3.ZERO, 0.35, 0.0, null)
        assertTrue(result is ArMeasureResult.Error)
    }

    @Test
    fun rayHitsFloorOnlyInFront() {
        val hit = intersectHorizontalPlane(Vec3(0.0, 1.5, 0.0), Vec3(0.0, -1.0, -1.0), 0.0)
        assertNotNull(hit)
        assertEquals(-1.5, hit!!.z, eps)
        assertNull(intersectHorizontalPlane(Vec3(0.0, 1.5, 0.0), Vec3(0.0, 1.0, -1.0), 0.0))
    }

    @Test
    fun convexPolygonHitTest() {
        val square = listOf(0.0 to 0.0, 10.0 to 0.0, 10.0 to 10.0, 0.0 to 10.0)
        assertTrue(pointInConvexPolygon(5.0, 5.0, square))
        assertTrue(pointInConvexPolygon(5.0, 5.0, square.reversed()))
        assertFalse(pointInConvexPolygon(15.0, 5.0, square))
    }

    @Test
    fun frontFaceIsVisibleFromTheFrontAndTapPicksTheNearestShelf() {
        val near = ShelfBox(1, "Perto", 0.0, 0.0, 0.0, 1.0, 0.4, 2.0, 5, 1, false)
        val far = ShelfBox(2, "Longe", 0.0, -3.0, 0.0, 1.0, 0.4, 2.0, 5, 1, false)
        val camera = OrbitCamera(target = Vec3(0.0, 1.0, 0.0), yawDeg = 0.0, pitchDeg = 5.0, distance = 6.0)
        val faces = visibleFaces(listOf(near, far), camera)
        assertTrue(faces.any { it.box.shelfId == 1 && it.kind == FaceKind.FRONT })
        assertFalse(faces.any { it.kind == FaceKind.BACK })
        assertTrue("ordenado do mais distante ao mais próximo", faces.zipWithNext().all { (a, b) -> a.distance >= b.distance })
        val center = project(Vec3(0.0, 1.0, 0.2), camera, 1000.0, 1000.0)!!
        assertEquals(1, pickShelf(listOf(near, far), camera, 1000.0, 1000.0, center.x, center.y))
        assertNull(pickShelf(listOf(near, far), camera, 1000.0, 1000.0, 5.0, 5.0))
    }

    @Test
    fun fitCameraCentersOnAllShelves() {
        val boxes = layoutShelves(listOf(
            ShelfEntity(id = 1, name = "A", mapX = 0.0, mapZ = 0.0),
            ShelfEntity(id = 2, name = "B", mapX = 10.0, mapZ = 4.0)
        ))
        val camera = fitCamera(boxes)
        assertEquals(5.0, camera.target.x, 0.3)
        assertEquals(2.0, camera.target.z, 0.3)
        boxes.forEach { b -> assertNotNull(project(b.topCenter(), camera, 1000.0, 1000.0)) }
    }
}

package com.example.ar

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.AppDatabase
import com.example.data.LibraryRepository
import com.example.data.ShelfEntity
import com.example.map.ArMeasureResult
import com.example.map.MapReference
import com.example.map.Vec3
import com.example.map.intersectHorizontalPlane
import com.example.map.measureShelf
import com.example.ui.theme.MyApplicationTheme
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Camera
import com.google.ar.core.Config
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** Estado da sessão AR mostrado na tela. */
sealed interface ArStatus {
    data object NeedsPermission : ArStatus
    data object Preparing : ArStatus
    data object Ready : ArStatus
    data class Failed(val message: String) : ArStatus
}

/** Snapshot de cada quadro, publicado pela thread GL. */
data class ArFrameState(
    val tracking: Boolean = false,
    val trackingHint: String? = null,
    val floorY: Double? = null,
    val markers: List<Pair<String, Offset>> = emptyList()
)

enum class HitMode { FLOOR, SURFACE }

data class HitOutcome(val point: Vec3?, val devicePosition: Vec3, val error: String?)

private class HitRequest(val mode: HitMode, val callback: (HitOutcome) -> Unit)

/**
 * Varredura da biblioteca com ARCore (pensada para celulares como o Galaxy S24 FE, que não têm
 * LiDAR/ToF: a profundidade vem do movimento da câmera). Em vez de gerar uma malha bruta, a pessoa
 * marca os cantos de cada estante e o app grava posição, largura, altura e orientação na planta.
 */
class LibraryScanActivity : ComponentActivity() {
    private lateinit var surfaceView: GLSurfaceView
    private lateinit var repository: LibraryRepository

    @Volatile private var session: Session? = null
    private val depthEnabled = mutableStateOf(false)
    private var arRunning = false
    @Volatile private var markerPoints: List<Pair<String, Vec3>> = emptyList()
    private var installRequested = false

    private val status = mutableStateOf<ArStatus>(ArStatus.Preparing)
    private val frameState = MutableStateFlow(ArFrameState())
    private val pendingHit = AtomicReference<HitRequest?>(null)
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = LibraryRepository(AppDatabase.getDatabase(this))
        surfaceView = GLSurfaceView(this).apply {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setRenderer(ScanRenderer())
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            setWillNotDraw(false)
        }
        if (!hasCameraPermission()) status.value = ArStatus.NeedsPermission

        setContent {
            MyApplicationTheme {
                val currentStatus by status
                ScanScreen(
                    status = currentStatus,
                    frameState = frameState,
                    surfaceView = surfaceView,
                    shelvesFlowOwner = repository,
                    depthEnabled = depthEnabled.value,
                    onPermissionResult = { granted ->
                        if (granted) {
                            status.value = ArStatus.Preparing
                            startAr()
                        } else {
                            status.value = ArStatus.Failed("Sem acesso à câmera não é possível escanear. Você ainda pode posicionar as estantes manualmente no mapa 3D.")
                        }
                    },
                    onMarkersChanged = { markerPoints = it },
                    requestHit = { mode, callback -> pendingHit.set(HitRequest(mode, callback)) },
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission()) startAr()
    }

    override fun onPause() {
        super.onPause()
        if (arRunning) {
            surfaceView.onPause()
            session?.pause()
            arRunning = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        session?.close()
        session = null
    }

    private fun hasCameraPermission() =
        checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun startAr() {
        if (arRunning) return
        val s = session ?: createSession() ?: return
        try {
            s.resume()
        } catch (e: CameraNotAvailableException) {
            status.value = ArStatus.Failed("A câmera está em uso por outro app. Feche-o e tente novamente.")
            s.close()
            session = null
            return
        }
        surfaceView.onResume()
        arRunning = true
        status.value = ArStatus.Ready
    }

    private fun createSession(): Session? {
        return try {
            when (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    installRequested = true
                    status.value = ArStatus.Preparing
                    return null
                }
                else -> Unit
            }
            val s = Session(this)
            val config = Config(s).apply {
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                focusMode = Config.FocusMode.AUTO
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                depthMode = if (s.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
            }
            s.configure(config)
            depthEnabled.value = config.depthMode == Config.DepthMode.AUTOMATIC
            session = s
            s
        } catch (e: Exception) {
            status.value = ArStatus.Failed(
                when (e) {
                    is UnavailableUserDeclinedInstallationException -> "Instale o “Google Play Services para RA” para escanear a biblioteca."
                    is UnavailableArcoreNotInstalledException -> "O “Google Play Services para RA” não está instalado."
                    is UnavailableApkTooOldException -> "Atualize o “Google Play Services para RA” na Play Store."
                    is UnavailableSdkTooOldException -> "Esta versão do EducLib é antiga para o ARCore instalado. Atualize o app."
                    is UnavailableDeviceNotCompatibleException -> "Este aparelho não é compatível com ARCore. Use o editor de medidas do mapa 3D."
                    else -> "Não foi possível iniciar a realidade aumentada: ${e.message ?: e.javaClass.simpleName}"
                }
            )
            null
        }
    }

    private fun displayRotation(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display?.rotation ?: 0
        else @Suppress("DEPRECATION") windowManager.defaultDisplay.rotation

    private inner class ScanRenderer : GLSurfaceView.Renderer {
        private val background = ArBackgroundRenderer()
        private var textureSession: Session? = null
        private var viewportWidth = 1
        private var viewportHeight = 1
        private var geometryChanged = true
        private val view = FloatArray(16)
        private val projection = FloatArray(16)
        private val viewProjection = FloatArray(16)
        private val clip = FloatArray(4)

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0.08f, 0.08f, 0.08f, 1f)
            background.createOnGlThread()
            textureSession = null
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            GLES20.glViewport(0, 0, width, height)
            viewportWidth = width
            viewportHeight = height
            geometryChanged = true
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            val s = session ?: return
            if (textureSession !== s) {
                s.setCameraTextureName(background.textureId)
                textureSession = s
                geometryChanged = true
            }
            if (geometryChanged) {
                s.setDisplayGeometry(displayRotation(), viewportWidth, viewportHeight)
                geometryChanged = false
            }
            val frame = try {
                s.update()
            } catch (e: Exception) {
                return // sessão pausada ou câmera indisponível neste quadro
            }
            background.draw(frame)

            val camera = frame.camera
            val tracking = camera.trackingState == TrackingState.TRACKING
            val deviceY = camera.pose.ty()
            val floorY = if (!tracking) null else s.getAllTrackables(Plane::class.java)
                .filter {
                    it.trackingState == TrackingState.TRACKING && it.subsumedBy == null &&
                        it.type == Plane.Type.HORIZONTAL_UPWARD_FACING && it.centerPose.ty() < deviceY - 0.4f
                }
                .minOfOrNull { it.centerPose.ty() }
                ?.toDouble()

            pendingHit.getAndSet(null)?.let { request ->
                val outcome = if (tracking) hit(frame, camera, floorY, request.mode)
                else HitOutcome(null, Vec3.ZERO, "Rastreamento perdido. Mova o aparelho devagar.")
                mainHandler.post { request.callback(outcome) }
            }

            val markers = if (tracking) projectMarkers(camera) else emptyList()
            frameState.value = ArFrameState(
                tracking = tracking,
                trackingHint = if (tracking) null else trackingHint(camera.trackingFailureReason),
                floorY = floorY,
                markers = markers
            )
        }

        private fun hit(frame: Frame, camera: Camera, floorY: Double?, mode: HitMode): HitOutcome {
            val pose = camera.pose
            val device = Vec3(pose.tx().toDouble(), pose.ty().toDouble(), pose.tz().toDouble())
            val cx = viewportWidth / 2f
            val cy = viewportHeight / 2f
            val hits = frame.hitTest(cx, cy)
            return when (mode) {
                HitMode.FLOOR -> {
                    val y = floorY ?: return HitOutcome(null, device, "O piso ainda não foi detectado. Aponte para o chão e mova o aparelho devagar.")
                    val planeHit = hits.firstOrNull {
                        val t = it.trackable
                        t is Plane && t.type == Plane.Type.HORIZONTAL_UPWARD_FACING && t.isPoseInPolygon(it.hitPose) &&
                            kotlin.math.abs(it.hitPose.ty() - y) < 0.15
                    }
                    val point = if (planeHit != null) {
                        Vec3(planeHit.hitPose.tx().toDouble(), y, planeHit.hitPose.tz().toDouble())
                    } else {
                        // Fora do polígono detectado: prolonga o piso pelo raio do centro da tela.
                        val z = pose.zAxis
                        intersectHorizontalPlane(device, Vec3(-z[0].toDouble(), -z[1].toDouble(), -z[2].toDouble()), y)
                    }
                    if (point == null) HitOutcome(null, device, "Incline o aparelho para baixo, mirando o piso.")
                    else HitOutcome(point, device, null)
                }
                HitMode.SURFACE -> {
                    val surface = hits.firstOrNull {
                        val t = it.trackable
                        (t is DepthPoint) ||
                            (t is Plane && t.isPoseInPolygon(it.hitPose)) ||
                            (t is Point && t.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
                    }
                    if (surface == null) {
                        HitOutcome(null, device, "Não encontrei uma superfície no centro da tela. Aproxime-se ou informe a altura manualmente.")
                    } else {
                        val p = surface.hitPose
                        HitOutcome(Vec3(p.tx().toDouble(), p.ty().toDouble(), p.tz().toDouble()), device, null)
                    }
                }
            }
        }

        private fun projectMarkers(camera: Camera): List<Pair<String, Offset>> {
            val points = markerPoints
            if (points.isEmpty()) return emptyList()
            camera.getViewMatrix(view, 0)
            camera.getProjectionMatrix(projection, 0, 0.05f, 100f)
            Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0)
            return points.mapNotNull { (label, p) ->
                Matrix.multiplyMV(clip, 0, viewProjection, 0, floatArrayOf(p.x.toFloat(), p.y.toFloat(), p.z.toFloat(), 1f), 0)
                if (clip[3] <= 0f) return@mapNotNull null
                val nx = clip[0] / clip[3]
                val ny = clip[1] / clip[3]
                label to Offset((nx + 1f) / 2f * viewportWidth, (1f - ny) / 2f * viewportHeight)
            }
        }
    }

    private fun trackingHint(reason: TrackingFailureReason): String = when (reason) {
        TrackingFailureReason.INSUFFICIENT_LIGHT -> "Ambiente escuro: acenda as luzes."
        TrackingFailureReason.EXCESSIVE_MOTION -> "Movimento rápido demais: mova o aparelho mais devagar."
        TrackingFailureReason.INSUFFICIENT_FEATURES -> "Aponte para áreas com mais detalhes (piso, lombadas, móveis)."
        TrackingFailureReason.CAMERA_UNAVAILABLE -> "A câmera está indisponível no momento."
        else -> "Iniciando o rastreamento: mova o aparelho lentamente."
    }
}

private enum class ScanStep { REF_A, REF_B, CHOOSE_SHELF, LEFT, RIGHT, TOP, REVIEW }

private fun Double.br(decimals: Int = 2) = String.format(Locale("pt", "BR"), "%.${decimals}f", this)
private fun String.toDecimal() = trim().replace(',', '.').toDoubleOrNull()

@Composable
private fun ScanScreen(
    status: ArStatus,
    frameState: StateFlow<ArFrameState>,
    surfaceView: GLSurfaceView,
    shelvesFlowOwner: LibraryRepository,
    depthEnabled: Boolean,
    onPermissionResult: (Boolean) -> Unit,
    onMarkersChanged: (List<Pair<String, Vec3>>) -> Unit,
    requestHit: (HitMode, (HitOutcome) -> Unit) -> Unit,
    onClose: () -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onPermissionResult)

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        when (status) {
            ArStatus.NeedsPermission -> CenteredMessage(
                title = "Escanear a biblioteca",
                body = "O EducLib usa a câmera, só neste aparelho, para medir onde ficam as estantes. Nenhuma imagem é gravada ou enviada.",
                action = "Permitir câmera",
                onAction = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onClose = onClose
            )
            ArStatus.Preparing -> CenteredMessage("Preparando a realidade aumentada…", "Se for solicitado, instale o “Google Play Services para RA”.", null, {}, onClose)
            is ArStatus.Failed -> CenteredMessage("Não foi possível escanear", status.message, null, {}, onClose)
            ArStatus.Ready -> ScanWorkflow(frameState, surfaceView, shelvesFlowOwner, depthEnabled, onMarkersChanged, requestHit, onClose)
        }
    }
}

@Composable
private fun CenteredMessage(title: String, body: String, action: String?, onAction: () -> Unit, onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        if (action != null) Button(onClick = onAction) { Text(action) }
        TextButton(onClick = onClose) { Text("Voltar") }
    }
}

@Composable
private fun ScanWorkflow(
    frameStateFlow: StateFlow<ArFrameState>,
    surfaceView: GLSurfaceView,
    repository: LibraryRepository,
    depthEnabled: Boolean,
    onMarkersChanged: (List<Pair<String, Vec3>>) -> Unit,
    requestHit: (HitMode, (HitOutcome) -> Unit) -> Unit,
    onClose: () -> Unit
) {
    val frame by frameStateFlow.collectAsState()
    val shelves by repository.allShelves.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(ScanStep.REF_A) }
    var refA by remember { mutableStateOf<Vec3?>(null) }
    var reference by remember { mutableStateOf<MapReference?>(null) }
    var shelf by remember { mutableStateOf<ShelfEntity?>(null) }
    var left by remember { mutableStateOf<Vec3?>(null) }
    var right by remember { mutableStateOf<Vec3?>(null) }
    var device by remember { mutableStateOf<Vec3?>(null) }
    var topY by remember { mutableStateOf<Double?>(null) }
    var floorAtMark by remember { mutableStateOf(0.0) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var savedCount by remember { mutableIntStateOf(0) }
    val measuredIds = remember { mutableStateOf(setOf<Int>()) }

    fun publishMarkers() {
        onMarkersChanged(listOfNotNull(
            refA?.let { "A" to it },
            left?.let { "E" to it },
            right?.let { "D" to it }
        ))
    }

    fun mark(mode: HitMode, onPoint: (Vec3, HitOutcome) -> Unit) {
        busy = true
        message = null
        requestHit(mode) { outcome ->
            busy = false
            if (outcome.point == null) message = outcome.error else onPoint(outcome.point, outcome)
            publishMarkers()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { surfaceView }, modifier = Modifier.fillMaxSize())

        // Retícula central e marcadores projetados.
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            drawCircle(Color.White, radius = 22f, center = c, style = Stroke(width = 3f))
            drawLine(Color.White, c - Offset(34f, 0f), c - Offset(12f, 0f), strokeWidth = 3f)
            drawLine(Color.White, c + Offset(12f, 0f), c + Offset(34f, 0f), strokeWidth = 3f)
            drawLine(Color.White, c - Offset(0f, 34f), c - Offset(0f, 12f), strokeWidth = 3f)
            drawLine(Color.White, c + Offset(0f, 12f), c + Offset(0f, 34f), strokeWidth = 3f)
            val e = frame.markers.firstOrNull { it.first == "E" }?.second
            val d = frame.markers.firstOrNull { it.first == "D" }?.second
            if (e != null && d != null) drawLine(Color(0xFFE0703A), e, d, strokeWidth = 6f)
            frame.markers.forEach { (label, pos) ->
                drawCircle(if (label == "A") Color(0xFF5C7D70) else Color(0xFFE0703A), radius = 16f, center = pos)
                drawCircle(Color.White, radius = 6f, center = pos)
            }
        }

        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text("Escanear biblioteca", color = Color.White, fontWeight = FontWeight.Bold)
                    val chips = listOfNotNull(
                        if (frame.tracking) "rastreando" else "procurando…",
                        if (frame.floorY != null) "piso detectado" else "sem piso",
                        if (depthEnabled) "profundidade ativa" else null,
                        if (savedCount > 0) "$savedCount salvas" else null
                    )
                    Text(chips.joinToString(" · "), color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                }
            }
            frame.trackingHint?.let {
                Text(it, color = Color.White, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().background(Color(0xCC7C5057)).padding(10.dp))
            }

            Spacer(Modifier.weight(1f))

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val shelfName = shelf?.name ?: ""
                when (step) {
                    ScanStep.REF_A, ScanStep.REF_B -> {
                        Text(
                            if (step == ScanStep.REF_A) "Referência 1 de 2" else "Referência 2 de 2",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (step == ScanStep.REF_A) "Mire o centro da tela num ponto fixo do piso que você consiga reencontrar — por exemplo, o lado esquerdo da porta de entrada — e toque em Marcar."
                            else "Agora outro ponto fixo do piso, a pelo menos 30 cm do primeiro — por exemplo, o lado direito da porta. Use sempre os mesmos dois pontos para que novas varreduras caibam no mesmo mapa.",
                            fontSize = 13.sp
                        )
                        Button(
                            onClick = {
                                mark(HitMode.FLOOR) { p, _ ->
                                    if (step == ScanStep.REF_A) {
                                        refA = p
                                        step = ScanStep.REF_B
                                    } else {
                                        val ref = MapReference.fromPoints(refA!!, p)
                                        if (ref == null) message = "Os pontos de referência estão muito próximos. Afaste-se pelo menos 30 cm."
                                        else {
                                            reference = ref
                                            refA = null
                                            step = ScanStep.CHOOSE_SHELF
                                        }
                                    }
                                }
                            },
                            enabled = frame.tracking && frame.floorY != null && !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Marcar ponto") }
                    }

                    ScanStep.CHOOSE_SHELF -> {
                        Text("Qual estante vai medir agora?", fontWeight = FontWeight.Bold)
                        if (shelves.isEmpty()) Text("Nenhuma estante cadastrada ainda. Crie uma abaixo.", fontSize = 13.sp)
                        shelves.forEach { s ->
                            val done = s.id in measuredIds.value
                            Text(
                                (if (done) "✓ " else if (s.layoutSource == "ar") "↻ " else "• ") + s.name,
                                fontSize = 15.sp,
                                fontWeight = if (done) FontWeight.Normal else FontWeight.Medium,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    shelf = s
                                    step = ScanStep.LEFT
                                }.padding(vertical = 8.dp)
                            )
                        }
                        var newName by remember { mutableStateOf("") }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("Nova estante") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                enabled = newName.isNotBlank() && !busy,
                                onClick = {
                                    val name = newName.trim()
                                    busy = true
                                    scope.launch {
                                        val id = repository.insertShelf(ShelfEntity(name = name)).toInt()
                                        shelf = ShelfEntity(id = id, name = name)
                                        newName = ""
                                        busy = false
                                        step = ScanStep.LEFT
                                    }
                                }
                            ) { Text("Criar") }
                        }
                        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Concluir varredura") }
                    }

                    ScanStep.LEFT, ScanStep.RIGHT -> {
                        Text("Estante “$shelfName”", fontWeight = FontWeight.Bold)
                        Text(
                            if (step == ScanStep.LEFT) "De frente para a estante, mire o canto inferior ESQUERDO, onde ela encosta no piso, e toque em Marcar."
                            else "Agora mire o canto inferior DIREITO, também rente ao piso.",
                            fontSize = 13.sp
                        )
                        Button(
                            onClick = {
                                mark(HitMode.FLOOR) { p, outcome ->
                                    floorAtMark = frame.floorY ?: p.y
                                    if (step == ScanStep.LEFT) {
                                        left = p
                                        step = ScanStep.RIGHT
                                    } else {
                                        right = p
                                        device = outcome.devicePosition
                                        step = ScanStep.TOP
                                    }
                                }
                            },
                            enabled = frame.tracking && frame.floorY != null && !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Marcar canto") }
                        TextButton(onClick = {
                            left = null; right = null; step = ScanStep.CHOOSE_SHELF; publishMarkers()
                        }) { Text("Escolher outra estante") }
                    }

                    ScanStep.TOP -> {
                        Text("Altura de “$shelfName”", fontWeight = FontWeight.Bold)
                        Text("Mire a borda superior da frente da estante e toque em Medir. Se não conseguir, pule e digite a altura na próxima tela.", fontSize = 13.sp)
                        Button(
                            onClick = {
                                mark(HitMode.SURFACE) { p, _ ->
                                    topY = p.y
                                    step = ScanStep.REVIEW
                                }
                            },
                            enabled = frame.tracking && !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Medir altura") }
                        TextButton(onClick = { topY = null; step = ScanStep.REVIEW }) { Text("Pular") }
                    }

                    ScanStep.REVIEW -> {
                        val target = shelf
                        val ref = reference
                        if (target == null || ref == null || left == null || right == null || device == null) {
                            LaunchedEffect(Unit) { step = ScanStep.CHOOSE_SHELF }
                        } else {
                            var depthText by remember(target.id) { mutableStateOf(target.depthM.br()) }
                            val preview = measureShelf(ref, left!!, right!!, device!!, depthText.toDecimal() ?: target.depthM, floorAtMark, topY)
                            var heightText by remember(target.id, topY) {
                                mutableStateOf(((preview as? ArMeasureResult.Ok)?.measurement?.heightM ?: target.heightM).br())
                            }
                            var levelsText by remember(target.id) { mutableStateOf(target.levelCount.toString()) }
                            var columnsText by remember(target.id) { mutableStateOf(target.columnCount.toString()) }
                            Text("Conferir “$shelfName”", fontWeight = FontWeight.Bold)
                            when (preview) {
                                is ArMeasureResult.Error -> Text(preview.message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                                is ArMeasureResult.Ok -> Text(
                                    "Largura medida: ${preview.measurement.widthM.br()} m" +
                                        (preview.measurement.heightM?.let { " · altura medida: ${it.br()} m" } ?: ""),
                                    fontSize = 13.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(depthText, { depthText = it }, label = { Text("Profund. (m)") }, singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                                OutlinedTextField(heightText, { heightText = it }, label = { Text("Altura (m)") }, singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(levelsText, { levelsText = it }, label = { Text("Prateleiras") }, singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(columnsText, { columnsText = it }, label = { Text("Colunas") }, singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { left = null; right = null; topY = null; step = ScanStep.LEFT; publishMarkers() },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Refazer") }
                                Button(
                                    enabled = preview is ArMeasureResult.Ok && !busy,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val depth = depthText.toDecimal()?.takeIf { it in 0.05..ShelfEntity.MAX_DIMENSION_M }
                                        val height = heightText.toDecimal()?.takeIf { it in 0.1..ShelfEntity.MAX_DIMENSION_M }
                                        val levels = levelsText.trim().toIntOrNull()?.takeIf { it in 1..ShelfEntity.MAX_LEVELS }
                                        val columns = columnsText.trim().toIntOrNull()?.takeIf { it in 1..ShelfEntity.MAX_COLUMNS }
                                        if (depth == null || height == null || levels == null || columns == null) {
                                            message = "Confira os valores: profundidade e altura em metros, prateleiras de 1 a ${ShelfEntity.MAX_LEVELS}, colunas de 1 a ${ShelfEntity.MAX_COLUMNS}."
                                            return@Button
                                        }
                                        val result = measureShelf(ref, left!!, right!!, device!!, depth, floorAtMark, null)
                                        if (result !is ArMeasureResult.Ok) return@Button
                                        val m = result.measurement
                                        busy = true
                                        scope.launch {
                                            // Relê a estante para não sobrescrever um nome editado durante a varredura.
                                            val current = repository.getShelfById(target.id) ?: target
                                            repository.updateShelf(
                                                current.copy(
                                                    mapX = m.mapX, mapZ = m.mapZ, rotationDeg = m.rotationDeg,
                                                    widthM = m.widthM, depthM = depth, heightM = height,
                                                    levelCount = levels, columnCount = columns, layoutSource = "ar"
                                                )
                                            )
                                            measuredIds.value = measuredIds.value + target.id
                                            savedCount++
                                            left = null; right = null; device = null; topY = null; shelf = null
                                            busy = false
                                            message = "“${current.name}” salva no mapa."
                                            step = ScanStep.CHOOSE_SHELF
                                            publishMarkers()
                                        }
                                    }
                                ) { Text("Salvar estante") }
                            }
                        }
                    }
                }
                message?.let { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

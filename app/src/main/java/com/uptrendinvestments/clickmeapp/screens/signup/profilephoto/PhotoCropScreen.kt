package com.uptrendinvestments.clickmeapp.screens.signup.profilephoto

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

const val CROP_CANVAS_TAG = "crop_canvas"

private val MinGuideDiameter = 240.dp
private val ChromeReserve = 224.dp
private val RingWidth = 2.dp
private val SpinnerSize = 24.dp
private val SpinnerStroke = 2.dp
private const val SCRIM_ALPHA = 0.6f
private const val SPRING_BACK_MS = 200

/** Hilt-aware entry for the crop step (D-12). [onDone] fires after the crop was stored; [onCancel] discards. */
@Composable
fun PhotoCropScreen(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    viewModel: PhotoCropViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()

    LaunchedEffect(done) {
        if (done) {
            onDone()
            viewModel.onDoneConsumed()
        }
    }

    PhotoCropContent(
        state = state,
        onGuideMeasured = viewModel::onGuideMeasured,
        onGesture = viewModel::onGesture,
        onGestureEnd = viewModel::onGestureEnd,
        onDoubleTap = viewModel::onDoubleTap,
        onZoomIn = viewModel::zoomIn,
        onZoomOut = viewModel::zoomOut,
        onMove = viewModel::move,
        onUse = viewModel::useCrop,
        onCancel = onCancel,
        onDismissDialog = viewModel::dismissDialog,
    )
}

/** Stateless crop screen: circular guide over a pan/zoom canvas, hint, and Cancel / Use Photo buttons. */
@Composable
fun PhotoCropContent(
    state: PhotoCropUiState,
    onGuideMeasured: (Float) -> Unit,
    onGesture: (Offset, Float, Offset) -> Unit,
    onGestureEnd: () -> Unit,
    onDoubleTap: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onUse: () -> Unit,
    onCancel: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val diameter = min(maxWidth - Spacing.lg * 2, maxHeight - ChromeReserve).coerceAtLeast(MinGuideDiameter)
            val canvasHeight = max(diameter + Spacing.lg * 2, maxHeight - ChromeReserve)
            val radiusPx = with(LocalDensity.current) { diameter.toPx() / 2f }
            LaunchedEffect(radiusPx) { onGuideMeasured(radiusPx) }

            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                CropCanvas(
                    state = state,
                    radiusPx = radiusPx,
                    onGesture = onGesture,
                    onGestureEnd = onGestureEnd,
                    onDoubleTap = onDoubleTap,
                    onZoomIn = onZoomIn,
                    onZoomOut = onZoomOut,
                    onMove = onMove,
                    modifier = Modifier.fillMaxWidth().height(canvasHeight),
                )
                Spacer(Modifier.height(Spacing.xl))
                Text(
                    text = stringResource(R.string.signup_crop_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.extras.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                )
                Spacer(Modifier.height(Spacing.lg))
                CropButtons(state = state, onUse = onUse, onCancel = onCancel)
            }
        }
    }

    state.dialogError?.let { error ->
        AuthErrorDialog(
            message = error.asString(),
            onDismiss = if (state.loadFailed) onCancel else onDismissDialog,
            title =
                stringResource(
                    if (state.loadFailed) R.string.signup_photo_open_error_title else R.string.auth_dialog_title,
                ),
        )
    }
}

@Composable
private fun CropButtons(
    state: PhotoCropUiState,
    onUse: () -> Unit,
    onCancel: () -> Unit,
) {
    val canUse = state.source != null && !state.isLoading && !state.isCropping
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PressScaleButton(
            onClick = onCancel,
            enabled = !state.isCropping,
            colors = PressScaleButtonDefaults.tonalColors(),
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.signup_crop_cancel))
        }
        PressScaleButton(
            onClick = onUse,
            enabled = canUse,
            colors = PressScaleButtonDefaults.filledColors(),
            modifier = Modifier.weight(1f),
        ) {
            if (state.isCropping) {
                CircularProgressIndicator(
                    modifier = Modifier.size(SpinnerSize),
                    color = LocalContentColor.current,
                    strokeWidth = SpinnerStroke,
                )
            } else {
                Text(stringResource(R.string.signup_crop_use))
            }
        }
    }
}

@Composable
private fun CropCanvas(
    state: PhotoCropUiState,
    radiusPx: Float,
    onGesture: (Offset, Float, Offset) -> Unit,
    onGestureEnd: () -> Unit,
    onDoubleTap: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onMove: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bitmap = state.source
    val ready = bitmap != null && state.transform.scale > 0f
    var dragging by remember { mutableStateOf(false) }

    val offsetSpec = if (dragging) snap<Offset>() else tween(SPRING_BACK_MS, easing = FastOutSlowInEasing)
    val scaleSpec = if (dragging) snap<Float>() else tween(SPRING_BACK_MS, easing = FastOutSlowInEasing)
    val animatedOffset by animateOffsetAsState(state.transform.offset, offsetSpec, label = "cropOffset")
    val animatedScale by animateFloatAsState(state.transform.scale, scaleSpec, label = "cropScale")

    val scrim = MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)
    val ring = MaterialTheme.extras.overlayWhite80

    val areaDescription = stringResource(R.string.signup_crop_area_a11y)
    val zoomInLabel = stringResource(R.string.signup_crop_zoom_in)
    val zoomOutLabel = stringResource(R.string.signup_crop_zoom_out)
    val moveLeftLabel = stringResource(R.string.signup_crop_move_left)
    val moveRightLabel = stringResource(R.string.signup_crop_move_right)
    val moveUpLabel = stringResource(R.string.signup_crop_move_up)
    val moveDownLabel = stringResource(R.string.signup_crop_move_down)

    val currentOnGesture by rememberUpdatedState(onGesture)
    val currentOnGestureEnd by rememberUpdatedState(onGestureEnd)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)

    val gestureModifier =
        if (ready) {
            Modifier
                .pointerInput(Unit) {
                    detectCropTransforms(
                        onDraggingChange = { dragging = it },
                        onGesture = { centroid, zoom, pan -> currentOnGesture(centroid, zoom, pan) },
                        onEnd = { currentOnGestureEnd() },
                    )
                }.pointerInput(Unit) { detectTapGestures(onDoubleTap = { currentOnDoubleTap() }) }
        } else {
            Modifier
        }

    Box(
        modifier =
            modifier
                .testTag(CROP_CANVAS_TAG)
                .semantics {
                    contentDescription = areaDescription
                    if (ready) {
                        customActions =
                            listOf(
                                CustomAccessibilityAction(zoomInLabel) {
                                    onZoomIn()
                                    true
                                },
                                CustomAccessibilityAction(zoomOutLabel) {
                                    onZoomOut()
                                    true
                                },
                                CustomAccessibilityAction(moveLeftLabel) {
                                    onMove(-1, 0)
                                    true
                                },
                                CustomAccessibilityAction(moveRightLabel) {
                                    onMove(1, 0)
                                    true
                                },
                                CustomAccessibilityAction(moveUpLabel) {
                                    onMove(0, -1)
                                    true
                                },
                                CustomAccessibilityAction(moveDownLabel) {
                                    onMove(0, 1)
                                    true
                                },
                            )
                    }
                }.then(gestureModifier),
        contentAlignment = Alignment.Center,
    ) {
        if (ready) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
                val image = bitmap
                val topLeft =
                    Offset(
                        center.x + animatedOffset.x - image.width * animatedScale / 2f,
                        center.y + animatedOffset.y - image.height * animatedScale / 2f,
                    )
                withTransform({
                    translate(topLeft.x, topLeft.y)
                    scale(animatedScale, animatedScale, pivot = Offset.Zero)
                }) {
                    drawImage(image)
                }
                drawRect(scrim)
                drawCircle(Color.Transparent, radius = radiusPx, center = center, blendMode = BlendMode.Clear)
                drawCircle(ring, radius = radiusPx, center = center, style = Stroke(width = RingWidth.toPx()))
            }
        }
        if (state.isLoading) CircularProgressIndicator()
    }
}

/** Pinch/pan recogniser with an end callback (detectTransformGestures has none), which the spring-back needs. */
private suspend fun PointerInputScope.detectCropTransforms(
    onDraggingChange: (Boolean) -> Unit,
    onGesture: (centroidFromCenter: Offset, zoom: Float, pan: Offset) -> Unit,
    onEnd: () -> Unit,
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        onDraggingChange(true)
        do {
            val event = awaitPointerEvent()
            val zoom = event.calculateZoom()
            val pan = event.calculatePan()
            val centroid = event.calculateCentroid(useCurrent = true)
            if ((zoom != 1f || pan != Offset.Zero) && centroid != Offset.Unspecified) {
                onGesture(centroid - center, zoom, pan)
            }
            event.changes.forEach { if (it.positionChanged()) it.consume() }
        } while (event.changes.any { it.pressed })
        onDraggingChange(false)
        onEnd()
    }
}

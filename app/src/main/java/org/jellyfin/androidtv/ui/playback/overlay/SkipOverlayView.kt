package org.jellyfin.androidtv.ui.playback.overlay

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.LumaFinPalette
import org.jellyfin.androidtv.ui.composable.rememberReducedMotion
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.playback.segment.MediaSegmentRepository
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The ask-to-skip prompt. It is styled like a focused LumaFin button because OK triggers it, with a bar
 * along the bottom that empties as the prompt times out.
 */
@Composable
fun SkipOverlayComposable(
	visible: Boolean,
	timeout: Duration = MediaSegmentRepository.AskToSkipAutoHideDuration,
) {
	val reducedMotion = rememberReducedMotion()
	Box(
		contentAlignment = Alignment.BottomEnd,
		modifier = Modifier
			.padding(48.dp, 48.dp)
	) {
		AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
			val remaining = remember { Animatable(1f) }
			LaunchedEffect(Unit) {
				if (!reducedMotion) remaining.animateTo(0f, tween(timeout.inWholeMilliseconds.toInt(), easing = LinearEasing))
			}
			val shape = RoundedCornerShape(12.dp)
			Box(
				modifier = Modifier
					.clip(shape)
					.background(LumaFinPalette.FocusedRow.copy(alpha = 0.92f))
					// The time-out bar, drawn just inside the outline so the button keeps its own size
					.drawBehind {
						if (!reducedMotion) {
							val inset = 2.dp.toPx()
							val barHeight = 4.dp.toPx()
							drawRect(
								color = LumaFinPalette.Cyan,
								topLeft = Offset(inset, size.height - inset - barHeight),
								size = Size((size.width - inset * 2) * remaining.value, barHeight),
							)
						}
					}
					.border(2.dp, LumaFinPalette.Cyan, shape)
			) {
				Row(
					modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
					horizontalArrangement = Arrangement.spacedBy(10.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Icon(
						imageVector = ImageVector.vectorResource(R.drawable.ic_fast_forward),
						contentDescription = null,
						tint = LumaFinPalette.Text,
						modifier = Modifier.size(22.dp),
					)
					Text(
						text = stringResource(R.string.segment_action_skip),
						color = LumaFinPalette.Text,
						fontSize = 18.sp,
						fontWeight = FontWeight.SemiBold,
					)
				}
			}
		}
	}
}

class SkipOverlayView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {
	private val _currentPosition = MutableStateFlow(Duration.ZERO)
	private val _targetPosition = MutableStateFlow<Duration?>(null)
	private val _skipUiEnabled = MutableStateFlow(true)

	var currentPosition: Duration
		get() = _currentPosition.value
		set(value) {
			_currentPosition.value = value
		}

	var currentPositionMs: Long
		get() = _currentPosition.value.inWholeMilliseconds
		set(value) {
			_currentPosition.value = value.milliseconds
		}

	var targetPosition: Duration?
		get() = _targetPosition.value
		set(value) {
			_targetPosition.value = value
		}

	var targetPositionMs: Long?
		get() = _targetPosition.value?.inWholeMilliseconds
		set(value) {
			_targetPosition.value = value?.milliseconds
		}

	var skipUiEnabled: Boolean
		get() = _skipUiEnabled.value
		set(value) {
			_skipUiEnabled.value = value
		}

	val visible: Boolean
		get() {
			val enabled = _skipUiEnabled.value
			val targetPosition = _targetPosition.value
			val currentPosition = _currentPosition.value

			return enabled && targetPosition != null && currentPosition <= (targetPosition - MediaSegmentRepository.SkipMinDuration)
		}

	@Composable
	override fun Content() {
		val skipUiEnabled by _skipUiEnabled.collectAsState()
		val currentPosition by _currentPosition.collectAsState()
		val targetPosition by _targetPosition.collectAsState()

		val visible by remember(skipUiEnabled, currentPosition, targetPosition) {
			derivedStateOf { visible }
		}

		// Auto hide
		LaunchedEffect(skipUiEnabled, targetPosition) {
			delay(MediaSegmentRepository.AskToSkipAutoHideDuration)
			_targetPosition.value = null
		}

		SkipOverlayComposable(visible)
	}
}

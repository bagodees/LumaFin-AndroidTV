package org.jellyfin.androidtv.ui.shared.toolbar

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import androidx.compose.foundation.Image
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.PrivateUser
import org.jellyfin.androidtv.auth.repository.AuthenticationRepository
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.ServerUserRepository
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.store.AuthenticationPreferences
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.base.button.ButtonBase
import org.jellyfin.androidtv.ui.base.button.ButtonDefaults
import org.jellyfin.androidtv.ui.base.dialog.DialogBase
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.navigation.ActivityDestinations
import org.koin.compose.koinInject

enum class UserSwitchResult { IDLE, SWITCHING, SWITCHED, SIGN_IN_REQUIRED, FAILED }

/** A picker owns one attempt at a time; opening and closing it never ends a session. */
class QuickUserSwitchController(
	private val sessions: SessionRepository,
	private val authenticationPreferences: AuthenticationPreferences,
) {
	private val _result = MutableStateFlow(UserSwitchResult.IDLE)
	val result = _result.asStateFlow()

	suspend fun switch(user: PrivateUser): Boolean {
		if (_result.value == UserSwitchResult.SWITCHING || _result.value == UserSwitchResult.SWITCHED) return false
		if (sessions.currentSession.value?.let { it.serverId == user.serverId && it.userId == user.id } == true) return false
		if (user.accessToken == null || authenticationPreferences[AuthenticationPreferences.alwaysAuthenticate]) {
			_result.value = UserSwitchResult.SIGN_IN_REQUIRED
			return false
		}
		_result.value = UserSwitchResult.SWITCHING
		try {
			_result.value = if (sessions.switchCurrentSession(user.serverId, user.id)) UserSwitchResult.SWITCHED else UserSwitchResult.FAILED
		} catch (err: CancellationException) {
			_result.value = UserSwitchResult.IDLE
			throw err
		} catch (_: Exception) {
			_result.value = UserSwitchResult.FAILED
		}
		return _result.value == UserSwitchResult.SWITCHED
	}
}

@Composable
fun QuickUserSwitcher(onDismissRequest: () -> Unit) {
	val sessions = koinInject<SessionRepository>()
	val servers = koinInject<ServerRepository>()
	val users = koinInject<ServerUserRepository>()
	val authentication = koinInject<AuthenticationRepository>()
	val preferences = koinInject<AuthenticationPreferences>()
	val activity = LocalActivity.current
	val scope = rememberCoroutineScope()
	val controller = remember { QuickUserSwitchController(sessions, preferences) }
	val result by controller.result.collectAsState()
	// Snapshot the old account while it is still active; the new activity owns the next home.
	val server = remember { servers.currentServer.value }
	val active = remember { sessions.currentSession.value }
	val savedUsers = remember {
		server?.let(users::getStoredServerUsers).orEmpty().sortedByDescending { it.id == active?.userId }
	}
	val busy = result == UserSwitchResult.SWITCHING || result == UserSwitchResult.SWITCHED
	val retryFocus = remember { FocusRequester() }
	LaunchedEffect(result) {
		if (result == UserSwitchResult.FAILED || result == UserSwitchResult.SIGN_IN_REQUIRED) retryFocus.requestFocus()
	}
	fun openStartup(signIn: Boolean, selectServer: Boolean = false) {
		if (signIn) {
			activity?.startActivity(ActivityDestinations.accountSelection(activity, selectServer))
			return
		}
		activity?.startActivity(ActivityDestinations.startup(activity, selectServer = selectServer))
		// MainActivity is singleTask: remove the old instance before startup can
		// launch the next home, so no old view models or back stack can be reused.
		activity?.finish()
	}
	DialogBase(visible = true, onDismissRequest = { if (!busy) onDismissRequest() }, scrimColor = Color(0xFF101419)) {
		Column(
			modifier = Modifier.fillMaxSize().background(Color(0xFF101419)).padding(vertical = 32.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center,
		) {
			Text(stringResource(R.string.user_switch_heading), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
			Spacer(Modifier.height(8.dp))
			Text(server?.name.orEmpty(), fontSize = 14.sp, color = Color(0xFFADB5C0))
			Spacer(Modifier.height(20.dp))
			LazyRow(
				modifier = Modifier.fillMaxWidth().focusRequester(retryFocus).focusGroup(),
				contentPadding = PaddingValues(horizontal = 48.dp, vertical = 12.dp),
				horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
			) {
				items(savedUsers.size, key = { savedUsers[it].id }) { index ->
					val user = savedUsers[index]
					val current = user.id == active?.userId
					UserProfileCard(
						name = user.name,
						imageUrl = server?.let { authentication.getUserImageUrl(it, user) },
						color = profileColors[Math.floorMod(user.id.hashCode(), profileColors.size)],
						current = current,
						enabled = !busy,
						onClick = {
							if (current) onDismissRequest()
							else scope.launch { if (controller.switch(user)) openStartup(false) }
						},
					)
				}
				item(key = "other-users") {
					UserProfileCard(name = stringResource(R.string.user_switch_other_users), imageUrl = null,
						color = Color(0xFF28303B), current = false, enabled = !busy, otherUsers = true,
						onClick = { openStartup(true) })
				}
			}
			Box(Modifier.height(54.dp).fillMaxWidth().padding(horizontal = 48.dp), contentAlignment = Alignment.Center) {
				if (result != UserSwitchResult.IDLE) Text(
					stringResource(when (result) {
						UserSwitchResult.SWITCHING, UserSwitchResult.SWITCHED -> R.string.user_switch_loading
						UserSwitchResult.SIGN_IN_REQUIRED -> R.string.user_switch_sign_in_required
						else -> R.string.user_switch_failed
					}), color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center,
				)
			}
			Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
				Button(onClick = { openStartup(true, selectServer = true) }, enabled = !busy) { Text(stringResource(R.string.user_switch_servers)) }
				Button(onClick = onDismissRequest, enabled = !busy) { Text(stringResource(android.R.string.cancel)) }
			}
		}
	}
}

private val profileColors = listOf(Color(0xFF2265A7), Color(0xFF7952A3), Color(0xFF257D78), Color(0xFFA65337), Color(0xFF9A6B1E))

@Composable
private fun UserProfileCard(
	name: String,
	imageUrl: String?,
	color: Color,
	current: Boolean,
	enabled: Boolean,
	otherUsers: Boolean = false,
	onClick: () -> Unit,
) {
	val interactions = remember { MutableInteractionSource() }
	val focused by interactions.collectIsFocusedAsState()
	val scale by animateFloatAsState(if (focused) 1.06f else 1f)
	val shape = RoundedCornerShape(12.dp)
	val painter = rememberAsyncImagePainter(imageUrl)
	val imageState by painter.state.collectAsState()
	val currentLabel = stringResource(R.string.user_switch_current)
	Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(136.dp)) {
		ButtonBase(
			onClick = onClick, enabled = enabled, interactionSource = interactions, shape = shape,
			colors = ButtonDefaults.colors(containerColor = color, focusedContainerColor = color, contentColor = Color.White,
				focusedContentColor = Color.White, disabledContainerColor = color.copy(alpha = 0.5f)),
			modifier = Modifier.size(128.dp).graphicsLayer { scaleX = scale; scaleY = scale }
				.border(3.dp, if (focused) Color.White else Color.Transparent, shape)
				.semantics { contentDescription = if (current) "$name, $currentLabel" else name },
		) {
			Box(Modifier.fillMaxSize().padding(4.dp).clip(RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
				if (!otherUsers && imageState is AsyncImagePainter.State.Success) {
					Image(painter, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
				} else {
					val initials = remember(name) { name.trim().split(Regex("\\s+")).take(2).joinToString("") { it.take(1).uppercase() } }
					Text(if (otherUsers) "+" else initials, color = Color.White, fontSize = if (otherUsers) 52.sp else 40.sp,
						fontWeight = FontWeight.Medium)
				}
			}
		}
		Spacer(Modifier.height(14.dp))
		Text(name, color = if (focused) Color.White else Color(0xFFBEC5CF), fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
			textAlign = TextAlign.Center, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
		Text(if (current) currentLabel else "", color = Color(0xFFADB5C0), fontSize = 12.sp)
	}
}

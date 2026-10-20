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
import androidx.compose.runtime.withFrameNanos
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
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.User
import org.jellyfin.androidtv.auth.model.ConnectionFailure
import org.jellyfin.androidtv.auth.model.classifyConnectionFailure
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
import org.jellyfin.androidtv.ui.base.LumaFinPalette
import org.jellyfin.androidtv.ui.base.rememberAccessibilityOptions
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
	private val _failure = MutableStateFlow<ConnectionFailure?>(null)
	val failure = _failure.asStateFlow()
	private var attemptedUser: PrivateUser? = null

	suspend fun retry(): Boolean {
		if (_failure.value?.retryable != true) return false
		return attemptedUser?.let { switch(it) } ?: false
	}

	suspend fun tryBackup(): Boolean {
		if (_failure.value != ConnectionFailure.UNREACHABLE || _result.value != UserSwitchResult.FAILED) return false
		val user = attemptedUser ?: return false
		_result.value = UserSwitchResult.SWITCHING
		try {
			when (val result = sessions.tryBackupAddress(user.serverId, user.id)) {
				is org.jellyfin.androidtv.auth.repository.BackupAddressResult.Saved -> {
					_failure.value = null
					_result.value = UserSwitchResult.SWITCHED
				}
				else -> {
					_failure.value = (result as? org.jellyfin.androidtv.auth.repository.BackupAddressResult.Unavailable)?.failure
						?: ConnectionFailure.ADDRESS
					_result.value = UserSwitchResult.FAILED
				}
			}
		} catch (err: CancellationException) {
			_result.value = UserSwitchResult.IDLE
			throw err
		} catch (err: Exception) {
			_failure.value = classifyConnectionFailure(err)
			_result.value = UserSwitchResult.FAILED
		}
		return _result.value == UserSwitchResult.SWITCHED
	}

	suspend fun switch(user: PrivateUser): Boolean {
		if (_result.value == UserSwitchResult.SWITCHING || _result.value == UserSwitchResult.SWITCHED) return false
		if (sessions.currentSession.value?.let { it.serverId == user.serverId && it.userId == user.id } == true) return false
		attemptedUser = user
		_failure.value = null
		if (user.accessToken == null || authenticationPreferences[AuthenticationPreferences.alwaysAuthenticate]) {
			_failure.value = ConnectionFailure.SIGN_IN
			_result.value = UserSwitchResult.SIGN_IN_REQUIRED
			return false
		}
		_result.value = UserSwitchResult.SWITCHING
		try {
			if (sessions.switchCurrentSession(user.serverId, user.id)) _result.value = UserSwitchResult.SWITCHED
			else {
				_failure.value = classifyConnectionFailure(sessions.lastSwitchError)
				_result.value = UserSwitchResult.FAILED
			}
		} catch (err: CancellationException) {
			_result.value = UserSwitchResult.IDLE
			throw err
		} catch (err: Exception) {
			_failure.value = classifyConnectionFailure(err)
			_result.value = UserSwitchResult.FAILED
		}
		return _result.value == UserSwitchResult.SWITCHED
	}
}

@Composable
fun QuickUserSwitcher(
	onDismissRequest: () -> Unit,
	serverOverride: Server? = null,
	usersOverride: List<User>? = null,
	onSelectUser: ((User) -> Unit)? = null,
	onAddUser: (() -> Unit)? = null,
	onSelectServer: (() -> Unit)? = null,
	onUserLongClick: ((User) -> Unit)? = null,
	embedded: Boolean = false,
	busyOverride: Boolean = false,
	notice: String? = null,
) {
	val sessions = koinInject<SessionRepository>()
	val servers = koinInject<ServerRepository>()
	val users = koinInject<ServerUserRepository>()
	val authentication = koinInject<AuthenticationRepository>()
	val preferences = koinInject<AuthenticationPreferences>()
	val activity = LocalActivity.current
	val scope = rememberCoroutineScope()
	val controller = remember { QuickUserSwitchController(sessions, preferences) }
	val result by controller.result.collectAsState()
	val failure by controller.failure.collectAsState()
	// Snapshot the old account while it is still active; the new activity owns the next home.
	val server = serverOverride ?: remember { servers.currentServer.value }
	val active = remember { sessions.currentSession.value }
	val savedUsers = usersOverride ?: remember(server) {
		server?.let(users::getStoredServerUsers).orEmpty()
	}
	val orderedUsers = savedUsers.sortedByDescending { it.id == active?.userId && it.serverId == active.serverId }
	val busy = busyOverride || result == UserSwitchResult.SWITCHING || result == UserSwitchResult.SWITCHED
	val retryFocus = remember { FocusRequester() }
	val initialFocus = remember { FocusRequester() }
	LaunchedEffect(embedded, savedUsers.isEmpty()) {
		if (embedded) { withFrameNanos { }; initialFocus.requestFocus() }
	}
	LaunchedEffect(result) {
		if (result == UserSwitchResult.FAILED || result == UserSwitchResult.SIGN_IN_REQUIRED) retryFocus.requestFocus()
	}
	fun openStartup(signIn: Boolean, selectServer: Boolean = false) {
		if (signIn) {
			activity?.startActivity(ActivityDestinations.accountSelection(activity, selectServer,
				addUser = !selectServer, serverId = server?.id))
			return
		}
		activity?.startActivity(ActivityDestinations.startup(activity, selectServer = selectServer))
		// MainActivity is singleTask: remove the old instance before startup can
		// launch the next home, so no old view models or back stack can be reused.
		activity?.finish()
	}
	val content: @Composable () -> Unit = {
		Column(
			modifier = Modifier.fillMaxSize().background(LumaFinPalette.Background).padding(vertical = 32.dp),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center,
		) {
			Image(androidx.compose.ui.res.painterResource(R.drawable.app_logo), contentDescription = null,
				modifier = Modifier.height(44.dp))
			Spacer(Modifier.height(24.dp))
			Text(stringResource(R.string.user_switch_heading), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
			Spacer(Modifier.height(8.dp))
			Text(server?.name.orEmpty(), fontSize = 14.sp, color = LumaFinPalette.Muted)
			Spacer(Modifier.height(20.dp))
			if (failure == null) LazyRow(
				modifier = Modifier.fillMaxWidth().focusRequester(retryFocus).focusGroup(),
				contentPadding = PaddingValues(horizontal = 48.dp, vertical = 12.dp),
				horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
			) {
				items(orderedUsers.size, key = { orderedUsers[it].id }) { index ->
					val user = orderedUsers[index]
					val current = user.id == active?.userId && user.serverId == active.serverId
					UserProfileCard(
						modifier = if (index == 0) Modifier.focusRequester(initialFocus) else Modifier,
						name = user.name,
						imageUrl = server?.let { authentication.getUserImageUrl(it, user) },
						color = profileColors[Math.floorMod(user.id.hashCode(), profileColors.size)],
						current = current,
						enabled = !busy,
						onClick = {
							if (onSelectUser != null) onSelectUser(user)
							else if (current) onDismissRequest()
							else scope.launch { if (controller.switch(user as PrivateUser)) openStartup(false) }
						},
						onLongClick = onUserLongClick?.let { { it(user) } },
					)
				}
				item(key = "other-users") {
					UserProfileCard(name = stringResource(R.string.user_switch_other_users), imageUrl = null,
						modifier = if (orderedUsers.isEmpty()) Modifier.focusRequester(initialFocus) else Modifier,
						color = LumaFinPalette.Raised, current = false, enabled = !busy, otherUsers = true,
						onClick = { if (onAddUser != null) onAddUser() else openStartup(true) })
				}
			}
			else Column(
				modifier = Modifier.fillMaxWidth().padding(horizontal = 100.dp, vertical = 20.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(12.dp),
			) {
				Text(stringResource(failure!!.title), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
				Text(stringResource(failure!!.message), color = LumaFinPalette.SecondaryText, fontSize = 16.sp, textAlign = TextAlign.Center)
				Text(stringResource(R.string.connection_account_preserved), color = LumaFinPalette.SecondaryText, fontSize = 14.sp)
			}
			Box(Modifier.height(54.dp).fillMaxWidth().padding(horizontal = 48.dp), contentAlignment = Alignment.Center) {
				if (notice != null && !busy) Text(notice, color = LumaFinPalette.SecondaryText, fontSize = 13.sp, textAlign = TextAlign.Center)
				else if (busy) Text(
					stringResource(when (result) {
						UserSwitchResult.SWITCHING, UserSwitchResult.SWITCHED -> R.string.user_switch_loading
						UserSwitchResult.SIGN_IN_REQUIRED -> R.string.user_switch_sign_in_required
						else -> R.string.user_switch_failed
					}), color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center,
				)
			}
			Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
				if (failure == ConnectionFailure.UNREACHABLE && server?.backupAddress != null) Button(onClick = {
					scope.launch { if (controller.tryBackup()) openStartup(false) }
				}, enabled = !busy) { Text(stringResource(R.string.connection_try_backup)) }
				if (failure?.retryable == true) Button(onClick = {
					scope.launch { if (controller.retry()) openStartup(false) }
				}, modifier = Modifier.focusRequester(retryFocus), enabled = !busy) {
					Text(stringResource(R.string.connection_retry))
				}
				if (failure == ConnectionFailure.SIGN_IN) Button(onClick = { openStartup(true) },
					modifier = Modifier.focusRequester(retryFocus), enabled = !busy) {
					Text(stringResource(R.string.connection_review_sign_in))
				}
				Button(onClick = { if (onSelectServer != null) onSelectServer() else openStartup(true, selectServer = true) },
					modifier = if (failure != null && failure?.retryable == false && failure != ConnectionFailure.SIGN_IN)
						Modifier.focusRequester(retryFocus) else Modifier,
					enabled = !busy) { Text(stringResource(R.string.user_switch_servers)) }
				Button(onClick = onDismissRequest, enabled = !busy) { Text(stringResource(android.R.string.cancel)) }
			}
		}
	}
	if (embedded) content() else DialogBase(visible = true, onDismissRequest = { if (!busy) onDismissRequest() },
		scrimColor = LumaFinPalette.Background) { content() }
}

private val profileColors = listOf(Color(0xFF285788), Color(0xFF544288), Color(0xFF236477), Color(0xFF35458A), Color(0xFF674576))

@Composable
private fun UserProfileCard(
	name: String,
	modifier: Modifier = Modifier,
	imageUrl: String?,
	color: Color,
	current: Boolean,
	enabled: Boolean,
	otherUsers: Boolean = false,
	onLongClick: (() -> Unit)? = null,
	onClick: () -> Unit,
) {
	val interactions = remember { MutableInteractionSource() }
	val focused by interactions.collectIsFocusedAsState()
	val accessibility = rememberAccessibilityOptions()
	val scale by animateFloatAsState(if (focused) 1.06f else 1f)
	val shape = RoundedCornerShape(12.dp)
	val painter = rememberAsyncImagePainter(imageUrl)
	val imageState by painter.state.collectAsState()
	val currentLabel = stringResource(R.string.user_switch_current)
	Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(136.dp)) {
		ButtonBase(
			onClick = onClick, onLongClick = onLongClick, enabled = enabled, interactionSource = interactions, shape = shape,
			colors = ButtonDefaults.colors(containerColor = color, focusedContainerColor = color, contentColor = Color.White,
				focusedContentColor = Color.White, disabledContainerColor = color.copy(alpha = 0.5f)),
			modifier = modifier.size(128.dp).graphicsLayer { scaleX = scale; scaleY = scale }
				.border(3.dp, if (!focused) Color.Transparent else if (accessibility.strongFocus) Color.White else LumaFinPalette.Cyan, shape)
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
		Text(name, color = if (focused) Color.White else LumaFinPalette.SecondaryText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
			textAlign = TextAlign.Center, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
		// Current account uses the purple "active" chip from the rest of the app; the empty slot keeps cards aligned.
		Box(Modifier.height(24.dp), contentAlignment = Alignment.Center) {
			if (current) Text(currentLabel, color = LumaFinPalette.Purple, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
				modifier = Modifier.background(LumaFinPalette.Selected, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 3.dp))
		}
	}
}

package dev.sora.protohax.ui.components.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.sora.protohax.BuildConfig
import dev.sora.protohax.R
import dev.sora.protohax.relay.service.AppService
import dev.sora.protohax.ui.activities.AppPickerActivity
import dev.sora.protohax.ui.activities.MainActivity
import dev.sora.protohax.ui.components.*
import dev.sora.protohax.ui.navigation.PHaxTopLevelDestination
import dev.sora.protohax.util.ContextUtils.isAppExists
import dev.sora.protohax.util.ContextUtils.toast
import dev.sora.protohax.util.NavigationType
import kotlinx.coroutines.launch

private fun getTargetPackage(ctx: Context): String {
    return MainActivity.targetPackage.let {
        if(ctx.packageManager.isAppExists(it)) it else {
            MainActivity.targetPackage = ""
            ""
        }
    }
}

@Composable
fun DashboardScreen(
    navigationType: NavigationType,
    connectionState: State<Boolean>,
    navigateToTopLevelDestination: (PHaxTopLevelDestination) -> Unit
) {
    val ctx = LocalContext.current
    val menuCreate = remember { mutableStateOf(false) }
    val dialogAbout = remember { mutableStateOf(false) }
    val applicationSelected = remember { mutableStateOf(getTargetPackage(ctx)) }
    val pickAppActivityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        applicationSelected.value = getTargetPackage(ctx)
    }
	val snackbarHostState = remember { SnackbarHostState() }

    MenuDashboard(menuCreate, dialogAbout)
    DialogAbout(dialogAbout)

    PHaxAppBar(
        title = stringResource(id = R.string.app_name),
        navigationType = navigationType,
        actions = {
            IconButton(onClick = { menuCreate.value = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(id = R.string.dashboard_more)
                )
            }
        },
		floatingActionButton = {
			BottomFloatingActionButton(connectionState, applicationSelected, pickAppActivityLauncher, snackbarHostState)
		},
		snackbarHost = {
			SnackbarHost(hostState = snackbarHostState)
		}
    ) {
        Column(modifier = Modifier.padding(it)) {
            CardLoginAlert(navigateToTopLevelDestination)
			CardUser(navigateToTopLevelDestination)
            CardCurrentApplication(applicationSelected, pickAppActivityLauncher)
        }
    }
}

@Composable
private fun BottomFloatingActionButton(
    connectionState: State<Boolean>,
    applicationSelected: State<String>,
    pickAppActivityLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
	snackbarHostState: SnackbarHostState
) {
    val mContext = LocalContext.current
    val scope = rememberCoroutineScope()

    fun connectVPN() {
        val intent = Intent(AppService.ACTION_START)
        intent.setPackage(mContext.packageName)
        mContext.startForegroundService(intent)

        scope.launch {
			val result = snackbarHostState.showSnackbar(
				message = mContext.getString(R.string.mitm_connected),
				actionLabel = mContext.getString(R.string.mitm_connected_launch),
				duration = SnackbarDuration.Short
			)
			if (result == SnackbarResult.ActionPerformed) {
				val intent1 = mContext.packageManager.getLaunchIntentForPackage(applicationSelected.value)
				mContext.startActivity(intent1)
			}
        }
    }

	fun disconnectVPN() {
		val intent = Intent(AppService.ACTION_STOP)
		intent.setPackage(mContext.packageName)
		mContext.startForegroundService(intent)

		scope.launch {
			snackbarHostState.showSnackbar(
				message = mContext.getString(R.string.mitm_disconnected),
				duration = SnackbarDuration.Short
			)
		}
	}

    val vpnRequestLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            connectVPN()
        }
    }
    fun checkVPN() {
        val intent = VpnService.prepare(mContext)
        if (intent != null) {
            vpnRequestLauncher.launch(intent)
        } else {
            connectVPN()
        }
    }
    val overlayRequestLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
		if (Settings.canDrawOverlays(mContext)) {
            checkVPN()
        }
    }

	ExtendedFloatingActionButton(
		onClick = {
			if (AppService.isActive) {
				disconnectVPN()
			} else if (applicationSelected.value.isEmpty()){
				scope.launch {
					val result = snackbarHostState.showSnackbar(
						message = mContext.getString(R.string.dashboard_no_application),
						actionLabel = mContext.getString(R.string.dashboard_select_application),
						duration = SnackbarDuration.Short
					)
					if (result == SnackbarResult.ActionPerformed) {
						pickAppActivityLauncher.launch(Intent(mContext, AppPickerActivity::class.java))
					}
				}
			} else {
				if (!Settings.canDrawOverlays(mContext)) {
					mContext.toast(R.string.request_overlay)
					overlayRequestLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
				} else {
					checkVPN()
				}
			}
		},
		elevation = FloatingActionButtonDefaults.elevation(8.dp, 8.dp, 8.dp, 8.dp),
		containerColor = if (connectionState.value) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
		contentColor = if (connectionState.value) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
	) {
		Icon(
			painter = painterResource(id = R.drawable.notification_icon),
			contentDescription = stringResource(id = if (connectionState.value) R.string.dashboard_fab_disconnect else R.string.dashboard_fab_connect),
		)
		Spacer(modifier = Modifier.size(8.dp, 0.dp))
		Text(
			text = stringResource(id = if (connectionState.value) R.string.dashboard_fab_disconnect else R.string.dashboard_fab_connect),
			textAlign = TextAlign.Center
		)
	}
}

@Composable
private fun MenuDashboard(state: MutableState<Boolean>, aboutState: MutableState<Boolean>) {
    Box(
        modifier = Modifier
			.fillMaxSize()
			.wrapContentSize(Alignment.TopEnd)
			.padding(12.dp, 0.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        DropdownMenu(
            expanded = state.value,
            onDismissRequest = { state.value = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.dashboard_about)) },
                onClick = { aboutState.value = true }
            )
        }
    }
}

@Composable
private fun DialogAbout(state: MutableState<Boolean>) {
    if (state.value) {
        AlertDialog(
            icon = { AppIcon() },
            onDismissRequest = { state.value = false },
            title = { Text(stringResource(R.string.app_name)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val hyperlinks = mutableMapOf(
                        "GPLv3" to "https://www.gnu.org/licenses/gpl-3.0.en.html",
                        "GitHub" to "https://github.com/Team-MoonMC",
                        "Discord" to "https://discord.gg/BR4GAEWGeg"
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    HyperlinkText(
                        fullText = stringResource(R.string.dashboard_about_info, formatArgs = hyperlinks.keys.toTypedArray()),
                        hyperLinks = hyperlinks,
                        textStyle = TextStyle(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        linkTextColor = MaterialTheme.colorScheme.primary,
                        linkTextDecoration = TextDecoration.Underline,
                        linkTextFontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {}
        )
    }
}

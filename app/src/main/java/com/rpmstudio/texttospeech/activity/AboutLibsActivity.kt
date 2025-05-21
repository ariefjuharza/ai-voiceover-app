package com.rpmstudio.texttospeech.activity

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.rpmstudio.texttospeech.BuildConfig
import com.rpmstudio.texttospeech.R
import kotlinx.coroutines.launch

class AboutLibsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MainLayout()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainLayout() {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val context = LocalContext.current
    val packageManager = context.packageManager
    val packageName = context.packageName

    val appName = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        "Unknown App"
    }

    val versionCode = try {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION") packageInfo.versionCode.toString()
        }
    } catch (_: PackageManager.NameNotFoundException) {
        "Unknown"
    }

    val buildDate = BuildConfig.BUILD_DATE

    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        var showAuthor by remember { mutableStateOf(true) }
        var showDescription by remember { mutableStateOf(false) }
        var showVersion by remember { mutableStateOf(true) }
        var showLicenseBadges by remember { mutableStateOf(true) }

        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        // Back press to close Bottom Drawer
        BackHandler(enabled = drawerState.isOpen) {
            scope.launch {
                drawerState.close()
            }
        }

        ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                    )
                ) {
                    ToggleableSetting(
                        title = stringResource(R.string.show_author),
                        icon = Icons.Default.Person,
                        enabled = showAuthor,
                        onToggled = { showAuthor = it },
                    )
                    ToggleableSetting(
                        title = stringResource(R.string.show_description),
                        icon = Icons.Default.Description,
                        enabled = showDescription,
                        onToggled = { showDescription = it },
                    )
                    ToggleableSetting(
                        title = stringResource(R.string.show_version),
                        icon = Icons.Default.Build,
                        enabled = showVersion,
                        onToggled = { showVersion = it },
                    )
                    ToggleableSetting(
                        title = stringResource(R.string.show_license_badges),
                        icon = Icons.AutoMirrored.Filled.List,
                        enabled = showLicenseBadges,
                        onToggled = { showLicenseBadges = it },
                    )
                }
            }
        }, gesturesEnabled = drawerState.isOpen, content = {
            Scaffold(
                topBar = {
                    // We use TopAppBar from accompanist-insets-ui which allows us to provide
                    // content padding matching the system bars insets.
                    TopAppBar(title = { Text(stringResource(R.string.about)) }, navigationIcon = {
                        IconButton(onClick = { backDispatcher?.onBackPressed() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back"
                            )
                        }
                    }, colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ), actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    })
                },
            ) { contentPadding ->
                LibrariesContainer(modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding), // Use padding parameter of Scaffold,

                    showAuthor = showAuthor,
                    showDescription = showDescription,
                    showVersion = showVersion,
                    showLicenseBadges = showLicenseBadges,
                    header = {
                        stickyHeader {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(text = appName, fontWeight = FontWeight.Bold)
                                Text(text = stringResource(R.string.version_code, versionCode))
                                Text(text = stringResource(R.string.build_date, buildDate))
                                Text(text = stringResource(R.string.developer_name))
                            }
                        }
                    })
            }
        })

    }
}

@Composable
fun ToggleableSetting(title: String, icon: ImageVector, enabled: Boolean, onToggled: (Boolean) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(interactionSource = interactionSource, onClick = { onToggled(!enabled) }, indication = ripple())
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(30.dp)
    ) {
        Icon(icon, contentDescription = title)
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(interactionSource = interactionSource, checked = enabled, onCheckedChange = {
            onToggled(!enabled)
        })
    }
}

@Preview
@Composable
fun MainLayoutPreview() {
    MainLayout()
}
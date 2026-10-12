package bible.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import bible.app.AppContainer
import bible.app.R
import bible.app.data.BibleDatabase
import bible.app.data.InstallFailed
import bible.app.ui.home.HomeScreen
import bible.app.ui.reader.ReaderScreen
import bible.app.ui.toc.TocScreen
import bible.app.ui.theme.BibleColors

sealed interface LoadState {
    data object Loading : LoadState
    data class Ready(val db: BibleDatabase) : LoadState
    data class Failed(val noSpace: Boolean) : LoadState
}

object Routes {
    const val HOME = "home"
    const val BIBLE = "bible"
    const val TOC = "toc"
}

@Composable
fun AppRoot(container: AppContainer) {
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<LoadState>(LoadState.Loading) }
    LaunchedEffect(attempt) {
        state = try {
            LoadState.Ready(container.database())
        } catch (e: InstallFailed) {
            LoadState.Failed(e.noSpace)
        }
    }
    when (val s = state) {
        LoadState.Loading -> Box(Modifier.fillMaxSize())
        is LoadState.Failed -> InstallErrorScreen(s.noSpace) {
            state = LoadState.Loading
            attempt++
        }
        is LoadState.Ready -> Tabs(container, s.db)
    }
}

@Composable
private fun InstallErrorScreen(noSpace: Boolean, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp).testTag("screen-no-space"),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (noSpace) {
                "Недостаточно памяти для установки текста Библии: нужно около 20 МБ. Освободите место и повторите."
            } else {
                "Не удалось подготовить текст Библии. Попробуйте ещё раз; если не поможет — переустановите приложение."
            },
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry) { Text("Повторить") }
    }
}

private fun NavHostController.openTab(route: String) = navigate(route) {
    popUpTo(Routes.HOME)
    launchSingleTop = true
}

@Composable
private fun Tabs(container: AppContainer, db: BibleDatabase) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    Scaffold(
        containerColor = BibleColors.Background,
        bottomBar = {
            NavigationBar(containerColor = BibleColors.Background) {
                val colors = NavigationBarItemDefaults.colors(
                    indicatorColor = BibleColors.Tab,
                    selectedIconColor = BibleColors.Ink,
                    selectedTextColor = BibleColors.Ink,
                    unselectedIconColor = BibleColors.Muted,
                    unselectedTextColor = BibleColors.Muted,
                )
                NavigationBarItem(
                    selected = route == Routes.HOME,
                    onClick = { nav.openTab(Routes.HOME) },
                    icon = { Icon(painterResource(R.drawable.ic_home), null) },
                    label = { Text("Главная") },
                    colors = colors,
                )
                NavigationBarItem(
                    selected = route == Routes.BIBLE || route == Routes.TOC,
                    onClick = { nav.openTab(Routes.BIBLE) },
                    icon = { Icon(painterResource(R.drawable.ic_book), null) },
                    label = { Text("Библия") },
                    colors = colors,
                )
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
            composable(Routes.HOME) {
                Box(Modifier.fillMaxSize().testTag("screen-home")) {
                    HomeScreen(db, container.store) { nav.openTab(Routes.BIBLE) }
                }
            }
            composable(Routes.BIBLE) {
                Box(Modifier.fillMaxSize().testTag("screen-reader")) {
                    ReaderScreen(db, container.store) { nav.navigate(Routes.TOC) }
                }
            }
            composable(Routes.TOC) {
                Box(Modifier.fillMaxSize().testTag("screen-toc")) {
                    TocScreen(db, container.store) { nav.navigate(Routes.BIBLE) { popUpTo(Routes.BIBLE) { inclusive = true } } }
                }
            }
        }
    }
}

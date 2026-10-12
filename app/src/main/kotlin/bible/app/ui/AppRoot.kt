package bible.app.ui

import bible.app.ui.common.TabItem
import bible.app.ui.common.BottomTabsHeight
import bible.app.ui.common.BottomTabs
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
    var reading by remember { mutableStateOf(false) }
    val tabsVisible = !(route == Routes.BIBLE && reading)
    val tabsSpace = BottomTabsHeight + 1.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize().background(BibleColors.Background)) {
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.fillMaxSize()) {
            composable(Routes.HOME) {
                Box(Modifier.fillMaxSize().padding(bottom = tabsSpace).testTag("screen-home")) {
                    HomeScreen(db, container.store) { nav.openTab(Routes.BIBLE) }
                }
            }
            composable(Routes.BIBLE) {
                Box(Modifier.fillMaxSize().testTag("screen-reader")) {
                    ReaderScreen(db, container.store, onImmersiveChange = { reading = it }) { nav.navigate(Routes.TOC) }
                }
            }
            composable(Routes.TOC) {
                Box(Modifier.fillMaxSize().padding(bottom = tabsSpace).testTag("screen-toc")) {
                    TocScreen(db, container.store, onBackToReading = { nav.popBackStack() }) {
                        nav.navigate(Routes.BIBLE) { popUpTo(Routes.BIBLE) { inclusive = true } }
                    }
                }
            }
        }
        AnimatedVisibility(
            tabsVisible,
            Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(tween(220)),
            exit = slideOutVertically { it } + fadeOut(tween(220)),
        ) {
            BottomTabs(
                listOf(
                    TabItem("Главная", R.drawable.ic_home, "tab-home", route == Routes.HOME) { nav.openTab(Routes.HOME) },
                    TabItem("Библия", R.drawable.ic_book, "tab-bible", route == Routes.BIBLE || route == Routes.TOC) { nav.openTab(Routes.BIBLE) },
                ),
            )
        }
    }
}

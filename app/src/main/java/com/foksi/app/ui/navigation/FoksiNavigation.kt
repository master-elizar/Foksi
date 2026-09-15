@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.foksi.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.foksi.app.R
import com.foksi.app.domain.model.ItemType
import com.foksi.app.ui.components.QuickAddSheet
import com.foksi.app.ui.screens.birthdays.BirthdaysScreen
import com.foksi.app.ui.screens.calendar.CalendarScreen
import com.foksi.app.ui.screens.detail.DetailScreen
import com.foksi.app.ui.screens.editor.EditorScreen
import com.foksi.app.ui.screens.home.HomeScreen
import com.foksi.app.ui.screens.search.SearchScreen
import com.foksi.app.ui.screens.settings.SettingsScreen
import com.foksi.app.ui.screens.tasks.TasksScreen
import com.foksi.app.ui.screens.whatsnext.WhatsNextScreen
import com.foksi.app.ui.vm.AgendaViewModel
import com.foksi.app.ui.vm.BirthdayViewModel
import com.foksi.app.ui.vm.CalendarViewModel
import com.foksi.app.ui.vm.DetailViewModel
import com.foksi.app.ui.vm.EditorViewModel
import com.foksi.app.ui.vm.FoksiViewModelFactory
import com.foksi.app.ui.vm.SearchViewModel
import com.foksi.app.ui.vm.SettingsViewModel

object Routes {
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val TASKS = "tasks"
    const val BIRTHDAYS = "birthdays"
    const val SETTINGS = "settings"
    const val WHATS_NEXT = "whatsnext"
    const val SEARCH = "search"
    const val DETAIL = "detail/{id}"
    const val EDITOR = "editor?id={id}&type={type}&start={start}"

    fun detail(id: Long) = "detail/$id"
    fun editor(id: Long = 0, type: ItemType = ItemType.EVENT, start: Long = -1L) =
        "editor?id=$id&type=${type.name}&start=$start"
}

private data class BottomTab(val route: String, val labelRes: Int, val icon: ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.HOME, R.string.nav_today, Icons.Outlined.Today),
    BottomTab(Routes.CALENDAR, R.string.nav_calendar, Icons.Outlined.CalendarMonth),
    BottomTab(Routes.TASKS, R.string.nav_tasks, Icons.Outlined.TaskAlt),
    BottomTab(Routes.BIRTHDAYS, R.string.nav_birthdays, Icons.Outlined.Cake),
    BottomTab(Routes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings),
)

@Composable
fun FoksiNavHost(
    navController: NavHostController = rememberNavController(),
    onReady: (NavHostController) -> Unit = {},
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showChrome = currentRoute in bottomTabs.map { it.route }
    var showQuickAdd by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(navController) { onReady(navController) }

    Scaffold(
        bottomBar = {
            if (showChrome) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = backStackEntry?.destination?.hierarchy?.any {
                            it.route == tab.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showChrome) {
                FloatingActionButton(onClick = { showQuickAdd = true }) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.cd_add_button),
                    )
                }
            }
        },
    ) { padding ->
        val direction = LocalLayoutDirection.current
        val contentPadding = PaddingValues(
            start = padding.calculateStartPadding(direction),
            end = padding.calculateEndPadding(direction),
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 16.dp,
        )

        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier,
            enterTransition = {
                slideInHorizontally(tween(220)) { it / 6 } + fadeIn(tween(220))
            },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition = {
                slideOutHorizontally(tween(200)) { it / 6 } + fadeOut(tween(180))
            },
        ) {
            composable(Routes.HOME) {
                val vm: AgendaViewModel = viewModel(factory = FoksiViewModelFactory)
                HomeScreen(
                    viewModel = vm,
                    onOpenItem = { navController.navigate(Routes.detail(it)) },
                    onAddEvent = { navController.navigate(Routes.editor()) },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                    onOpenWhatsNext = { navController.navigate(Routes.WHATS_NEXT) },
                    contentPadding = contentPadding,
                )
            }

            composable(Routes.CALENDAR) {
                val vm: CalendarViewModel = viewModel(factory = FoksiViewModelFactory)
                CalendarScreen(
                    viewModel = vm,
                    onOpenItem = { navController.navigate(Routes.detail(it)) },
                    contentPadding = contentPadding,
                )
            }

            composable(Routes.TASKS) {
                val vm: AgendaViewModel = viewModel(factory = FoksiViewModelFactory)
                TasksScreen(
                    viewModel = vm,
                    onOpenItem = { navController.navigate(Routes.detail(it)) },
                    contentPadding = contentPadding,
                )
            }

            composable(Routes.BIRTHDAYS) {
                val vm: BirthdayViewModel = viewModel(factory = FoksiViewModelFactory)
                BirthdaysScreen(
                    viewModel = vm,
                    onOpenItem = { navController.navigate(Routes.detail(it)) },
                    onAddBirthday = {
                        navController.navigate(Routes.editor(type = ItemType.BIRTHDAY))
                    },
                    contentPadding = contentPadding,
                )
            }

            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(factory = FoksiViewModelFactory)
                SettingsScreen(viewModel = vm, contentPadding = contentPadding)
            }

            composable(Routes.WHATS_NEXT) {
                val vm: AgendaViewModel = viewModel(factory = FoksiViewModelFactory)
                WhatsNextScreen(
                    viewModel = vm,
                    onOpenItem = { navController.navigate(Routes.detail(it)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SEARCH) {
                val vm: SearchViewModel = viewModel(factory = FoksiViewModelFactory)
                SearchScreen(
                    viewModel = vm,
                    onOpenItem = { navController.navigate(Routes.detail(it)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val vm: DetailViewModel = viewModel(factory = FoksiViewModelFactory)
                DetailScreen(
                    eventId = id,
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.editor(id = it)) },
                    onOpenItem = { navController.navigate(Routes.detail(it)) },
                )
            }

            composable(
                route = Routes.EDITOR,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("type") { type = NavType.StringType; defaultValue = ItemType.EVENT.name },
                    navArgument("start") { type = NavType.LongType; defaultValue = -1L },
                ),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val typeName = entry.arguments?.getString("type") ?: ItemType.EVENT.name
                val start = entry.arguments?.getLong("start") ?: -1L
                val vm: EditorViewModel = viewModel(factory = FoksiViewModelFactory)
                EditorScreen(
                    eventId = id,
                    initialType = runCatching { ItemType.valueOf(typeName) }.getOrDefault(ItemType.EVENT),
                    prefillStart = start.takeIf { it > 0 },
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onSaved = { savedId ->
                        navController.popBackStack()
                        if (id == 0L) navController.navigate(Routes.detail(savedId))
                    },
                )
            }
        }

        if (showQuickAdd) {
            val quickVm: AgendaViewModel = viewModel(factory = FoksiViewModelFactory)
            QuickAddSheet(
                onDismiss = { showQuickAdd = false },
                onCreateEvent = {
                    showQuickAdd = false
                    navController.navigate(Routes.editor())
                },
                onCreateBirthday = {
                    showQuickAdd = false
                    navController.navigate(Routes.editor(type = ItemType.BIRTHDAY))
                },
                onQuickSave = { type, text, description ->
                    quickVm.quickSave(type, text, description)
                    showQuickAdd = false
                },
            )
        }
    }
}

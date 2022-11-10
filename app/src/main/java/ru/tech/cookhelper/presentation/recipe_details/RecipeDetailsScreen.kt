package ru.tech.cookhelper.presentation.recipe_details

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import dev.olshevski.navigation.reimagined.hilt.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.tech.cookhelper.R
import ru.tech.cookhelper.core.constants.Constants.DOTS
import ru.tech.cookhelper.core.utils.kotlin.cptlize
import ru.tech.cookhelper.domain.model.Recipe
import ru.tech.cookhelper.presentation.app.components.Picture
import ru.tech.cookhelper.presentation.app.components.TopAppBar
import ru.tech.cookhelper.presentation.app.components.TopAppBarSize
import ru.tech.cookhelper.presentation.forum_screen.components.IndicatorType
import ru.tech.cookhelper.presentation.forum_screen.components.TabRow
import ru.tech.cookhelper.presentation.recipe_details.viewModel.RecipeDetailsViewModel
import ru.tech.cookhelper.presentation.recipe_post_creation.components.Separator
import ru.tech.cookhelper.presentation.ui.utils.compose.ResUtils.stringResourceListOf
import ru.tech.cookhelper.presentation.ui.utils.compose.ScrollBehavior
import ru.tech.cookhelper.presentation.ui.utils.compose.TopAppBarUtils.topAppBarScrollBehavior
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun RecipeDetailsScreen(
    recipe: Recipe?,
    percentString: String,
    onBack: () -> Unit,
    viewModel: RecipeDetailsViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.updateRecipe(recipe)
    }

    val recipeState = viewModel.recipe
    val scrollBehavior = topAppBarScrollBehavior(ScrollBehavior.ExitUntilCollapsed())

    Column(Modifier.navigationBarsPadding()) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, null)
                }
            },
            topAppBarSize = TopAppBarSize.Large,
            title = { Text(recipeState?.title ?: "") },
            scrollBehavior = scrollBehavior
        )
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val state = rememberPagerState()
            val scope = rememberCoroutineScope()

            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                LandscapeContent(
                    recipe = recipe,
                    scrollBehavior = scrollBehavior,
                    scope = scope,
                    state = state
                )
            } else {
                PortraitContent(
                    recipe = recipe,
                    scrollBehavior = scrollBehavior,
                    scope = scope,
                    state = state
                )
            }
        }
    }

    BackHandler(onBack = onBack)

}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
private fun BoxWithConstraintsScope.PortraitContent(
    recipe: Recipe?,
    scrollBehavior: TopAppBarScrollBehavior,
    scope: CoroutineScope,
    state: PagerState
) {
    val constraints = this.constraints
    val maxHeight = with(LocalDensity.current) { constraints.maxHeight.toDp() }
    val maxWidth = with(LocalDensity.current) { constraints.maxWidth.toDp() }
    val selectedTab = state.currentPage

    Column(
        Modifier
            .height(maxHeight)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            var scrolled by remember { mutableStateOf(0) }
            Picture(
                model = recipe?.image?.link,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .width(maxWidth)
                    .height((maxWidth / 1.5f) - (0.5 * scrolled).dp)
                    .padding(12.dp)
            )
            HorizontalPager(
                modifier = Modifier.weight(1f),
                count = 3,
                state = state,
                verticalAlignment = Alignment.Top
            ) { page ->
                val scrollState = rememberScrollState()

                LaunchedEffect(scrollState.value) {
                    scrolled = scrollState.value
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    when (page) {
                        0 -> {
                            if (recipe != null) {
                                repeat(recipe.ingredients.size) {
                                    val product = recipe.ingredients[it]
                                    val measure = recipe.measures[it]

                                    Row(Modifier.padding(8.dp)) {
                                        Text(text = product.title.cptlize())
                                        Text(
                                            text = DOTS,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Clip
                                        )
                                        Text(text = "${if (measure.isRoundable()) measure.roundToInt() else measure} ${product.mimetype}")
                                    }
                                }
                            }
                        }
                        1 -> {
                            if (recipe != null) {
                                repeat(recipe.cookSteps.size) {
                                    Text(
                                        text = recipe.cookSteps[it],
                                        modifier = Modifier.padding(16.dp)
                                    )
                                    Spacer(Modifier.height(16.dp))
                                }
                            }
                        }
                        2 -> {

                        }
                    }
                }
            }
        }
        Separator()

        TabRow(
            indicatorType = IndicatorType.Tonal,
            selectedTabIndex = selectedTab,
            tabs = stringResourceListOf(R.string.products, R.string.recipe, R.string.discussion),
            onTabClick = {
                scope.launch {
                    state.animateScrollToPage(it)
                }
            }
        )
    }
}

private fun Double.isRoundable(): Boolean {
    return this.toString().split(".")[1] == "0"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
private fun BoxWithConstraintsScope.LandscapeContent(
    recipe: Recipe?,
    scrollBehavior: TopAppBarScrollBehavior,
    scope: CoroutineScope,
    state: PagerState
) {

}
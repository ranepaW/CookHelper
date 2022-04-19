package ru.tech.prokitchen.presentation.recipes_list.components

import ru.tech.prokitchen.domain.model.Recipe

data class CuisineState(
    val isLoading: Boolean = false,
    val recipeList: List<Recipe>? = null,
    val error: String = ""
)
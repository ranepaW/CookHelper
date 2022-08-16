package ru.tech.cookhelper.presentation.authentication.components

import ru.tech.cookhelper.domain.model.User

data class RestorePasswordState(
    val isLoading: Boolean = false,
    val state: RestoreState = RestoreState.Login,
    val user: User? = null
)

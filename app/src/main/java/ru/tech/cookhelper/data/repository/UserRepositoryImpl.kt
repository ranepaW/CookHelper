package ru.tech.cookhelper.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.tech.cookhelper.core.Action
import ru.tech.cookhelper.data.local.dao.UserDao
import ru.tech.cookhelper.data.local.entity.toEntity
import ru.tech.cookhelper.data.local.entity.toUser
import ru.tech.cookhelper.data.remote.api.auth.AuthService
import ru.tech.cookhelper.data.remote.api.auth.toUser
import ru.tech.cookhelper.domain.model.User
import ru.tech.cookhelper.domain.repository.UserRepository
import javax.inject.Inject

@Suppress("BlockingMethodInNonBlockingContext")
class UserRepositoryImpl @Inject constructor(
    private val authService: AuthService,
    private val userDao: UserDao
) : UserRepository {

    override fun loginWith(login: String, password: String): Flow<Action<User?>> = flow {
        emit(Action.Loading())
        val response = io { authService.loginWith(login, password).execute() }
        val body = response.let { it.body() ?: throw Exception("${it.code()} ${it.message()}") }

        when (body.status) {
            101, 102 -> emit(Action.Empty(body.status))
            100, 103 -> emit(Action.Success(data = body.user?.toUser()))
            else -> emit(Action.Error(message = body.message))
        }
    }.catch { t -> emit(Action.Error(message = t.message.toString())) }

    override fun registerWith(
        name: String,
        surname: String,
        nickname: String,
        email: String,
        password: String
    ): Flow<Action<User?>> = flow {
        emit(Action.Loading())
        val response =
            io { authService.registerWith(name, surname, nickname, email, password).execute() }
        val body = response.let { it.body() ?: throw Exception("${it.code()} ${it.message()}") }

        if (body.status == 100) emit(Action.Success(data = body.user?.toUser()))
        else emit(Action.Error(message = body.message))

    }.catch { t -> emit(Action.Error(message = t.message.toString())) }

    override suspend fun requestCode(
        token: String
    ): Result<User?> = runCatching {
        authService.requestCode(token).user?.toUser()
    }

    override fun checkCode(code: String, token: String): Flow<Action<User?>> = flow {
        emit(Action.Loading())
        val response = io { authService.verifyEmail(code, token).execute() }
        val body = response.let { it.body() ?: throw Exception("${it.code()} ${it.message()}") }

        when (body.status) {
            102 -> emit(Action.Empty())
            100 -> emit(Action.Success(data = body.user?.toUser()))
            else -> emit(Action.Error(message = body.message))
        }
    }.catch { t -> emit(Action.Error(message = t.message.toString())) }

    override suspend fun cacheUser(user: User) = userDao.cacheUser(user.toEntity())

    override fun getUser(): Flow<User?> = userDao.getUser().map { it?.toUser() }

    override suspend fun checkLoginOrEmailForAvailability(
        query: String
    ): Action<User?> = try {
        val response = authService.checkLoginOrEmailForAvailability(query)
        if (response.status == 100) Action.Success(data = response.user?.toUser())
        else Action.Error(message = response.message)
    } catch (t: Throwable) {
        Action.Error(message = t.message.toString())
    }

    override suspend fun logOut() = userDao.clearUser()

    override suspend fun requestPasswordRestoreCode(
        login: String
    ): Action<User?> {
        val result = kotlin.runCatching { authService.requestPasswordRestoreCode(login) }
        if (result.isFailure) {
            return Action.Error(message = result.exceptionOrNull()?.message)
        } else {
            val authInfo = result.getOrNull()
            if (authInfo != null) {
                return when (authInfo.status) {
                    100 -> Action.Success(authInfo.user?.toUser())
                    else -> {
                        Action.Empty(status = authInfo.status)
                    }
                }
            }
            return Action.Empty()
        }
    }


    override fun restorePasswordBy(
        login: String,
        code: String,
        newPassword: String
    ): Flow<Action<User?>> = flow {
        emit(Action.Loading())
        val response = io { authService.restorePasswordBy(login, code, newPassword).execute() }
        val body = response.let { it.body() ?: throw Exception("${it.code()} ${it.message()}") }

        when (body.status) {
            102 -> emit(Action.Empty())
            100 -> emit(Action.Success(data = body.user?.toUser()))
            else -> emit(Action.Error(message = body.message))
        }
    }.catch { t -> emit(Action.Error(message = t.message.toString())) }

}

private suspend fun <T> io(
    function: suspend () -> T
): T = withContext(Dispatchers.IO) { function() }
package org.jetbrains.demo.thinkter

import org.jetbrains.demo.thinkter.dao.*
import org.jetbrains.demo.thinkter.model.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.receive
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set

fun Route.register(dao: ThinkterStorage, hashFunction: (String) -> String) {
    post<Register> { _ ->
        val form = call.receive<Parameters>()
        val userId = form["userId"] ?: ""
        val password = form["password"] ?: ""
        val displayName = form["displayName"] ?: ""
        val email = form["email"] ?: ""
        val user = call.sessions.get<Session>()?.let { dao.user(userId) }

        if (user != null) {
            call.redirect(LoginResponse(user))
        } else {
            if (password.length < 6) {
                call.respond(LoginResponse(error = "Password should be at least 6 characters long"))
            } else if (userId.length < 4) {
                call.respond(LoginResponse(error = "Login should be at least 4 characters long"))
            } else if (!userNameValid(userId)) {
                call.respond(LoginResponse(error = "Login should be consists of digits, letters, dots or underscores"))
            } else if (dao.user(userId) != null) {
                call.respond(LoginResponse(error = "User with the following login is already registered"))
            } else {
                val hash = hashFunction(password)
                val newUser = User(userId, email, displayName, hash)

                try {
                    dao.createUser(newUser)
                } catch (e: Throwable) {
                    when {
                        dao.user(userId) != null ->
                            call.respond(LoginResponse(error = "User with the following login is already registered"))
                        dao.userByEmail(email) != null ->
                            call.respond(LoginResponse(error = "User with the following email $email is already registered"))
                        else -> {
                            application.environment.log.error("Failed to register user", e)
                            call.respond(LoginResponse(error = "Failed to register"))
                        }
                    }
                }

                call.sessions.set(Session(newUser.userId))
                call.respond(LoginResponse(newUser))
            }
        }
    }
    get<Register> {
        call.respond(HttpStatusCode.MethodNotAllowed)
    }
}

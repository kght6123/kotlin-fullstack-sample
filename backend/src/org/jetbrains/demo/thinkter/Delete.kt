package org.jetbrains.demo.thinkter

import org.jetbrains.demo.thinkter.dao.*
import org.jetbrains.demo.thinkter.model.*
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.sessions.*

fun Route.delete(dao: ThinkterStorage, hashFunction: (String) -> String) {
    get<ThoughtDelete> { _ ->
        val user = call.sessions.get<Session>()?.let { dao.user(it.userId) }
        val date = System.currentTimeMillis()

        if (user == null) {
            call.respond(HttpStatusCode.Forbidden)
        } else {
            val code = call.securityCode(date, user, hashFunction)
            call.respond(PostThoughtToken(user.userId, date, code))
        }
    }

    post<ThoughtDelete> { it ->
        val user = call.sessions.get<Session>()?.let { dao.user(it.userId) }
        val thought = dao.getThought(it.id)

        val form = call.receive<Parameters>()

        val date = form["date"]?.toLong() ?: -1
        val code = form["code"] ?: ""

        if (user == null || thought.userId != user.userId || !call.verifyCode(date, user, code, hashFunction)) {
            call.respond(HttpStatusCode.Forbidden)
        } else {
            dao.deleteThought(it.id)
            call.respond(object : RpcData {})
        }
    }
}

package ua.eco.aggregator.api.admin.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import java.security.MessageDigest

fun Application.configureAuth() {
    install(Authentication) {
        basic("auth-basic") {
            validate { userPasswordCredentials ->
                if (authenticate(userPasswordCredentials.name, userPasswordCredentials.password.sha512()))
                    UserIdPrincipal(userPasswordCredentials.name)
                else
                    null
            }
        }
    }
}

private fun authenticate(username: String, passwordSHA512Hash: String): Boolean {
    return username == System.getenv("UAECOAGGREGATOR_USERNAME") &&
            passwordSHA512Hash == System.getenv("UAECOAGGREGATOR_PASSWORD_HASH")
}

private fun String.sha512(): String = MessageDigest
    .getInstance("SHA-512")
    .digest(this.toByteArray())
    .fold("") { str, it -> str + "%02x".format(it) }
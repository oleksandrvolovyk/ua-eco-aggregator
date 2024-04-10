package ua.eco.aggregator.api.admin.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import java.security.MessageDigest

fun Application.configureAuth() {
    install(Authentication) {
        basic("auth-basic") {
            validate { userPasswordCredentials ->
                if (authenticate(userPasswordCredentials.name, userPasswordCredentials.password.sha256()))
                    UserIdPrincipal(userPasswordCredentials.name)
                else
                    null
            }
        }
    }
}

private fun authenticate(username: String, passwordSHA256Hash: String): Boolean {
    return username == System.getenv("UAECOAGGREGATOR_USERNAME") &&
            passwordSHA256Hash == System.getenv("UAECOAGGREGATOR_PASSWORD_HASH")
}

private fun String.sha256(): String = MessageDigest
    .getInstance("SHA-256")
    .digest(this.toByteArray())
    .fold("") { str, it -> str + "%02x".format(it) }
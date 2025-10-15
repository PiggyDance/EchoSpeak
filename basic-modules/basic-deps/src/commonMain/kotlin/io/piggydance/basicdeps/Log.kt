package io.piggydance.basicdeps

import io.github.oshai.kotlinlogging.KotlinLogging

object Log {

    private val LOGGER = KotlinLogging.logger {}

    fun d(tag: String, msg: String) {
        LOGGER.debug { "[$tag] $msg" }
    }

    fun e(tag: String, msg: String) {
        LOGGER.error { "[$tag] $msg" }
    }

    fun i(tag: String, msg: String) {
        LOGGER.info { "[$tag] $msg" }
    }

    fun w(tag: String, msg: String) {
        LOGGER.warn { "[$tag] $msg" }
    }
}
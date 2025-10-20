package io.piggydance.basicdeps

object Log {

    private val LOGGER = saschpe.log4k.Log

    fun d(tag: String, msg: String) {
        LOGGER.debug(tag = tag) { msg }
    }

    fun e(tag: String, msg: String, error: Throwable? = null) {
        LOGGER.error(error, tag) { msg }
    }

    fun i(tag: String, msg: String) {
        LOGGER.info(tag = tag) { msg }
    }

    fun w(tag: String, msg: String, error: Throwable? = null) {
        LOGGER.warn(error, tag) { msg }
    }
}
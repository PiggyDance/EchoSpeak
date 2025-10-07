package io.piggydance.echospeak

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
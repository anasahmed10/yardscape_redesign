package com.naslabs.yardscape

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
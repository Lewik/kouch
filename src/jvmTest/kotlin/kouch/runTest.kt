package kouch

import kotlinx.coroutines.runBlocking

actual fun runTest(block: suspend () -> Unit) = runBlocking { block() }
actual fun <T> runTestWithResult(block: suspend () -> T) = runBlocking { block() }

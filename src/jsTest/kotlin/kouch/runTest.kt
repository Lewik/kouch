package kouch

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

actual fun runTest(block: suspend () -> Unit): dynamic = TODO()//GlobalScope.promise { block() }
actual fun <T> runTestWithResult(block: suspend () -> T): dynamic = TODO()//GlobalScope.promise { block() }

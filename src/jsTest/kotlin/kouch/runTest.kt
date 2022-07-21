package kouch

actual fun runTest(block: suspend () -> Unit): dynamic = Unit //TODO
actual fun <T> runTestWithResult(block: suspend () -> T): dynamic = Unit //TODO

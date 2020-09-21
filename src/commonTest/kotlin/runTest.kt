package kouch

expect fun runTest(block: suspend () -> Unit)
expect fun <T> runTestWithResult(block: suspend () -> T): T

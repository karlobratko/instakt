package hr.kbratko.instakt.infrastructure.jobs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration


fun interface Job {
    suspend operator fun invoke()
}

fun CoroutineScope.schedule(
    every: Duration,
    execute: Job,
    failureHandler: (error: Throwable) -> Unit = {}
): kotlinx.coroutines.Job =
    launch {
        // An infinite loop that executes the job at the specified interval.
        while (true) {
            // Pause the coroutine for the specified duration.
            delay(every)
            try {
                // Try to execute the job.
                execute()
            } catch (e: Throwable) {
                // If the job throws an exception, handle it with the failureHandler function.
                failureHandler(e)
            }
        }
    }

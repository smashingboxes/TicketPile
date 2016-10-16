package ticketpile.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import ticketpile.service.advance.AdvanceManager
import ticketpile.service.advance.AdvanceSyncController
import ticketpile.service.advance.bookingQueueSync
import ticketpile.service.advance.individualBookingSync
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Background tasks meant to launch after the application is started and DB
 * is connected.
 * 
 * Created by jonlatane on 10/14/16.
 */
fun wrapTask(task: () -> Unit, taskName: String) : () -> Unit {
    return {
        try {
            task()
        } catch(t: Throwable) {
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            println("$taskName error: $sw")
        }
    }
}

@Component
open class BackgroundJobs() : CommandLineRunner, Ordered {
    @Autowired
    lateinit var syncController : AdvanceSyncController
    override fun run(vararg args : String) {
        syncController.unstickQueues()
        
        val scheduler = Executors.newScheduledThreadPool(13)
        scheduler.scheduleAtFixedRate(
                wrapTask(bookingQueueSync, "Booking Queue Sync"),
                0, AdvanceManager.syncPeriodSeconds, TimeUnit.SECONDS
        )
        // Schedule multiple tasks for booking sync
        for(offset in 1..3) {
            scheduler.scheduleAtFixedRate(
                    wrapTask(individualBookingSync, "Individual Booking Sync"),
                    0, 10, TimeUnit.MILLISECONDS
            )
        }
    }

    override fun getOrder(): Int {
        return 2
    }
}
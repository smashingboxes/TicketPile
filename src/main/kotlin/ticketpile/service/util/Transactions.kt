package ticketpile.service.util

import org.jetbrains.exposed.dao.EntityCache
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection

/**
 * Functions handy
 */

/**
 * Override of the standard Exposed [org.jetbrains.exposed.sql.transactions.transaction] 
 * function for handier logging control.
 */
var defaultIsolationLevel = Connection.TRANSACTION_REPEATABLE_READ
fun <T> transaction(
        logging: Boolean = true,
        isolationLevel: Int = defaultIsolationLevel,
        repetitionAttempts: Int = 3,
        statement: Transaction.() -> T
): T = org.jetbrains.exposed.sql.transactions.transaction(
        isolationLevel,
        repetitionAttempts,
        {
            if (logging)
                logger.addLogger(StdOutSqlLogger())
            statement()
        })


/**
 * Exposed queues SQL when interacting with the DAO.  Sometimes we need to
 * be sure elements were before continuing computations; in this case we
 * can use this.
 */
fun flushEntityCache() {
    EntityCache.getOrCreate(TransactionManager.current()).flush()
}
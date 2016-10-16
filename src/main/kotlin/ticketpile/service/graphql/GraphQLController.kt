package ticketpile.service.graphql

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import ticketpile.service.springconfig.BearerToken
import ticketpile.service.util.AuthenticationException
import ticketpile.service.util.transaction
import javax.servlet.http.HttpServletRequest

/**
 * Basic GraphQL controller for TicketPile.
 * 
 * Created by jonlatane on 10/11/16.
 */
@RestController
@RequestMapping(value = "/graphql")
open class TPGraphQLController {
    @GetMapping(produces = arrayOf(MediaType.APPLICATION_JSON_VALUE))
    @ResponseBody
    fun executeOperation(@RequestBody body : Map<String, Any>): Any {
        val auth = SecurityContextHolder.getContext().authentication as BearerToken
        val graphQL = createGraphQL(auth.user!!)
        val query = body["query"] as String
        val variables = body["variables"] as Map<String, Any>?
        val executionResult = transaction {
            graphQL.execute(query, null as Any?, variables)
        }
        val result = mutableMapOf<String, Any>()
        if (executionResult.errors?.count() ?: 0 > 0) {
            result.put("errors", executionResult.errors.map{
                "${it.javaClass.simpleName}: ${it.locations}"
            })
            println("Errors: {${executionResult.errors}}")
        }
        if(executionResult.data != null)
            result.put("data", executionResult.data)
        return result
    }

    @PostMapping(consumes = arrayOf("application/json"))
    fun postJson(@RequestBody body: Map<String, Any>,
                 @RequestHeader(value = "graphql-schema", required = false) graphQLSchemaName: String?,
                 httpServletRequest: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        val auth = SecurityContextHolder.getContext().authentication as BearerToken
        if(auth.user == null) {
            throw AuthenticationException("User must be logged in to access GraphQL.")
        }
        val graphQL = createGraphQL(auth.user)

        val query = body["query"] as String
        val operationName = body["operationName"] as String?
        var variables = emptyMap<String, Any>()
        try {
           variables = body["variables"] as Map<String, Any>
        } catch(t:Throwable) {}

        val executionResult = transaction {
            graphQL.execute(query, operationName, null as Any?, variables)
        }
        val result : Map<String, Any>
        if (executionResult.errors?.count() ?: 0 > 0) {
            result = mutableMapOf<String, Any>()
            result.put("errors", executionResult.errors.map{
                "${it.javaClass.simpleName}: ${it.locations}"
            })
            println("Errors: {${executionResult.errors}}")
        } else {
            result = executionResult.data as Map<String,Any>
        }
        
        
        /*val result = transaction {
            TicketPileGraphQL.execute(query, null as Any?, variables).data
        } as Map<String, Any>*/

        return ResponseEntity.ok<Map<String, Any>>(result)
    }
}
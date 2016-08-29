package ticketpile.service.controllers

import io.swagger.annotations.SwaggerDefinition
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import ticketpile.service.model.Event
import ticketpile.service.model.Product

/**
 * Created by jonlatane on 8/29/16.
 */
@RestController
@RequestMapping(value = "/product")
class ProductController {
    @PostMapping(
        produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
    )
    fun createProduct(
            @RequestParam(value = "name", required = true)
            productName: String,
            @RequestParam(value = "description", required = true)
            productDesc: String
    ): Product
    {
        var product : Product? = null
        transaction {
            product = Product.new {
                name = productName
                description = productDesc
            }
        }
        return product!!
    }

    @GetMapping(
            value = "/all",
            produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
    )
    fun allProducts() : Iterable<Product> {
        var allProducts : Iterable<Product> = emptyList()
        transaction {
            allProducts = Product.all().map {it -> it}
        }
        return allProducts
    }
}
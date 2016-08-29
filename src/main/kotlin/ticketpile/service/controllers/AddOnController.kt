package ticketpile.service.controllers

import io.swagger.annotations.SwaggerDefinition
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import ticketpile.service.model.AddOn
import ticketpile.service.model.Event
import ticketpile.service.model.Product

/**
 * Created by jonlatane on 8/29/16.
 */
@RestController
@RequestMapping(value = "/addon")
class AddOnController {
    @PostMapping(
        produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
    )
    fun createAddOn(
            @RequestParam(value = "name", required = true)
            addOnName: String,
            @RequestParam(value = "productId", required = true)
            productId: Int
    ): AddOn
    {
        var addOn : AddOn? = null
        transaction {
            addOn = AddOn.new {
                name = addOnName
                product = Product.findById(productId)!!
            }
        }
        return addOn!!
    }

    @GetMapping(
            value = "/all",
            produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
    )
    fun allAddOns() : Iterable<AddOn> {
        var allAddOns : Iterable<AddOn> = emptyList()
        transaction {
            allAddOns = AddOn.all().map {it -> it}
        }
        return allAddOns
    }
    
}
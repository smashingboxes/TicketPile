package ticketpile.service.controllers

/**
 * Created by jonlatane on 8/29/16.
 *
@RestController
@RequestMapping(value = "/discount")
class DiscountController {
    @PostMapping(
        produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
    )
    fun createDiscount(
            @RequestParam(value = "name", required = true)
            discountName: String,
            @RequestParam(value = "description", required = true)
            discountDesc: String
    ): Discount
    {
        var discount : Discount? = null
        transaction {
            discount = Discount.new {
                name = discountName
                description = discountDesc
            }
        }
        return discount!!
    }

    @GetMapping(
            value = "/all",
            produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
    )
    fun allDiscounts() : Iterable<Discount> {
        var allDiscounts : Iterable<Discount> = emptyList()
        transaction {
            allDiscounts = Discount.all().map { it -> it}
        }
        return allDiscounts
    }
    
}*/
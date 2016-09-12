package ticketpile.service.sisense

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ticketpile.service.model.Booking

/**
 * Created by jonlatane on 9/7/16.
 */
@RestController
@RequestMapping(value = "/sisense")
open class SisenseController {
    @PostMapping(value = "/requestImport")
    fun requestImport(
            @RequestParam(value = "sisenseHost", required = true)
            host: String,
            @RequestParam(value = "adminKey", required = true)
            authorizationKey: String,
            @RequestParam(value = "elasticube", required = true)
            elasticube: String,
            @RequestParam(value = "reservationId", required = true)
            reservationId: Int
    ): Booking? {
        return null
    }
}
package ticketpile.service.sisense

import org.springframework.web.client.RestTemplate
import ticketpile.service.model.PersonCategory
import ticketpile.service.model.Product
import java.net.URL

/**
 * Created by jonlatane on 9/7/16.
 */
class SisenseManager {
    companion object {
        private val restTemplate = RestTemplate()
    }

    private val source: String
    private val authKey: String
    private val locationId: Int
    private val personCategories = mutableMapOf<Int, PersonCategory>()
    private val products = mutableMapOf<Int, Product>()
    
    constructor(host: String, authorizationKey: String, locationId: Int) {
        if (authorizationKey.startsWith("Bearer:")) authKey = authorizationKey.substring(7)
        else authKey = authorizationKey
        this.locationId = locationId
        source = URL(URL(host), "/").toString()
    }
}
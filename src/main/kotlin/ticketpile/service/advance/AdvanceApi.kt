package ticketpile.service.advance

import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.DateTime
import ticketpile.service.util.BigZero
import java.math.BigDecimal

/**
 * Input POJO types for Advance API20 requests.
 * 
 * Created by jonlatane on 8/28/16.
 */
data class AdvanceAuthRequest(
        @get:JsonProperty
        var username : String,
        @get:JsonProperty
        var password : String
)
class AdvanceAuthResponse() {
    @get:JsonProperty
    lateinit var token : String
}

class AdvanceUserRespose() {
    lateinit var user : AdvanceUser
}

class AdvanceUser() {
    lateinit var emailAddress : String
}

class AdvanceModifiedBookingsResponse() {
    var bookingIds = emptyList<Int>()
}

class AdvanceReservationResponse() {    
    var booking : AdvanceReservation = AdvanceReservation()
}

class AdvanceReservation() {    
    var addonSelections = emptyList<AdvanceAddOnSelection>()    
    var agentCode : String? = null    
    var bookingCode = ""    
    var bookingDateTime = DateTime.now()    
    var bookingID  = -1    
    var bookingItems = emptyList<AdvanceBookingItem>()         
    var bookingStatus = ""
    var channelCode = ""    
	var currencyCode = ""    
	var customer = AdvanceCustomer()    
	var lineTotals = emptyList<AdvanceLineTotal>()    
	var merchantID = -1
	var officeNotes = ""    
	var orderTakerID = -1    
	var payments = emptyList<AdvancePayment>()    
	var pricing = AdvancePricing()    
	var promotionVoucherCodes: String? = null
	var salesChannel = AdvanceSalesChannel()    
	var status = ""    
	var taxExempt = false    
	var termsAndConditions1Agreed = false    
	var uuid = ""
}

class AdvanceAddOnSelection() {
    var addonID = -1
	var name = ""
    var options = emptyList<AdvanceAddOnOption>()
}

class AdvanceAddOnOption {
    var label :String? = ""
    var optionID = -1
    var price :BigDecimal? = null
}

class AdvanceBookingItem() {
    var availabilityID = -1
    var bookingItemID = -1
    var noPersons = emptyList<Int>()
    var productId = -1
	var addonSelections = emptyList<AdvanceAddOnSelection>()
    var ticketCodes = emptyList<AdvanceTicketCode>()
    var lineTotals = emptyList<AdvanceLineTotal>()
	var startDateTime = DateTime.now()
	var endDateTime = DateTime.now()
}

class AdvanceTicketCode() {
    var ticketCodeID = -1
    var code = ""
    var status = ""
    var personCategoryIndex = -1
}

class AdvanceLineTotal() {
    var addonID : Int? = null
    var type = -1
    var label = ""
    var quantity = -1
    var price = BigZero
    var unitPrice : BigDecimal? = null
}

class AdvanceCustomer() {
    var customerID = -1
    var address1: String? = null
    var address2: String? = null
    var emailAddress: String? = null
    var firstName: String? = null
    var lastName: String? = null
    var state: String? = null
    var country: String? = null
}

class AdvancePayment() {
    
}

class AdvancePricing() {
    var baseAmount = BigZero
    var totalAmount = BigZero
    var priceAdjustments = emptyList<AdvancePriceAdjustment>()
}

class AdvancePriceAdjustment(
        var amount : BigDecimal = BigZero,
        var label : String = "",
        var promotionID : Int? = null,
        var type : Int = 0
)

class AdvanceSalesChannel() {
    
}

//Ex: from http://galaxy-elbert.zozi.com/services/api20/merchants/34100/calendars/54715823
class AdvanceAvailabilityResponse() {
    var calendarEntry = AdvanceAvailability()
}

class AdvanceAvailability() {
    var availabilityID = -1
    var maxCapacity = -1
    var startDateTime = DateTime.now()
    var endDateTime = DateTime.now()
    var productID = -1
}

//Person Categories
class AdvancePersonCategoryResponse() {
    var personCategories = emptyList<AdvancePersonCategory>()
}

class AdvancePersonCategory() {
    var personCategoryIndex = -1
    var label : String? = ""
    var categoryDescription = ""
}

class AdvanceProductsReponse() {
    var products = emptyList<AdvanceProduct>()
}

class AdvanceProduct() {
    var productID = -1
    var name = ""
    var shortDescription = ""
}

class AdvancePromotionResponse() {
    var promotion = AdvancePromotion()
}

class AdvancePromotion() {
    var promotionCode = ""
    var description : String? = ""
    var calcbasis = "perperson"
    var associations = emptyList<AdvancePromotionAssociation>()
    var personCategories = emptyList<AdvancePromotionPersonCategory>()
}

class AdvanceAddOnResponse() {
    var addon = AdvanceAddOn()
}

class AdvanceAddOn() {
    var priceCalcBasis = ""
}

class AdvancePromotionPersonCategory() {
    var label : String? = null
    var personCategoryIndex: Int? = null
    var selected = false
}

class AdvancePromotionAssociation() {
    var label: String = ""
    var productID: Int? = null
    var selected = false
}
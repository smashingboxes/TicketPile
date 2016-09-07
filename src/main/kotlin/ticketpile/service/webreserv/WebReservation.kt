package ticketpile.service.webreserv

import org.joda.time.DateTime
import java.io.Serializable
import java.math.BigDecimal

/**
 * Input POJO type for WebReserv API20 /bookings/{bookingId} requests.
 * 
 * Created by jonlatane on 8/28/16.
 */
class WRReservationResponse() : Serializable {    
    var booking : WebReservation = WebReservation()
}

class WebReservation() {    
    var addonSelections = emptyList<WRAddOnSelection>()    
    var agentCode : String? = null    
    var bookingCode = ""    
    var bookingDateTime = DateTime.now()    
    var bookingID  = -1    
    var bookingItems = emptyList<WRBookingItem>()         
    var bookingStatus = ""
    var channelCode = ""    
	var currencyCode = ""    
	var customer = WRCustomer()    
	var lineTotals = emptyList<WRLineTotal>()    
	var merchantID = -1    
	var officeNotes = ""    
	var orderTakerID = -1    
	var payments = emptyList<WRPayment>()    
	var pricing = WRPricing()    
	var promotionVoucherCodes = ""    
	var salesChannel = WRSalesChannel()    
	var status = ""    
	var taxExempt = false    
	var termsAndConditions1Agreed = false    
	var uuid = ""
}

class WRAddOnSelection() {
    var addonId = -1
	var name = ""
}

class WRBookingItem() {
    var availabilityID = -1
    var bookingItemID = -1
    var noPersons = emptyList<Int>()
    var productId = -1
	var addonSelections = emptyList<WRAddOnSelection>()
    var ticketCodes = emptyList<WRTicketCode>()
    var lineTotals = emptyList<WRLineTotal>()
	var startDateTime = DateTime.now()
	var endDateTime = DateTime.now()
}

class WRTicketCode() {
    var ticketCodeID = -1
    var code = ""
    var status = ""
    var personCategoryIndex = -1
}

class WRLineTotal() {
    var addonID : Int? = null
    var type = -1
    var label = ""
    var quantity = -1
    var price = BigDecimal.ZERO
    var unitPrice : BigDecimal? = null
}

class WRCustomer() {
    
}

class WRPayment() {
    
}

class WRPricing() {
    
}

class WRSalesChannel() {
    
}

//Ex: from http://galaxy-elbert.zozi.com/services/api20/merchants/34100/calendars/54715823
class WRAvailabilityResponse() {
    var calendarEntry = WRAvailability()
}

class WRAvailability() {
    var availabilityID = -1
    var maxCapacity = -1
    var startDateTime = DateTime.now()
    var endDateTime = DateTime.now()
    var productID = -1
}

//Person Categories
class WRPersonCategoryResponse() {
    var personCategories = emptyList<WRPersonCategory>()
}

class WRPersonCategory() {
    var personCategoryIndex = -1
    var label : String? = ""
    var categoryDescription = ""
}

class WRProductsReponse() {
    var products = emptyList<WRProduct>()
}

class WRProduct() {
    var productID = -1
    var name = ""
    var shortDescription = ""
}
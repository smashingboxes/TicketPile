package ticketpile.service.advance

import org.joda.time.DateTime
import java.io.Serializable
import java.math.BigDecimal

/**
 * Input POJO types for Advance API20 requests.
 * 
 * Created by jonlatane on 8/28/16.
 */
class AdvanceReservationResponse() : Serializable {    
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
	var promotionVoucherCodes = ""    
	var salesChannel = AdvanceSalesChannel()    
	var status = ""    
	var taxExempt = false    
	var termsAndConditions1Agreed = false    
	var uuid = ""
}

class AdvanceAddOnSelection() {
    var addonId = -1
	var name = ""
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
    var price = BigDecimal.ZERO
    var unitPrice : BigDecimal? = null
}

class AdvanceCustomer() {
    
}

class AdvancePayment() {
    
}

class AdvancePricing() {
    
}

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
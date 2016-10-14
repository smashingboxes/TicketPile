package ticketpile.service.graphql

import graphql.GraphQL
import graphql.Scalars.*
import graphql.schema.*
import graphql.schema.GraphQLFieldDefinition.newFieldDefinition
import graphql.schema.GraphQLObjectType.newObject
import org.joda.time.DateTime
import ticketpile.service.model.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The schema for GraphQL access to TicketPile.
 * 
 * Created by jonlatane on 10/11/16.
 */
val TicketPileGraphQL : GraphQL by lazy {
    GraphQL(TicketPileGraphQLSchema)
}
val TicketPileGraphQLSchema : GraphQLSchema by lazy {
    val queryRoot: GraphQLObjectType =
        newObject().name("QueryRoot")
        .field(newFieldDefinition().type(GraphQLTypeReference("BookingsResult"))
            .name("bookings")
            .argument(GraphQLArgument("locations",
                    "Locations to search in", GraphQLList(GraphQLInt), listOf(-1)))
            .argument(GraphQLArgument("withEventsAfter",
                    "Limit bookings to those with events on a range of dates", GraphQLString, null))
            .argument(GraphQLArgument("withEventsBefore",
                    "Limit bookings to those with events on a range of dates", GraphQLString, null))
            .argument(GraphQLArgument("id",
                    "An Advance booking ID to search for", GraphQLInt, null))
            .argument(GraphQLArgument("code",
                    "A booking code to search for", GraphQLString, null))
            .argument(GraphQLArgument("status",
                    "Find bookings by status", GraphQLList(GraphQLString), emptyList<String>()))
            .argument(GraphQLArgument("limit",
                    "How many bookings to find", GraphQLInt, 50))
            .argument(GraphQLArgument("offset",
                    "Offset for the limit parameter", GraphQLInt, 0))
            .dataFetcher {
                    BookingQuery(
                        locations = it.arguments["locations"] as List<Int>,
                        eventsBefore = if (it.arguments["withEventsBefore"] != null)
                            DateTime(it.arguments["withEventsBefore"] as String)
                        else null,
                        eventsAfter = if (it.arguments["withEventsAfter"] != null)
                            DateTime(it.arguments["withEventsAfter"] as String)
                        else null,
                        code = it.arguments["code"] as String?,
                        id = it.arguments["id"] as Int?,
                        status = it.arguments["status"] as List<String>,
                        limit = Math.min(it.arguments["limit"] as Int, 200),
                        offset = it.arguments["offset"] as Int
                    )
            }.build())
        .build()

    val bookingsResult: GraphQLObjectType = 
        newObject().name("BookingsResult")
        .field(newFieldDefinition().type(GraphQLInt)
            .name("totalCount")
            .description("The total number of bookings returned by the query")
            .dataFetcher {
                (it.source as BookingQuery).totalCount
            }.build())
        .field(newFieldDefinition().type(GraphQLBigDecimal)
            .name("pageTotal")
            .description("The total gross for bookings on this page (defined by limit and offset)")
            .dataFetcher {
               (it.source as BookingQuery).pageTotal
            }
            .build())
        .field(newFieldDefinition().type(GraphQLBigDecimal)
            .name("totalAmount")
            .description("The total gross for all bookings found in this search")
            .dataFetcher {
                (it.source as BookingQuery).totalAmount
            }
            .build())
        .field(newFieldDefinition().type(GraphQLList(GraphQLTypeReference("Booking")))
            .name("results")
            .description("A list of bookings that matched the query")
            .dataFetcher {
                (it.source as BookingQuery).results
            }
            .build())
        .field(newFieldDefinition().type(GraphQLInt)
            .name("limit")
            .description("The booking limit of the query")
            .dataFetcher {
                (it.source as BookingQuery).limit
            }.build())
        .field(newFieldDefinition().type(GraphQLInt)
            .name("offset")
            .description("The offset for the limit of the query")
            .dataFetcher {
                (it.source as BookingQuery).offset
            }.build())
        .build()

    val booking: GraphQLObjectType =
        newObject().name("Booking")
            .field(newFieldDefinition().type(GraphQLInt)
                .name("id")
                .description("The Advance Booking ID for this booking")
                .dataFetcher {
                    (it.source as Booking).externalId!!
                }
                .build())
            .field(newFieldDefinition().type(GraphQLString)
                .name("code")
                .dataFetcher {
                    (it.source as Booking).code
                }
                .build())
            .field(newFieldDefinition().type(GraphQLString)
                .name("status")
                .dataFetcher {
                    (it.source as Booking).status
                }
                .build())
            .field(newFieldDefinition().type(GraphQLBigDecimal)
                .name("totalAmount")
                .dataFetcher {
                    (it.source as Booking).bookingTotal!!
                }
                .build())
            .field(newFieldDefinition().type(GraphQLList(GraphQLTypeReference("BookingItem")))
                .name("items")
                .dataFetcher {
                    (it.source as Booking).items
                }
                .build())
            .field(newFieldDefinition().type(GraphQLList(GraphQLTypeReference("DiscountAdjustment")))
                .name("discounts")
                .dataFetcher {
                    (it.source as Booking).discounts
                }
                .build())
            .field(newFieldDefinition().type(GraphQLList(GraphQLTypeReference("AddOnAdjustment")))
                .name("addOns")
                .dataFetcher {
                    (it.source as Booking).addOns
                }
                .build())
            .field(newFieldDefinition().type(GraphQLList(GraphQLTypeReference("ManualAdjustment")))
                .name("manualAdjustments")
                .dataFetcher {
                    (it.source as Booking).manualAdjustments
                }
                .build())
            .field(newFieldDefinition().type(GraphQLList(GraphQLTypeReference("FeeAdjustment")))
                .name("fees")
                .dataFetcher {
                    (it.source as Booking).fees
                }
                .build())
        .build()
    
    val discountAdjustment: GraphQLObjectType =
        newObject().name("DiscountAdjustment")
            .field(newFieldDefinition().type(GraphQLInt)
                .name("promotionId")
                .dataFetcher {
                    (it.source as DiscountAdjustment<*>).discount.externalId!!
                }
                .build())
            .field(newFieldDefinition().type(GraphQLString)
                .name("code")
                .dataFetcher {
                    (it.source as DiscountAdjustment<*>).discount.name
                }
                .build())
            .field(newFieldDefinition().type(GraphQLString)
                .name("description")
                .dataFetcher {
                    (it.source as DiscountAdjustment<*>).discount.description
                }
                .build())
            .field(newFieldDefinition().type(GraphQLBigDecimal)
                .name("amount")
                .dataFetcher {
                    (it.source as DiscountAdjustment<*>).amount
                }
                .build())
            .build()



    val addOnAdjustment: GraphQLObjectType =
        newObject().name("AddOnAdjustment")
            .field(newFieldDefinition().type(GraphQLString)
                .name("name")
                .dataFetcher {
                    (it.source as AddOnAdjustment<*>).addOn.name
                }
                .build())
            .field(newFieldDefinition().type(GraphQLString)
                .name("selection")
                .dataFetcher {
                    (it.source as AddOnAdjustment<*>).selection
                }
                .build())
            .field(newFieldDefinition().type(GraphQLBigDecimal)
                .name("amount")
                .dataFetcher {
                    (it.source as AddOnAdjustment<*>).amount
                }
                .build())
            .build()

    val manualAdjustment: GraphQLObjectType =
        newObject().name("ManualAdjustment")
            .field(newFieldDefinition().type(GraphQLString)
                .name("description")
                .dataFetcher {
                    (it.source as ManualAdjustment<*>).description
                }
                .build())
            .field(newFieldDefinition().type(GraphQLBigDecimal)
                .name("amount")
                .dataFetcher {
                    (it.source as ManualAdjustment<*>).amount
                }
                .build())
            .build()

    val feeAdjustment : GraphQLObjectType =
        newObject().name("FeeAdjustment")
            .field(newFieldDefinition().type(GraphQLString)
                .name("description")
                .dataFetcher {
                    (it.source as FeeAdjustment<*>).description
                }
                .build())
            .field(newFieldDefinition().type(GraphQLBigDecimal)
                .name("amount")
                .dataFetcher {
                    (it.source as FeeAdjustment<*>).amount
                }
                .build())
            .build()

    val bookingItem: GraphQLObjectType = newObject().name("BookingItem")
        .field(newFieldDefinition().type(GraphQLList(GraphQLTypeReference("Ticket")))
            .name("tickets")
            .dataFetcher {
                (it.source as BookingItem).tickets
            }
            .build())
        .field(newFieldDefinition().type(GraphQLList(GraphQLTypeReference("AddOnAdjustment")))
            .name("addOns")
            .dataFetcher {
                (it.source as BookingItem).addOns
            }
            .build())
        .build()
    
    val ticket: GraphQLObjectType = newObject().name("Ticket")
        .field(newFieldDefinition().type(GraphQLString)
            .name("code")
            .dataFetcher {
                (it.source as Ticket).code
            }
            .build())
        .field(newFieldDefinition().type(GraphQLBigDecimal)
            .name("basePrice")
            .dataFetcher {
                (it.source as Ticket).basePrice
            }
            .build())
        .field(newFieldDefinition().type(GraphQLBigDecimal)
            .name("discountedPrice")
            .dataFetcher {
                (it.source as Ticket).discountedPrice
            }
            .build())

        .field(newFieldDefinition().type(GraphQLBigDecimal)
            .name("totalAmount")
            .dataFetcher {
                (it.source as Ticket).grossAmount!!
            }
            .build())
        .build()
    
    // Create the schema
    (GraphQLSchema.newSchema()
        .query(queryRoot))
        .build(setOf(
            bookingsResult, 
            booking, 
            discountAdjustment,
            manualAdjustment,
            feeAdjustment,
            addOnAdjustment,
            bookingItem,
            ticket
        ))
    //GraphQL(schema)
}
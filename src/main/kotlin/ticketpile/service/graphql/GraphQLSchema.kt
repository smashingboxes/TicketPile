package ticketpile.service.graphql

import graphql.GraphQL
import graphql.Scalars.GraphQLInt
import graphql.Scalars.GraphQLString
import graphql.schema.*
import graphql.schema.GraphQLFieldDefinition.newFieldDefinition
import graphql.schema.GraphQLObjectType.newObject
import org.joda.time.DateTime
import ticketpile.service.model.Booking
import ticketpile.service.model.BookingItem
import ticketpile.service.model.Ticket
import ticketpile.service.security.ApplicationUser

/**
 * The schema for GraphQL access to TicketPile.
 * 
 * Created by jonlatane on 10/11/16.
 */
fun createGraphQL(user : ApplicationUser) : GraphQL {
    return GraphQL(createGraphQLSchema(user))
}
private fun createGraphQLSchema(user : ApplicationUser) : GraphQLSchema {
    val queryRoot: GraphQLObjectType =
        newObject().name("QueryRoot")
        .field(newFieldDefinition().type(GraphQLTypeReference("BookingSearch"))
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
                    @Suppress("UNCHECKED_CAST")
                    BookingSearch(
                        locations = (it.arguments["locations"] as List<Int>)
                                .intersect(user.locations).toList(),
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
        newObject().name("BookingSearch")
        .field(newFieldDefinition().type(GraphQLInt)
            .name("totalCount")
            .description("The total number of bookings returned by the query")
            .dataFetcher {
                (it.source as BookingSearch).totalCount
            }.build())
        .field(newFieldDefinition().type(GraphQLBigDecimal)
            .name("pageTotal")
            .description("The total gross for bookings on this page (defined by limit and offset)")
            .dataFetcher {
               (it.source as BookingSearch).pageTotal
            }
            .build())
        .field(newFieldDefinition().type(GraphQLList(GraphQLTypeReference("Booking")))
            .name("results")
            .description("A list of bookings that matched the query")
            .dataFetcher {
                (it.source as BookingSearch).results
            }
            .build())
        .field(newFieldDefinition().type(GraphQLInt)
            .name("limit")
            .description("The booking limit of the query")
            .dataFetcher {
                (it.source as BookingSearch).limit
            }.build())
        .field(newFieldDefinition().type(GraphQLInt)
            .name("offset")
            .description("The offset for the limit of the query")
            .dataFetcher {
                (it.source as BookingSearch).offset
            }.build())
        .weighableFields("BookingSearch")
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
            .weighableFields("Booking")
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
        .weighableFields("BookingItem")
        .build()
    
    val ticket: GraphQLObjectType = newObject().name("Ticket")
        .field(newFieldDefinition().type(GraphQLString)
            .name("code")
            .dataFetcher {
                (it.source as Ticket).code
            }
            .build())
        .weighableFields("Ticket")
        .build()
    
    // Create the schema
    return (GraphQLSchema.newSchema()
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
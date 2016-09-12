## TicketPile

TicketPile is a very basic Spring + Kotlin + Exposed DAO/MySQL app
designed for importing booking data from sources such as WebReserv,
Ovation, and Sisense and comparing them, or for importing booking 
into a highly-normalized database suitable for easy usage in SiSense,
MS Power BI, or most other BI tools.

You must

To run and access the Swagger API:

```
$ ./gradlew run
$ open https://localhost:8443/swagger-ui.html
```
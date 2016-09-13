## TicketPile

TicketPile is a very basic Spring + Kotlin + Exposed DAO/MySQL app
designed for importing booking data from sources such as WebReserv,
Ovation, and Sisense and comparing them, or for importing booking 
into a highly-normalized database suitable for easy usage in SiSense,
MS Power BI, or most other BI tools.

#### Database configuration
Open `db.properties` and update the host to point to your MySQL database
and your username and pasword.  You may need to `mysql -u root -p` and
then `create database ticketpile;` so the application has a database to
connect to.

#### Run and access Swagger API:

To run and access the Swagger API:

```
$ ./gradlew run & open https://localhost:8443/swagger-ui.html
```

Gradle will download dependencies and run the application.  It will
connect to your database, create all necessary tables and indexes, 
and provide an admin auth token.

Once Spring finishes booking, you should be able to see an "insecure
certificate" error in the browser window you just opened, which you 
will need to ignore.

Now, simply paste the auth token into the top-right text box in the
browser and hit "Explore" as below.  You should be able to access all
endpoints documented without hitting 403 errors!

![Swagger Screenshot](/docs/swagger-screenshot.png)
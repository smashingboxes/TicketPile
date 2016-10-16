# High Risk ZOZI Merchants

See [this Google Doc](https://docs.google.com/spreadsheets/d/16JhqR0JMmbmjOnLtSwd_DexxucZ_txUHmeLHmadOzHY/edit#gid=0) 
for a running list of High-Risk Merchants.

Let's get those with reporting concerns up and running on TicketPile, with or without Sisense.

To find some users for these companies you can use to sync TicketPile, use the following query:

```
select c.companyID, c.name as `Company Name`, l.locationID, u.userID, u.UserName, u.Password 
from users u 
  join company c on u.CompanyID = c.CompanyID
  join useraccess ua on u.UserID = ua.UserID
  join location l on ua.LocationID = l.locationid
WHERE
  (u.Enabled = 1 and u.status = 1)
  AND
  (
    c.name LIKE '%bridgewater%'
    OR c.name LIKE '%bernard%'
    OR c.name LIKE '%sandland%'
    OR c.name LIKE '%twin cities inflatables%'
    OR c.name LIKE '%oasis water%'
    -- OR c.name LIKE '%door%trolley'
    -- OR c.name LIKE '%bright%angel%'
    -- OR c.name LIKE '%valley%zipline%'
    -- OR c.name LIKE '%sand%highway%'
    OR c.name LIKE '%9/11%tribute%'
    OR c.name LIKE '%go%paddle%'
    -- OR c.name LIKE '%montlake%'
    -- OR c.name LIKE '%duffy%san%diego'
    -- OR c.name LIKE '%starkey%'
  )
order by c.companyID, l.locationID, u.userID;
```
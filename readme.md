Disclaimer: Read [Design](#Design) section for explanation on how I designed the application and the 
intentions for deployment.

The rest of this file is primarily for personal breaking down of the task, however it could still be of some use
to the assessors.

---
## Task Breakdown (For personal use)

Micro-service that collates financial products from a small selection of partners.

_expose a single endpoint that consumes some information about the userʼs financial situation
and return credit cards recommended for them, sorted based on their eligibility and the cardsʼ APR
(annual percentage rate)._

Request:

POST /creditcards
body: CreditCardRequest (required)
CreditCardRequest:
```
{
    name: String
    creditScore: Integer
    salary: Integer
}
```


Response: [CreditCard]
```
[
    {
        "provider": "string",
        "name": "string",
        "apr": 0,
        "cardScore": 0
    }
]
```


### Logic

- Downstream endpoints will return an eligibility rating and an APR for each card (list of cards)
- results should be sorted using
`sortingScore = eligibility ∗ ((1/apr)^2)`

- Both endpoints return the same data but handled differently

1. CSCards
https://app.clearscore.com/api/global/backend-tech-test/v1/doc/

- requires name & creditscore **only**
- returns `cardName`, `apr` and `eligibility`
  - eligibility is from 0.0 to 10.0


2. ScoredCards
https://app.clearscore.com/api/global/backend-tech-test/v2/doc/

- requires name & creditscore **AND salary**
- returns `cardName`, `apr` and `approvalRating`
    - approvalRating is from 0.0 to 1.0


## Key things to look for + how to solve them

1. documented code with a coherent project structure
2. 2.products being returned in a timely manner
3. behaviour when upstream APIs returning errors or taking too long to respond
4. unit tests (we're huge fans of TDD!)

### Include

- `start.sh` script 
  - Support following **environment** variables
    - `HTTP_PORT`             - The port to expose your service on
    - `CSCARDS_ENDPOINT`      - The url for CSCards
    - `SCOREDCARDS_ENDPOINT`  - The url for ScoredCards
- a short README outlining how youʼve designed your service and how youʼd
  intend to deploy it (THIS)

---


# Design

### Key assumption
Both downstream API calls return an `eligibility` / `approvalRating`, however the former is valued from 0.0
to 10.0 and the latter from 0.0 to 1.0. I have assumed that both these values represent the same thing for
our business logic, specifically sorting, and purely have a different scale. That is to say, that an `eligibility`
of 5.0 is functionally the same as an  `approvalRating` of 0.5, and that they should be normalised to the same
value.


### Project structure

As this is a simple microservice containing a server and a downstream client with some additional business logic,
I believe it makes sense to divide the code into corresponding parts.
- App
  - Responsible for spinning up the app and importing environment variables
- Routes
  - Responsible for processing incoming request(s) and sending the data down to the service
- Service
  - Responsible for handling all business logic
- Client
  - Responsible or fetching downstream data and converting it into a domain model.

The only contentious point regarding this structure for me is the Client handling the conversion of data into a domain model.
It could be argued the client should't know what the domain model is and is purely responsible for making client calls, 
however I believe it makes logical sense that the service layer shouldn't know about the formatting of the downstream data
so shouldn't be forced to perform that conversion itself.

This structure also gives us independent models at different layers, with the edge layers responsible for converting their
data into the consistent domain model in the center (the service).

### TDD

My approach to TDD:
- Consider errors we want to catch
- Consider all happy paths
- Write out a loose structure of the code, using `???` unimplemented to give us an outline of the methods we want to be testing
- Decide on testing plan
    - For this Project, I believe a combination of Feature and Unit testing should be sufficient
      - Feature testing - testing the routes and service interaction
      - Unit testing - testing the service logic
- Build tests iteratively
  - Start by writing a test for something very small, in this case probably that a request to the right endpoint returns
    a 200 response.

### Errors
Important consideration:
- _behaviour when upstream APIs returning errors or taking too long to respond_

Specifically regarding slow responses, that can be handled in a few ways
- Simply setting timeouts on client calls
- Caching 
  - Simple to implement but wouldn't have a huge impact
- Circuit breaking
- Fallback response
  - Seems inappropriate for this kind of data, in which correctness is very important (personal financial decisions) and 
  coming up with a graceful response would be difficult given how the data changes with each user
- Background jobs
  - Too complex for a task like this, and would require API or infrastructure changes
- retry & exponential backoff
- load balancing

For this task I believe client timeouts are a simple and effective solution. Timeouts would stop the downstream service
, which might never respond, from blocking the server. Caching would provide limited value as it is unlikely a user will need 
to repeat requests, and the requests require username as a parameter, so multiple users with the same creditscore would return
the same data, but would be tricky to cache together.

I think its more important in this instance, if we imagine this is a real-world task, to ensure correctness of data. The best way to solve it
would be a combination of all of the above methods, and return the result asynchronously. This could be done by triggering a background process
(that would wait for the api to succeed, probably with retry & exponential backoff) and then having a different endpoint which
would open up an SSE stream. This endpoint would require some process ID which would be returned in our initial error response to 
the caller of this api.

In summary, I will be implementing general client timeouts, with an arbitrary value which would be refined in the real world.


#### Other errors

- Client error
  - credit score must be between 0 and 700 inclusively
  - This will be caught and thrown in the service layer as that property belongs to the creditscore type
- Service error


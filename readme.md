"a short README outlining how youʼve designed your service and how youʼd
intend to deploy it"

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

### Key assumption 1
Both downstream API calls return an `eligibility` / `approvalRating`, however the former is valued from 0.0
to 10.0 and the latter from 0.0 to 1.0. I have assumed that both these values represent the same thing for
our business logic, specifically sorting, and purely have a different scale. That is to say, that an `eligibility`
of 5.0 is functionally the same as an  `approvalRating` of 0.5, and that they should be normalised to the same
value, which is between 0.0 and 100.0.

### Key assumption 2

The card score values should be **truncated** to 3 decimal places. The examples in the assignment calculate values such as
0.212 wheras the true value is 0.212562...

### Code style

I have chosen to write this app in tagless final, and inject IO at the edge layers and tests. I won't get into a discussion about
the pros and cons of this style, as there are many and I sit pretty neutral on the matter. The big disadvantage for me can be
ease of use and readability. The main reason for me using tagless here is that this is a coding exercise and I like to challenge
myself, and not relying on IO is a harder challenge in my opinion, and encourages better coding practices.

My key point here is that I'm not necessarily a hard believer in Tagless Final as a style and I'm perfectly happy and comfortable
writing in IO. In essence wanted to demonstrate my ability in this exercise.

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

### Considerations

While there are only two clients to fetch data from now, it is likely in the real world this would be a lot higher. As a result,
I've decided to explicity separate logic surrounding both of them, where appropriate. In practice this means that some logic is duplicated
where it could be combined into shared logic, for example normalising the eligibility ratings of the two sets of cards. The fact that
the data is similarly structured doesn't mean they should be coupled in the code. Normalisation, for example, for other card providers
may require different logic. It could be moved, but the code should be separated.

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
the same data, but would be tricky to cache together. While its not obvious why the downstream clients require the username
as a parameter, it would be wrong to assume that its unnecessary and create our own cache without it.

I think its more important in this instance, if we imagine this is a real-world task, to ensure correctness of data. The best way to solve it
would be a combination of all of the above methods, and return the result asynchronously. This could be done by triggering a background process
(that would wait for the api to succeed, probably with retry & exponential backoff) and then having a different endpoint which
would open up an SSE stream. I believe this is too complex for this exercise and would distract from the key challenge I am solving.

Another option would be a dynamic timeout value, which increases and decreases as per the rate of timeout exceptions, to reduce the 
waiting time if a downstream call is notoriously slow. Again, I believe this is too complex for this exercise but in a real world situation
could be appropriate.

In the real world, my approach to solving this would be first establishing why the downstream calls take too long, and use that
to come up with a solution. For example if they were under high load, then caching and rate limiting on our side would mitigate
this problem. Naturally, this isn't possible for this task.

For this exercise I will be implementing general client timeouts, with an arbitrary value.


#### Other errors

- Client error
  - credit score must be between 0 and 700 inclusively
  - This will be caught and thrown in the service layer as that property belongs to the creditscore type


## Deployment

To deploy this application, I would:
- Containerise using docker
- Deploy multiple containers with a load balancer to distribute traffic between nodes
  - Preferred deployment option would be Kubernetes
    - deploy in a cluster for scaling and resilience
    - Probably in combination with AWS
- Deploy via a CI/CD pipeline
  - Write and run tests before deployment
    - application tests (written in scala)
    - e2e tests
    - integration tests
  - Deploy to a staging environment and test manually before deploying to a production environment
- Environment configuration
  - Use something like Kubernetes ConfigMaps or AWS Secrets Manager to manage to environment configuration 
- Observability:
  - Implement metrics
    - e.g. prometheus metrics
  - Implement tracing
    - Can be done at the application level using libraries such as Natchez
- Use a tool like grafana to visualise these distributed logs/traces/metrics

---

## To Run

### Pre-requisites
- Java version: 1.8
- sbt 1.10.1
- bash 
### Run
Environment variables - either:
- Export manually
- If on linux, run `source environments/source_linux.sh`

`sbt run` or `./run.sh`





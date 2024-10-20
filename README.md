# Lottery System

A simple lottery system, based on `cats-effect`, `http4s`, `circe`, `tapir`, and `doobie`.

Use a containerized `PostgreSQL` database as a storage.

### How to use the service
#### Swagger Documentation
```
GET docs/
```
#### Enroll a new principal:
```
POST /principals
```
Return a `UUID` to use a bearer token to identify yourself.

##### Put a ballot as a principal on the lottery:
```
POST /lotteries/{lotteryId}/ballot
```
Need a `UUID` as a bearer token to authenticate yourself.

Return `201` if successful, `403` if the lottery is closed.

#### Get the list of lotteries:
```
GET /lotteries
```
Return a list of lotteries, accept `endDate=year-month-day` or `status=closed | open`
as query param to filter the results.

#### Open a new lottery:
```
POST /admin/lotteries
```
Need the `ApiKey` as a bearer token to authenticate yourself.

Take a date in the ISO-8601 format (`year-month-day`) as Json string in input.

Return `201` if successful.

#### Close a lottery:
```
PUT /admin/lotteries/{lotteryId}/close
```
Need the `ApiKey` as a bearer token to authenticate yourself.

Return `200` if successful, `403` if the lottery is closed
or there are no ballots for the lottery.

#### ApiKey
In order to identify yourself as admin you need the environment variable `API_KEY`.
By default, is read from the `.env` file but if the variable is invalid or not found
the system generate a one time `ApiKey` for the session.

## Start the environment
Use the `.data/` folder as permanent storage.
```shell
docker compose up
```

## Run application

```shell
sbt run
```

## Run tests
Run both tests and integration test.
```shell
sbt test
```

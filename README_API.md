# API Basics Lab: Alex books a study room

A small hands-on project for learning what an API is, how HTTP requests and responses work, and what separates a well-written HTTP API from a badly written one. You start a server in one terminal, send requests to it with `curl` from another terminal, and read what comes back.

Needs a JDK 11 or newer and `curl` (already installed on macOS and most Linux). No libraries.

```sh
# go into the project folder so the next commands find the files
cd api-basics-lab

# create a folder called out/ for the compiled classes
# (-p: no error if it already exists)
mkdir -p out

# compile every .java file
# (-d out: put the resulting .class files in out/)
javac -d out src/main/java/*.java
```

Or run the whole tour at once with:

```sh
# compile, start both servers, send every example request with curl,
# then stop the servers
sh demo.sh
```

## What each file does

All the `.java` files below live in `src/main/java/`. `demo.sh` stays in the project folder.

### The engine

`RoomService.java` holds the one real piece of business logic, `reserve(ucid, room)`. It knows two rooms, MS-101 and MS-102. A known room gives back a Booking with a new id (R-1, R-2, ...). An unknown room throws an exception. It has no idea HTTP exists.

`Booking.java` is the result object: an id, a room and a status. It is plain data.

### The three steps

`Step1_LibraryApi.java` is a library API. Its main method creates a RoomService and calls reserve directly, then prints the returned object and catches the exception for a bad room. No server, no network.

`Step2_BadServer.java` is a web server on port 8080 that puts HTTP in front of reserve, written badly. It answers 200 OK for success and for failure. It exists to show you what not to do.

`Step3_GoodServer.java` is a web server on port 8081 that does the same job properly. Booking is a POST with the details in a JSON body. Its endpoints are POST /v1/reservations, POST /v2/reservations and GET /v1/reservations/{id}. It uses honest status codes (201, 400, 404, 405), JSON error messages, and versioned paths.

### Helpers

`Json.java` has one tiny function, quote, so the servers can build simple JSON without a library.

`demo.sh` compiles everything, starts both servers, sends every example request with curl, then stops the servers. It is the one-command version of this guide.

### The caller is curl

There is no client program. You type the requests yourself in a terminal, which is exactly what a web page or another service does in code.

```
Step1_LibraryApi ─────────────────────────┐
curl ──► Step2_BadServer  (port 8080) ────┼──► RoomService ──► Booking
curl ──► Step3_GoodServer (port 8081) ────┘
```

Every step ends up calling RoomService.reserve. Only the door in front of it changes.

## 0. The idea

An **API** (application programming interface) is the agreed way for one piece of code to ask another piece of code to do something. You send a request in an agreed shape and you get a result back in an agreed shape.

The story used throughout: **Alex** is a student with ucid `10000001`. Alex wants to book study room **MS-101**. Every call in this project uses that booking.

The real work is one Java method:

```
RoomService.reserve(ucid, room)  ->  Booking
```

It returns a `Booking` (id, room, status) or throws an exception if the room doesn't exist. It knows nothing about the web. What changes from step to step is only the **door** you use to reach it:

| | Library API | HTTP API |
|---|---|---|
| Who calls | Code in the **same process** | A program in **another process** (separate terminal, device, web page) |
| How | A Java method call | A request over HTTP |
| What comes back | A Java object | An HTTP response: a **status** and a **body** |
| Failure looks like | An exception | A status code (and a message in the body) |
| Example | `service.reserve(...)` | `GET https://api.github.com/...` |

Why do we need the second door? A web page cannot call a Java method that lives in someone else's program. So we put HTTP in front of the method. The server receives the request, calls `reserve`, and turns the result into a response. The caller never sees the Java object, only an HTTP response that tells it what happened.

## 1. Library API: run Step1

Same process, no server, no network.

**1. Compile once.** From `api-basics-lab`. Skip this if `out/` already exists.

```sh
javac -d out src/main/java/*.java
# Turns all the .java source files into .class files that Java can run,
# and writes them into out/.
```

**2. Run Step1.**

```sh
java -cp out Step1_LibraryApi
# Starts the JVM, tells it to look for classes in out/ (-cp means classpath),
# and runs the main method of Step1_LibraryApi.
# That method calls reserve directly and prints the result.
```

**3. Read the lines.** The first is the `Booking` the method returned. The second is the exception for the room `NO-SUCH`.

```
Got back a Java object: Booking{id=R-1, room=MS-101, status=confirmed}
Caught: no bookable room NO-SUCH
```

The request was Alex's ucid plus the room code. The return was one `Booking` with an id (R-1). A library API reports failure by throwing.

## 2. HTTP basics and the `curl` command

```
 curl ──GET /api/reserve?ucid=..&room=..──► Step2_BadServer  (8080)
                                              always answers 200
                                                      │
 curl ──POST /v1/reservations  + JSON body──► Step3_GoodServer (8081)
 curl ──POST /v2/reservations  + JSON body──►   also GET /v1/reservations/{id}
 curl ──GET  /v1/reservations/R-1───────────►   and keeps bookings in memory
                                                      │
                                                      ▼
                                      RoomService.reserve() -> Booking

 curl runs outside the server process. reserve runs inside it.
```

Vocabulary you need for the next sections:

- **Request**: a method (GET or POST), a path (`/v1/reservations`), optional parameters, headers, and sometimes a body.
- **Query string**: parameters written in the URL after a `?`, joined with `&`, for example `?ucid=10000001&room=MS-101`.
- **GET** is the method for reading. **POST** is the method for creating or changing something, and its details travel in the request **body**, not in the URL.
- **JSON body**: the data of a POST written as JSON, for example `{"ucid":"10000001","room":"MS-101"}`.
- The bad server books with a GET and a query string. That is the mistake to compare against: a GET should only read.
- **Header**: extra information about the request or response, for example `Content-Type` or `Accept`.
- **Response**: a status line (`HTTP/1.1 201 Created`), headers, and a body.
- **Body**: usually JSON, for example `{"reservationId":"R-1"}`.
- **Status code**: a three-digit number. It is the first thing a caller checks.

**`curl`** is a command-line program that sends one HTTP request and prints the answer. These are the parts you will see in every command below:

| Part | Meaning |
|---|---|
| `curl URL` | Send a **GET** request to the URL and print the body. |
| `-i` | Also print the response **status line and headers**, not just the body. We always use it, because the status is the point. |
| `-X POST` | Use the **POST** method instead of GET. |
| `-H 'Name: value'` | Add a request **header**. |
| `-d '...'` | Send this text as the request **body** (the JSON). |
| `'...?a=1&b=2'` | A URL with a query string. Use quotes so the shell doesn't treat `&` and `?` specially. |
| `localhost:8081` | This computer, port 8081, which is where `Step3_GoodServer` listens. |
| `\` at the end of a line | The command continues on the next line. |

**Use two terminals.** Terminal A runs a server and stays busy (stop it with Ctrl+C). Terminal B is where you type `curl` commands. In both, first `cd api-basics-lab`.

## 3. A badly written API

The mistake is in `Step2_BadServer.handle`. It sends status `200` in every branch, including the `catch` that handles failure.

**Terminal A**: start the bad server. Wait until it prints `LISTENING 8080`.

```sh
java -cp out Step2_BadServer
# Runs the bad web server. It opens port 8080 and waits for requests.
```

**Terminal B**: two requests.

```sh
curl -i 'localhost:8080/api/reserve?ucid=10000001&room=MS-101'
# GET with the booking details in the URL after the ?
# (the quotes stop the shell from treating & specially).
# Alex's real booking.

curl -i 'localhost:8080/api/reserve?ucid=10000001&room=NO-SUCH'
# Same request, but the room does not exist.
```

### Both answers are 200

1. **MS-101** is Alex's real booking.
   ```
   HTTP/1.1 200 OK
   {"ok":true,"id":"R-1","msg":null}
   ```
2. **NO-SUCH** does not exist. The status is still 200.
   ```
   HTTP/1.1 200 OK
   {"ok":false,"id":null,"msg":"booking refused"}
   ```

The caller can't trust the status. Every caller must open the body and invent its own rule for spotting failure. Tools that only look at the status (monitoring, retries, browsers, proxies) are fooled too.

## 4. Conventions for writing APIs

1. **Status codes are crucial.** The status code is the main thing an API user checks. Make it reflect what really happened: success (200), created (201), not found (404), and so on.
2. **Read the message.** The status gives the general result. The body, usually JSON, carries the details of the request and its result, for example a machine-readable `code` and a human `message`.

| Code | Meaning | Whose problem |
|---|---|---|
| 200 | OK | success |
| 201 | Created: a new thing now exists | success |
| 400 | Bad request: the request itself is wrong | caller |
| 401 | Unauthorized: who are you? (real APIs use it for logins; this lab has no login) | caller |
| 404 | Not found: that id was never stored | caller |
| 405 | Method not allowed | caller |
| 5xx | The server broke | server |

## 5. A well-written API: POST /v1/reservations

`Step3_GoodServer.create` is the code to read. In order, it: reads `ucid` and `room` from the JSON body, rejects missing values (400), calls `reserve`, and answers with the right status (201).

**Terminal A**: stop the bad server (Ctrl+C) and start the good one. Leave it running for the rest of this guide.

```sh
java -cp out Step3_GoodServer
# Runs the well-written web server on port 8081 and waits for requests.
# It keeps bookings in memory: restart it to reset everything.
```

**Terminal B**: two requests. They book the same two rooms as in section 3, in the same order. The difference is how: POST, with the details in a JSON body.

```sh
curl -i -X POST localhost:8081/v1/reservations \
  -d '{"ucid":"10000001","room":"MS-101"}'
# POST = create something. The body carries ucid and room.
# Alex's real booking.

curl -i -X POST localhost:8081/v1/reservations \
  -d '{"ucid":"10000001","room":"NO-SUCH"}'
# Same request, but the room does not exist.
```

### The status carries the result

1. **MS-101** is Alex's real booking. 201. The reservation now exists.
   ```
   HTTP/1.1 201 Created
   {"reservationId":"R-1","room":"MS-101","status":"confirmed"}
   ```
2. **NO-SUCH** does not exist. 400, not 200. The message is the one the service threw.
   ```
   HTTP/1.1 400 Bad Request
   {"code":"unknown_room","message":"no bookable room NO-SUCH"}
   ```

Compare with section 3: the bad server answered 200 to both. Here the status alone tells you which one failed. The caller can branch on the status and read the body only for details.

## 6. Reading a reservation back

**Terminal B**:

```sh
curl -i localhost:8081/v1/reservations/R-1
# GET: read booking R-1. Nothing changes on the server.

curl -i localhost:8081/v1/reservations/R-missing
# GET an id that was never stored.
```

1. **Find R-1.** 200 and the same body as before.
   ```
   HTTP/1.1 200 OK
   {"reservationId":"R-1","room":"MS-101","status":"confirmed"}
   ```
2. **Find R-missing.** 404.
   ```
   HTTP/1.1 404 Not Found
   {"code":"not_found","message":"no such reservation"}
   ```
3. **Contrast it with 400.** 404 means that id was never stored. 400 means the request itself was wrong.

## 7. Versions: v2 renames the id

**Terminal B**:

```sh
curl -i -X POST localhost:8081/v2/reservations \
  -d '{"ucid":"10000001","room":"MS-101"}'
# Same call as in section 5, but the path says /v2 instead of /v1.
```

1. **Look at the answer.** 201. This is a new booking, so the id is R-2 (R-1 was made in section 5).
   ```
   HTTP/1.1 201 Created
   {"id":"R-2","room":"MS-101","status":"confirmed"}
   ```
2. **Compare the field.** `/v1` says `reservationId`. `/v2` says `id`.
3. **Note where the version lives.** In the path, so the two calls sit side by side. In the code, `Step3_GoodServer.body` uses one boolean to pick the field name.

### An old API version stays

- `/v1` says `reservationId`. `/v2` says `id`.
- **Both paths stay.** `/v1/reservations` is still served beside `/v2/reservations`.
- **Leave the old API alone.** Removing `/v1` breaks every page already using it.
- **A new field is safe. A rename is not.** Adding a field doesn't break old callers. Renaming `reservationId` does, so it needs a new version.

## 8. One more status: 405

**Terminal B**:

```sh
curl -i localhost:8081/v1/reservations
# A plain GET on the collection. This path only accepts POST.
```

```
HTTP/1.1 405 Method Not Allowed
{"code":"method_not_allowed","message":"GET is not supported here"}
```

The server understood the path but refused the method. In `Step3_GoodServer.handle`, a path that exists with the wrong method ends up here. A path that doesn't exist at all gets 404.

## Exercises

1. **Add a room.** Put `"MS-103"` in `RoomService`, recompile (`javac -d out src/main/java/*.java`), restart the good server, and book it with `{"ucid":"10000001","room":"MS-103"}`.
2. **Book twice.** Send the same POST from section 5 again. Which id comes back? Is that what a caller who retried after a lost response would want?
3. **Fix the bad server.** Make `Step2_BadServer` return 400 for an unknown room. What does the caller no longer need to do?
4. **New endpoint.** Add `DELETE /v1/reservations/{id}` that returns 204, or 404 if the id is unknown. Right now it returns 405. Test it with `curl -i -X DELETE localhost:8081/v1/reservations/R-1`.
5. **New field vs rename.** Add `"currency":"CAD"` to the v1 body. Does any old caller break? Now rename `room` in v1. Why is that different?
6. **Read only the status.** `curl -s -o /dev/null -w '%{http_code}\n' localhost:8081/v1/reservations/R-missing` prints just the number (`-s` quiet, `-o /dev/null` discard the body, `-w` print the status). Try it on each request above. This is how a script checks an API.

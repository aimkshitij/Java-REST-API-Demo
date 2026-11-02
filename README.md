# Product Management REST API

A Spring Boot REST API for managing products, including pricing, categories, and stock quantities. It includes request validation, consistent JSON errors, initial product data, and a health endpoint. Products are stored in memory and reset when the application restarts.

## Stack and prerequisites

- Java 21 JDK
- Maven 3.6.3 or newer
- Spring Boot 3.5.16, Spring Web, Spring Validation, and Actuator
- JUnit 5, Spring Boot Test, and MockMvc
- Optional: Postman desktop application or the Postman Desktop Agent

The Spring Boot parent manages compatible dependency and Maven plugin versions. See the [Spring Boot 3.5 requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html). Maven needs internet access on the first build to download dependencies; the application itself makes no external service calls.

Confirm that `java -version` and `mvn -version` both use Java 21. If necessary, set `JAVA_HOME` to your JDK 21 installation and add its `bin` directory to `PATH`.

## Run

From the project root, alongside `pom.xml`:

```bash
mvn clean verify
mvn spring-boot:run
```

The API listens at `http://localhost:8080`. Leave that terminal running; use another terminal or Postman for requests. Stop it with Ctrl+C.

Alternatively, run the executable JAR after building:

```bash
java -jar target/product-api-1.0.0.jar
```

Use a different port when needed:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

Update Postman's `baseUrl` variable to match a changed port.

Health check:

```bash
curl http://localhost:8080/actuator/health
```

Expected response: `{"status":"UP"}`. Actuator exposes `health` and `info`.

## Product fields

| Field | Java type | Meaning / validation |
| --- | --- | --- |
| `id` | `Long` | Server-generated identifier; omit from requests |
| `name` | `String` | Required and nonblank |
| `description` | `String` | Optional, at most 1000 characters |
| `price` | `BigDecimal` | Required, greater than zero |
| `category` | `String` | Optional product category |
| `stockQuantity` | `Integer` | Required, integer from 0 through 2147483647 |
| `createdAt` | `LocalDateTime` | Server-generated creation time, preserved on update |

Prices are JSON numbers. Date/time values use ISO-8601 local date/time strings without a timezone offset. Request bodies and responses use `application/json` except for the empty DELETE response.

## Endpoints

| Method | Endpoint | Success | Behavior |
| --- | --- | --- | --- |
| POST | `/api/products` | `201 Created` | Creates a product; returns the product and a `Location` header |
| GET | `/api/products` | `200 OK` | Returns a JSON array ordered by ascending ID |
| GET | `/api/products/{id}` | `200 OK` | Returns the requested product |
| PUT | `/api/products/{id}` | `200 OK` | Replaces editable fields and returns the updated product |
| DELETE | `/api/products/{id}` | `204 No Content` | Deletes the product; response body is empty |

PUT requires `name`, `price`, and `stockQuantity` just like POST. Omitted optional fields become `null`. The server controls `id` and `createdAt`. PUT does not create a missing product. GET by ID, PUT, and DELETE return `404` when the ID is missing, including a repeated DELETE.

### Create

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Desk Lamp","description":"Dimmable LED desk lamp","price":24.95,"category":"Office","stockQuantity":10}'
```

Example POST / PUT request body:

```json
{
  "name": "Desk Lamp",
  "description": "Dimmable LED desk lamp",
  "price": 24.95,
  "category": "Office",
  "stockQuantity": 10
}
```

Example creation response (ID and timestamp vary):

```json
{
  "id": 6,
  "name": "Desk Lamp",
  "description": "Dimmable LED desk lamp",
  "price": 24.95,
  "category": "Office",
  "stockQuantity": 10,
  "createdAt": "2026-11-01T10:30:00"
}
```

### List, retrieve, update, and delete

Replace `6` with the ID returned by POST:

```bash
curl http://localhost:8080/api/products
curl http://localhost:8080/api/products/6
curl -X PUT http://localhost:8080/api/products/6 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Adjustable Desk Lamp","description":"Updated lamp","price":34.50,"category":"Lighting","stockQuantity":8}'
curl -i -X DELETE http://localhost:8080/api/products/6
```

### Errors

Validation failures, malformed JSON, incompatible field values, and nonnumeric IDs return `400 Bad Request`. Validation errors include field names and messages. Error responses have this shared shape:

```json
{
  "timestamp": "2026-11-01T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Product with ID 999999 was not found"
}
```

Example invalid creation:

```bash
curl -i -X POST http://localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"","price":0,"stockQuantity":-1}'
```

## Sample data and storage

Every startup creates these records in this order:

| Initial ID | Product | Price | Category | Stock |
| --- | --- | --- | --- | --- |
| 1 | Wireless Mouse | 29.99 | Accessories | 50 |
| 2 | Mechanical Keyboard | 89.99 | Accessories | 25 |
| 3 | USB-C Hub | 49.99 | Connectivity | 40 |
| 4 | Monitor Stand | 39.99 | Office | 30 |
| 5 | Laptop Backpack | 59.99 | Bags | 20 |

`ConcurrentHashMap` stores immutable products and `AtomicLong` generates IDs. Deletion does not reuse an ID during the process lifetime. All changes are lost when the process stops; restarting restores the five samples. No persistence configuration is required.

## Browser access

Frontend code can call the API using `fetch` or any HTTP client. For example:

```javascript
const response = await fetch('http://localhost:8080/api/products');
if (!response.ok) throw new Error(`HTTP ${response.status}`);
const products = await response.json();
console.log(products);
```

CORS permits frontend origins on `localhost` and `127.0.0.1`, ports `3000` and `5173`, including preflight requests. The `Location` response header is exposed to browser clients. To use another origin, configure a comma-separated list:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--app.cors.allowed-origins=http://localhost:4200"
```

## Tests

```bash
mvn test
mvn clean verify
```

Tests start the Spring application context and exercise HTTP routing, serialization, validation, and storage through MockMvc. They cover creation, listing, retrieval, update, deletion, missing records, invalid and malformed requests, validation boundaries, unchanged creation timestamps, health, and browser preflight. No separately running server is required. Maven writes test reports to `target/surefire-reports/` and the executable JAR to `target/product-api-1.0.0.jar`.

## Postman

1. Start the application with `mvn spring-boot:run`.
2. In Postman, choose **Import** and select `postman/product-api.postman_collection.json`.
3. Open the collection's **Variables** tab. `baseUrl` defaults to `http://localhost:8080`. No environment or authentication is required.
4. Run the entire collection in its listed order, or send **Create product** first. Its test script stores the returned ID in the `productId` collection variable.
5. List, get, update, and delete use the created record. The final two requests check `400` and `404` responses. Inspect the **Test Results** tab for status assertions.

The collection contains create, list, get by ID, update, delete, invalid creation, and nonexistent retrieval requests with descriptions and sample JSON bodies. Running it again creates a fresh record. After deletion, send **Create product** again before manually repeating requests that use `productId`. If using Postman's web UI, select the Desktop Agent to reach localhost.

The separate `postman/product-stock.postman_collection.json` collection covers creating a product, updating its stock, retrieving it, and deleting it. Each collection manages its own `productId` variable.

## Layout

```text
pom.xml
src/main/java/com/example/productapi/
  ProductApiApplication.java
  config/         Browser CORS configuration
  controller/     HTTP endpoints and response mapping
  service/        Product operations and business rules
  repository/     Repository interface and in-memory implementation
  model/          Immutable domain model
  dto/            Validated requests, responses, and errors
  exception/      Missing-product exception and shared error handling
  initializer/    Startup sample products
src/main/resources/application.properties
src/test/java/com/example/productapi/ProductApiTests.java
postman/product-api.postman_collection.json
postman/product-stock.postman_collection.json
```

Controllers handle HTTP and map domain products into response DTOs. The service performs product operations. The repository owns storage and ID assignment. Validation stays on request DTOs, and centralized advice formats application errors.

## License

Licensed under the [MIT License](LICENSE).

package com.example.productapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductApiTests {
    private static final String API = "/api/products";
    private static final String VALID_REQUEST = """
            {"name":"Desk Lamp","description":"Dimmable LED desk lamp","price":24.95,
             "category":"Office","stockQuantity":10}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsProductWithLocationAndGeneratedFields() throws Exception {
        JsonNode created = createProduct();
        assertTrue(created.get("id").asLong() > 0);
        LocalDateTime.parse(created.get("createdAt").asText());
        assertEquals("Office", created.get("category").asText());
        assertEquals("Dimmable LED desk lamp", created.get("description").asText());
    }

    @Test
    void retrievesCreatedProduct() throws Exception {
        JsonNode created = createProduct();
        mvc.perform(get(API + "/" + created.get("id").asLong()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.get("id").asLong()))
                .andExpect(jsonPath("$.name").value("Desk Lamp"))
                .andExpect(jsonPath("$.description").value("Dimmable LED desk lamp"))
                .andExpect(jsonPath("$.price").value(24.95))
                .andExpect(jsonPath("$.unitPrice").doesNotExist())
                .andExpect(jsonPath("$.category").value("Office"))
                .andExpect(jsonPath("$.stockQuantity").value(10))
                .andExpect(jsonPath("$.createdAt").value(created.get("createdAt").asText()));
    }

    @Test
    void listsInitializedProducts() throws Exception {
        mvc.perform(get(API)).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItems("Wireless Mouse", "Mechanical Keyboard",
                        "USB-C Hub", "Monitor Stand", "Laptop Backpack")))
                .andExpect(jsonPath("$[?(@.id == 1)].price", hasItem(29.99)))
                .andExpect(jsonPath("$[*].unitPrice").isEmpty())
                .andExpect(jsonPath("$[0].createdAt").isString());
    }

    @Test
    void updatesProductAndPreservesIdentityAndCreationTime() throws Exception {
        JsonNode created = createProduct();
        long id = created.get("id").asLong();
        String request = """
                {"name":"Adjustable Desk Lamp","description":"Updated lamp","price":34.50,
                 "category":"Lighting","stockQuantity":8}
                """;
        mvc.perform(put(API + "/" + id).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Adjustable Desk Lamp"))
                .andExpect(jsonPath("$.description").value("Updated lamp"))
                .andExpect(jsonPath("$.price").value(34.50))
                .andExpect(jsonPath("$.unitPrice").doesNotExist())
                .andExpect(jsonPath("$.category").value("Lighting"))
                .andExpect(jsonPath("$.stockQuantity").value(8))
                .andExpect(jsonPath("$.createdAt").value(created.get("createdAt").asText()));
        mvc.perform(get(API + "/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Adjustable Desk Lamp"))
                .andExpect(jsonPath("$.price").value(34.50))
                .andExpect(jsonPath("$.unitPrice").doesNotExist())
                .andExpect(jsonPath("$.stockQuantity").value(8));
    }

    @Test
    void zeroStockUpdateCurrentlyRetainsPreviousQuantity() throws Exception {
        // Characterize the initial demo behavior, including persistence, without hiding the defect.
        JsonNode created = createProduct();
        long id = created.get("id").asLong();
        String request = VALID_REQUEST.replace(":10", ":0").replace("Desk Lamp", "Sold-out Lamp");
        mvc.perform(put(API + "/" + id).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sold-out Lamp"))
                .andExpect(jsonPath("$.stockQuantity").value(10));
        mvc.perform(get(API + "/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sold-out Lamp"))
                .andExpect(jsonPath("$.price").value(24.95))
                .andExpect(jsonPath("$.stockQuantity").value(10));
    }

    @Test
    void deletesProductAndSubsequentRequestsReturnNotFound() throws Exception {
        long id = createProduct().get("id").asLong();
        mvc.perform(delete(API + "/" + id)).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        expectError(mvc.perform(get(API + "/" + id)), 404, "Not Found");
        expectError(mvc.perform(delete(API + "/" + id)), 404, "Not Found");
        mvc.perform(get(API)).andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
    }

    @Test
    void missingGetUpdateAndDeleteReturnConsistentNotFound() throws Exception {
        String url = API + "/" + Long.MAX_VALUE;
        expectError(mvc.perform(get(url)), 404, "Not Found")
                .andExpect(jsonPath("$.message").value("Product with ID " + Long.MAX_VALUE + " was not found"));
        expectError(mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST)),
                404, "Not Found");
        expectError(mvc.perform(delete(url)), 404, "Not Found");
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void rejectsInvalidCreation(String request) throws Exception {
        int before = listSize();
        expectError(mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(request)),
                400, "Bad Request");
        assertEquals(before, listSize());
    }

    @Test
    void rejectsInvalidUpdateWithoutChangingStoredProduct() throws Exception {
        long id = createProduct().get("id").asLong();
        String before = mvc.perform(get(API + "/" + id)).andReturn().getResponse().getContentAsString();
        expectError(mvc.perform(put(API + "/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST.replace("24.95", "-1"))), 400, "Bad Request")
                .andExpect(jsonPath("$.message", containsString("Price must be greater than zero")));
        assertEquals(before, mvc.perform(get(API + "/" + id)).andReturn().getResponse().getContentAsString());
    }

    @Test
    void acceptsZeroStockSmallPositivePriceAndMaximumDescriptionLength() throws Exception {
        String request = VALID_REQUEST.replace("24.95", "0.001").replace(":10", ":0")
                .replace("Dimmable LED desk lamp", "x".repeat(1000));
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.price").value(0.001))
                .andExpect(jsonPath("$.stockQuantity").value(0));
    }

    @Test
    void acceptsOptionalDescriptionAndCategory() throws Exception {
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Cable","price":5,"stockQuantity":1}
                        """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Cable"));
    }

    @Test
    void invalidIdAndUnknownRouteReturnJsonErrors() throws Exception {
        expectError(mvc.perform(get(API + "/abc")), 400, "Bad Request");
        expectError(mvc.perform(get(API + "/999999999999999999999")), 400, "Bad Request");
        expectError(mvc.perform(get("/api/unknown")), 404, "Not Found");
    }

    @Test
    void exposesHealthEndpoint() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void allowsBrowserPreflightAndExposesLocationHeader() throws Exception {
        mvc.perform(options(API).header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(post(API).header("Origin", "http://localhost:5173")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().string("Access-Control-Expose-Headers", "Location"));
    }

    private JsonNode createProduct() throws Exception {
        String response = mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.price").value(24.95))
                .andExpect(jsonPath("$.unitPrice").doesNotExist())
                .andExpect(header().string("Location", containsString(API + "/")))
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(response);
        mvc.perform(get(API + "/" + created.get("id").asLong())).andExpect(status().isOk());
        return created;
    }

    private int listSize() throws Exception {
        return objectMapper.readTree(mvc.perform(get(API)).andReturn().getResponse().getContentAsString()).size();
    }

    private ResultActions expectError(ResultActions result, int status, String error) throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.status", is(status)))
                .andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    static Stream<String> invalidRequests() {
        return Stream.of(
                "{}",
                VALID_REQUEST.replace("\"Desk Lamp\"", "\"  \""),
                VALID_REQUEST.replace("\"Desk Lamp\"", "null"),
                VALID_REQUEST.replace("24.95", "0"),
                VALID_REQUEST.replace("24.95", "-0.01"),
                VALID_REQUEST.replace("24.95", "null"),
                VALID_REQUEST.replace(":10", ":-1"),
                VALID_REQUEST.replace(":10", ":null"),
                VALID_REQUEST.replace(":10", ":1.5"),
                VALID_REQUEST.replace("Dimmable LED desk lamp", "x".repeat(1001)),
                VALID_REQUEST.replace("24.95", "\"not-a-number\""),
                VALID_REQUEST.replace(":10", ":2147483648"),
                "{broken-json",
                "null",
                "[]",
                "");
    }
}

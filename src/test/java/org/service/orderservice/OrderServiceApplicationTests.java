package org.service.orderservice;

import org.service.orderservice.stubs.InventoryClientStub;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
class OrderServiceApplicationTests {

    @ServiceConnection
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:16");

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    static {
        postgreSQLContainer.start();
    }

    @Test
    void shouldSubmitOrder() {
        //String submitOrderJson = """
                //{
                //         "orderNumber": "1",
                //         "skuCode": "iphone_15",
                //         "price": 1000,
                //         "quantity": 1
               // }
               // """;

        //InventoryClientStub.stubInventoryCall("iphone_15", 1);

       // RestAssured.given()
          //      .contentType("application/json")
          //      .body(submitOrderJson)
          //      .when()
           //     .post("/api/orders")
           //     .then()
          //      .log().all()
          //      .statusCode(201)
        //        .body("id", org.hamcrest.Matchers.notNullValue())
         //       .body("orderNumber", org.hamcrest.Matchers.equalTo("1"))
         //       .body("skuCode", org.hamcrest.Matchers.equalTo("iphone_15"))
          //      .body("price", Matchers.equalTo(1000))
        //        .body("quantity", Matchers.equalTo(1));
        }

    }

package com.addi.loan.features.submitloan;

import com.addi.loan.features.submitloan.dto.SubmitLoanRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration Test for SubmitLoanResource
 *
 * Tests the REST API endpoint with Quarkus Test framework.
 * Note: This requires Temporal and PostgreSQL to be running.
 */
@QuarkusTest
class SubmitLoanResourceIT {

    @Test
    void shouldSubmitLoanApplication() {
        SubmitLoanRequest request = new SubmitLoanRequest(
            "client-test-123",
            BigDecimal.valueOf(25000),
            36,
            "Home improvement"
        );

        given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/v1/loans")
        .then()
            .statusCode(202) // Accepted
            .body("loanId", startsWith("loan-"))
            .body("status", equalTo("PROCESSING"))
            .body("message", notNullValue());
    }

    @Test
    void shouldRejectInvalidRequest() {
        SubmitLoanRequest invalidRequest = new SubmitLoanRequest(
            "client-test-456",
            BigDecimal.valueOf(500), // Below minimum
            36,
            "Home improvement"
        );

        given()
            .contentType(ContentType.JSON)
            .body(invalidRequest)
        .when()
            .post("/api/v1/loans")
        .then()
            .statusCode(400); // Bad Request
    }

    @Test
    void shouldRejectMissingClientId() {
        SubmitLoanRequest invalidRequest = new SubmitLoanRequest(
            null, // Missing client ID
            BigDecimal.valueOf(25000),
            36,
            "Home improvement"
        );

        given()
            .contentType(ContentType.JSON)
            .body(invalidRequest)
        .when()
            .post("/api/v1/loans")
        .then()
            .statusCode(400); // Bad Request
    }
}

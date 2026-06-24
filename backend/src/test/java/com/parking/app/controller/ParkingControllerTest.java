package com.parking.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parking.app.model.VehicleType;
import com.parking.app.service.ParkingLotService;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.h2.console.enabled=false",
        "parking.sqs.enabled=false"
})
class ParkingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ParkingLotService service;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SqsTemplate sqsTemplate;

    @BeforeEach
    void setUp() {
        service.resetLot();
    }

    // ── GET /api/parking/spots ─────────────────────────────────────────────────

    @Test
    void getSpotsReturns27FreeSpotsAfterReset() throws Exception {
        mockMvc.perform(get("/api/parking/spots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(27)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].size").exists())
                .andExpect(jsonPath("$[0].status").value("FREE"));
    }

    @Test
    void getSpotsReflectsParkedVehicle() throws Exception {
        service.parkVehicle(VehicleType.CAR, "AB-001");

        mockMvc.perform(get("/api/parking/spots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.status=='OCCUPIED')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.status=='FREE')]", hasSize(26)));
    }

    // ── GET /api/parking/availability ─────────────────────────────────────────

    @Test
    void getAvailabilityReturnsTotalsOnEmptyLot() throws Exception {
        mockMvc.perform(get("/api/parking/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpots").value(27))
                .andExpect(jsonPath("$.totalFree").value(27))
                .andExpect(jsonPath("$.totalOccupied").value(0))
                .andExpect(jsonPath("$.bySize.SMALL.total").value(9))
                .andExpect(jsonPath("$.bySize.MEDIUM.total").value(9))
                .andExpect(jsonPath("$.bySize.LARGE.total").value(9));
    }

    @Test
    void getAvailabilityDecrementsFreeCountAfterPark() throws Exception {
        service.parkVehicle(VehicleType.CAR, "AB-001");
        service.parkVehicle(VehicleType.CAR, "AB-002");

        mockMvc.perform(get("/api/parking/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFree").value(25))
                .andExpect(jsonPath("$.totalOccupied").value(2))
                .andExpect(jsonPath("$.bySize.MEDIUM.free").value(7))
                .andExpect(jsonPath("$.bySize.MEDIUM.occupied").value(2));
    }

    // ── POST /api/parking/park ─────────────────────────────────────────────────

    @Test
    void parkMotorcycleReturns201WithSmallSpot() throws Exception {
        mockMvc.perform(post("/api/parking/park")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleType\":\"MOTORCYCLE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.size").value("SMALL"))
                .andExpect(jsonPath("$.status").value("OCCUPIED"))
                .andExpect(jsonPath("$.occupiedBy").value("MOTORCYCLE"));
    }

    @Test
    void parkCarReturns201WithMediumSpot() throws Exception {
        mockMvc.perform(post("/api/parking/park")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleType\":\"CAR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.size").value("MEDIUM"))
                .andExpect(jsonPath("$.occupiedBy").value("CAR"));
    }

    @Test
    void parkTruckReturns201WithLargeSpot() throws Exception {
        mockMvc.perform(post("/api/parking/park")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleType\":\"TRUCK\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.size").value("LARGE"))
                .andExpect(jsonPath("$.occupiedBy").value("TRUCK"));
    }

    @Test
    void parkWithLicensePlatePersistsIt() throws Exception {
        mockMvc.perform(post("/api/parking/park")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleType\":\"CAR\",\"licensePlate\":\"XY-9999\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate").value("XY-9999"));
    }

    @Test
    void parkWithoutLicensePlateGeneratesPlaceholder() throws Exception {
        mockMvc.perform(post("/api/parking/park")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleType\":\"CAR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate").exists());
    }

    @Test
    void parkWithMissingVehicleTypeReturns400() throws Exception {
        mockMvc.perform(post("/api/parking/park")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void parkWithUnknownVehicleTypeReturns400() throws Exception {
        mockMvc.perform(post("/api/parking/park")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleType\":\"BICYCLE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void parkWhenNoCompatibleSpotReturns409() throws Exception {
        for (int i = 0; i < 9; i++) {
            service.parkVehicle(VehicleType.TRUCK, "T" + i);
        }

        mockMvc.perform(post("/api/parking/park")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleType\":\"TRUCK\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").exists());
    }

    // ── POST /api/parking/spots/{id}/clear ────────────────────────────────────

    @Test
    void clearOccupiedSpotReturns200WithFreeStatus() throws Exception {
        var parkResult = mockMvc.perform(post("/api/parking/park")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleType\":\"CAR\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String spotId = objectMapper
                .readTree(parkResult.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(post("/api/parking/spots/" + spotId + "/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(spotId))
                .andExpect(jsonPath("$.status").value("FREE"));
    }

    @Test
    void clearUnknownSpotReturns404() throws Exception {
        mockMvc.perform(post("/api/parking/spots/DOES-NOT-EXIST/clear"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void clearAlreadyFreeSpotReturns409() throws Exception {
        String spotId = service.getAllSpots().get(0).getId();

        mockMvc.perform(post("/api/parking/spots/" + spotId + "/clear"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ── POST /api/parking/park-concurrent ─────────────────────────────────────

    @Test
    void parkConcurrentlyReturnsParkedAndFailedCounts() throws Exception {
        mockMvc.perform(post("/api/parking/park-concurrent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motorcycleCount\":2,\"carCount\":2,\"truckCount\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parked").value(5))
                .andExpect(jsonPath("$.failed").value(0));
    }

    @Test
    void parkConcurrentlyCountsFailuresWhenFull() throws Exception {
        for (int i = 0; i < 9; i++) {
            service.parkVehicle(VehicleType.TRUCK, "T" + i);
        }

        mockMvc.perform(post("/api/parking/park-concurrent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motorcycleCount\":0,\"carCount\":0,\"truckCount\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parked").value(0))
                .andExpect(jsonPath("$.failed").value(3));
    }

    // ── POST /api/parking/reset ────────────────────────────────────────────────

    @Test
    void resetLotReturns204AndFreesAllSpots() throws Exception {
        service.parkVehicle(VehicleType.CAR, "TEST");
        service.parkVehicle(VehicleType.TRUCK, "TEST2");

        mockMvc.perform(post("/api/parking/reset"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/parking/availability"))
                .andExpect(jsonPath("$.totalFree").value(27))
                .andExpect(jsonPath("$.totalOccupied").value(0));
    }

    // ── GET /actuator/health ───────────────────────────────────────────────────

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}

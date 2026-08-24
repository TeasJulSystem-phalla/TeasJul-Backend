package com.example.spring_boot_project_api.controller;

import java.util.List;
                       
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.spring_boot_project_api.dto.request.CreateHouseRequest;
import com.example.spring_boot_project_api.dto.request.UpdateHouseRequest;
import com.example.spring_boot_project_api.dto.response.HouseResponse;
import com.example.spring_boot_project_api.service.HouseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/houses")
@RequiredArgsConstructor
@Tag(name = "Houses", description = "House management endpoints (owner is resolved from the authenticated user)")
public class HouseController {

    private final HouseService houseService;

    @Operation(summary = "Create a new house",
            description = "Creates a house owned by the currently authenticated user. Name, slug and code must be unique per owner.")
    @PostMapping
    public ResponseEntity<HouseResponse> createHouse(@Valid @RequestBody CreateHouseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(houseService.createHouse(request));
    }

    @Operation(summary = "List my houses",
            description = "Returns all houses belonging to the currently authenticated user.")
    @GetMapping
    public ResponseEntity<List<HouseResponse>> getMyHouses() {
        return ResponseEntity.ok(houseService.getMyHouses());
    }

    @Operation(summary = "Get a house by id",
            description = "Returns the house with the given id if it belongs to the currently authenticated user.")
    @GetMapping("/{id}")
    public ResponseEntity<HouseResponse> getHouseById(@PathVariable Long id) {
        return ResponseEntity.ok(houseService.getHouseById(id));
    }

    @Operation(summary = "Update a house",
            description = "Updates the house with the given id if it belongs to the currently authenticated user. Only provided fields are updated.")
    @PutMapping("/{id}")
    public ResponseEntity<HouseResponse> updateHouse(@PathVariable Long id,
            @Valid @RequestBody UpdateHouseRequest request) {
        return ResponseEntity.ok(houseService.updateHouse(id, request));
    }

    @Operation(summary = "Delete a house",
            description = "Deletes the house with the given id if it belongs to the currently authenticated user.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHouse(@PathVariable Long id) {
        houseService.deleteHouse(id);
        return ResponseEntity.noContent().build();
    }
}

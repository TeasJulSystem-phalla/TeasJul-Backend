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

import com.example.spring_boot_project_api.dto.request.RoomRepuest;
import com.example.spring_boot_project_api.dto.response.RoomResponse;
import com.example.spring_boot_project_api.service.RoomService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Rooms", description = "Room management endpoints")
public class RoomController {

    private final RoomService roomService;

    @Operation(summary = "Create a new room",
            description = "Creates a room. Room number must be unique within its house.")
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomRepuest requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.createRoom(requestDto));
    }

    @Operation(summary = "List all rooms",
            description = "Returns all rooms.")
    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    @Operation(summary = "Get a room by id",
            description = "Returns the room with the given id.")
    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @Operation(summary = "List rooms by house",
            description = "Returns all rooms belonging to the given house.")
    @GetMapping("/house/{houseId}")
    public ResponseEntity<List<RoomResponse>> getRoomsByHouse(@PathVariable Long houseId) {
        return ResponseEntity.ok(roomService.getRoomsByHouse(houseId));
    }

    @Operation(summary = "Update a room",
            description = "Updates the room with the given id.")
    @PutMapping("/{id}")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Long id,
            @Valid @RequestBody RoomRepuest requestDto) {
        return ResponseEntity.ok(roomService.updateRoom(id, requestDto));
    }

    @Operation(summary = "Delete a room",
            description = "Deletes the room with the given id.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}

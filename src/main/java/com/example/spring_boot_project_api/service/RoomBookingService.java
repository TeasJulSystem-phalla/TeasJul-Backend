package com.example.spring_boot_project_api.service;

import java.util.List;
import com.example.spring_boot_project_api.dto.request.RoomBookingRequest;
import com.example.spring_boot_project_api.dto.response.RoomBookingResponse;

public interface RoomBookingService {
    RoomBookingResponse createBooking(RoomBookingRequest request);
    RoomBookingResponse getBookingById(Long id);
    List<RoomBookingResponse> getBookings(Long roomId);
    RoomBookingResponse cancelBooking(Long id);
    void completeBookedForRoom(Long roomId);
}

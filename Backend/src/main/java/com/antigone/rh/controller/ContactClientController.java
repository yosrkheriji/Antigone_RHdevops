package com.antigone.rh.controller;

import com.antigone.rh.dto.ApiResponse;
import com.antigone.rh.dto.ContactClientDTO;
import com.antigone.rh.service.ContactClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance/contacts")
@RequiredArgsConstructor
public class ContactClientController {

    private final ContactClientService contactClientService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ContactClientDTO>>> getByClient(@RequestParam Long clientId) {
        return ResponseEntity.ok(ApiResponse.ok(contactClientService.getByClient(clientId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ContactClientDTO>> create(@RequestBody ContactClientDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok("Contact créé", contactClientService.create(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactClientDTO>> update(
            @PathVariable Long id, @RequestBody ContactClientDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok("Contact mis à jour", contactClientService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        contactClientService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Contact supprimé", null));
    }
}

package com.fooddelivery.orders.controller;

import com.fooddelivery.orders.entity.CodRemittance;
import com.fooddelivery.orders.service.CodRemittanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/remittances")
@RequiredArgsConstructor
public class CodRemittanceController {

    private final CodRemittanceService codRemittanceService;

    @GetMapping("/shipper/{shipperId}")
    public ResponseEntity<List<CodRemittance>> getShipperRemittances(@PathVariable Long shipperId) {
        return ResponseEntity.ok(codRemittanceService.getShipperRemittances(shipperId));
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<List<CodRemittance>> getShopRemittances(@PathVariable Long shopId) {
        return ResponseEntity.ok(codRemittanceService.getShopRemittances(shopId));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<CodRemittance>> getAllRemittancesAdmin() {
        return ResponseEntity.ok(codRemittanceService.getAllRemittances());
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<CodRemittance> completeRemittance(@PathVariable Long id) {
        return ResponseEntity.ok(codRemittanceService.completeRemittance(id));
    }
}

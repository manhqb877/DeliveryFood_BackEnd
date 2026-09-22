package com.fooddelivery.orders.controller;

import com.fooddelivery.orders.entity.CommissionConfig;
import com.fooddelivery.orders.repository.CommissionConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/commission-configs")
@RequiredArgsConstructor
public class CommissionConfigController {

    private final CommissionConfigRepository commissionConfigRepository;

    @GetMapping("/admin/all")
    public ResponseEntity<List<CommissionConfig>> getAllConfigs() {
        return ResponseEntity.ok(commissionConfigRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "validFrom")));
    }

    @PostMapping("/admin")
    public ResponseEntity<CommissionConfig> createConfig(@RequestBody CommissionConfig config) {
        CommissionConfig saved = commissionConfigRepository.save(config);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<CommissionConfig> updateConfig(@PathVariable Long id, @RequestBody CommissionConfig configDetails) {
        return commissionConfigRepository.findById(id).map(config -> {
            config.setShopId(configDetails.getShopId());
            config.setShopName(configDetails.getShopName());
            config.setAreaId(configDetails.getAreaId());
            config.setAreaName(configDetails.getAreaName());
            config.setCommissionType(configDetails.getCommissionType());
            config.setRate(configDetails.getRate());
            return ResponseEntity.ok(commissionConfigRepository.save(config));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        if (commissionConfigRepository.existsById(id)) {
            commissionConfigRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}

package com.fooddelivery.core.controller;

import com.fooddelivery.core.dto.request.BulkItemOptionUpdateRequest;
import com.fooddelivery.core.dto.request.ItemOptionRequest;
import com.fooddelivery.core.dto.request.ItemRequest;
import com.fooddelivery.core.dto.response.ItemDto;
import com.fooddelivery.core.dto.response.ItemOptionDto;
import com.fooddelivery.core.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/core/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    public ResponseEntity<ItemDto> createItem(@Valid @RequestBody ItemRequest request) {
        return ResponseEntity.ok(itemService.createItem(request));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ItemDto> updateItem(@PathVariable Long itemId, @Valid @RequestBody ItemRequest request) {
        return ResponseEntity.ok(itemService.updateItem(itemId, request));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long itemId) {
        itemService.deleteItem(itemId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ItemDto> getItemDetails(@PathVariable Long itemId) {
        return ResponseEntity.ok(itemService.getItemDetails(itemId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ItemDto>> searchItems(@RequestParam String keyword) {
        return ResponseEntity.ok(itemService.searchItems(keyword));
    }

    @PostMapping("/deduct-stock")
    public ResponseEntity<Void> deductStock(@Valid @RequestBody com.fooddelivery.core.dto.request.DeductStockRequest request) {
        itemService.deductStock(request);
        return ResponseEntity.ok().build();
    }

    // --- OPTIONS ---

    @GetMapping("/{itemId}/options")
    public ResponseEntity<List<ItemOptionDto>> getItemOptions(@PathVariable Long itemId) {
        return ResponseEntity.ok(itemService.getOptions(itemId));
    }

    @PostMapping("/{itemId}/options")
    public ResponseEntity<ItemOptionDto> addOption(@PathVariable Long itemId, @Valid @RequestBody ItemOptionRequest request) {
        return ResponseEntity.ok(itemService.addOption(itemId, request));
    }

    @DeleteMapping("/options/{optionId}")
    public ResponseEntity<Void> deleteOption(@PathVariable Long optionId) {
        itemService.deleteOption(optionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/category/{categoryId}/suggested-options")
    public ResponseEntity<List<ItemOptionDto>> getSuggestedOptions(@PathVariable Long categoryId) {
        return ResponseEntity.ok(itemService.getSuggestedOptionsByCategory(categoryId));
    }

    @PostMapping("/category/{categoryId}/bulk-options")
    public ResponseEntity<Void> addBulkOptionToCategory(
            @PathVariable Long categoryId, 
            @Valid @RequestBody ItemOptionRequest request) {
        itemService.addBulkOptionToCategory(categoryId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/category/{categoryId}/bulk-options")
    public ResponseEntity<Void> updateBulkOptionInCategory(
            @PathVariable Long categoryId, 
            @Valid @RequestBody BulkItemOptionUpdateRequest request) {
        itemService.updateBulkOptionInCategory(categoryId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/category/{categoryId}/bulk-options")
    public ResponseEntity<Void> deleteBulkOptionInCategory(
            @PathVariable Long categoryId, 
            @RequestParam String groupName, 
            @RequestParam String optionName) {
        itemService.deleteBulkOptionInCategory(categoryId, groupName, optionName);
        return ResponseEntity.ok().build();
    }
}

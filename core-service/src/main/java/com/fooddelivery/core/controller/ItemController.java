package com.fooddelivery.core.controller;

import com.fooddelivery.core.dto.response.ItemDto;
import com.fooddelivery.core.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/core/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/{itemId}")
    public ResponseEntity<ItemDto> getItemDetails(@PathVariable Long itemId) {
        return ResponseEntity.ok(itemService.getItemDetails(itemId));
    }

    @GetMapping("/search")
    public ResponseEntity<java.util.List<ItemDto>> searchItems(@org.springframework.web.bind.annotation.RequestParam String keyword) {
        return ResponseEntity.ok(itemService.searchItems(keyword));
    }
}

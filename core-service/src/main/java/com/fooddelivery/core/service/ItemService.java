package com.fooddelivery.core.service;

import com.fooddelivery.core.dto.response.ItemDto;
import com.fooddelivery.core.dto.response.ItemOptionDto;
import com.fooddelivery.core.entity.Item;
import com.fooddelivery.core.entity.ItemOption;
import com.fooddelivery.core.entity.ItemPrice;
import com.fooddelivery.core.repository.ItemOptionRepository;
import com.fooddelivery.core.repository.ItemPriceRepository;
import com.fooddelivery.core.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemOptionRepository itemOptionRepository;
    private final ItemPriceRepository itemPriceRepository;

    public void applyDynamicPricing(Item item, ItemDto.ItemDtoBuilder builder) {
        List<ItemPrice> activePrices = itemPriceRepository.findByItemIdAndIsActiveTrueOrderByPriorityDesc(item.getId());
        applyDynamicPricing(item, builder, activePrices);
    }

    public void applyDynamicPricing(Item item, ItemDto.ItemDtoBuilder builder, List<ItemPrice> activePrices) {
        LocalTime nowTime = LocalTime.now();
        LocalDate nowDate = LocalDate.now();
        
        BigDecimal finalPrice = item.getBasePrice();
        String appliedPriceName = null;
        BigDecimal originalPrice = null;

        for (ItemPrice ip : activePrices) {
            boolean validDate = true;
            if (ip.getValidFrom() != null && ip.getValidUntil() != null) {
                validDate = !nowDate.isBefore(ip.getValidFrom()) && !nowDate.isAfter(ip.getValidUntil());
            } else if (ip.getValidFrom() != null) {
                validDate = !nowDate.isBefore(ip.getValidFrom());
            } else if (ip.getValidUntil() != null) {
                validDate = !nowDate.isAfter(ip.getValidUntil());
            }

            boolean validTime = true;
            if (ip.getTimeStart() != null && ip.getTimeEnd() != null) {
                validTime = !nowTime.isBefore(ip.getTimeStart()) && !nowTime.isAfter(ip.getTimeEnd());
            }

            // You can also add logic for applicableDays if needed
            
            if (validDate && validTime) {
                finalPrice = ip.getPrice();
                appliedPriceName = ip.getPriceName();
                originalPrice = item.getBasePrice();
                break; // highest priority first
            }
        }

        builder.basePrice(finalPrice);
        builder.originalPrice(originalPrice);
        builder.priceName(appliedPriceName);
    }

    public ItemDto getItemDetails(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        List<ItemOption> options = itemOptionRepository.findByItemIdInAndIsActiveTrueOrderBySortOrderAsc(List.of(itemId));

        List<ItemOptionDto> optionDtos = options.stream().map(opt -> 
            ItemOptionDto.builder()
                .id(opt.getId())
                .groupName(opt.getGroupName())
                .optionName(opt.getOptionName())
                .extraPrice(opt.getExtraPrice())
                .isRequired(opt.getIsRequired())
                .isMultiple(opt.getIsMultiple())
                .maxSelect(opt.getMaxSelect())
                .build()
        ).collect(Collectors.toList());

        ItemDto.ItemDtoBuilder builder = ItemDto.builder()
                .id(item.getId())
                .shopId(item.getShop().getId())
                .shopName(item.getShop().getShopName())
                .name(item.getName())
                .description(item.getDescription())
                .imageUrl(item.getImageUrl())
                .status(item.getStatus().name())
                .avgRating(item.getAvgRating())
                .totalReviews(item.getTotalReviews())
                .options(optionDtos);

        applyDynamicPricing(item, builder);
        return builder.build();
    }

    public List<ItemDto> searchItems(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        
        List<Item> items = itemRepository.findByNameContainingIgnoreCaseAndStatusNot(
                keyword.trim(), com.fooddelivery.core.enums.ItemStatus.DISCONTINUED);
                
        return items.stream().map(item -> {
            ItemDto.ItemDtoBuilder builder = ItemDto.builder()
                .id(item.getId())
                .shopId(item.getShop().getId())
                .shopName(item.getShop().getShopName())
                .name(item.getName())
                .description(item.getDescription())
                .imageUrl(item.getImageUrl())
                .status(item.getStatus().name())
                .avgRating(item.getAvgRating())
                .totalReviews(item.getTotalReviews())
                .options(List.of());

            applyDynamicPricing(item, builder);
            return builder.build();
        }).collect(Collectors.toList());
    }
}

package com.fooddelivery.core.service;

import com.fooddelivery.core.dto.response.CategoryDto;
import com.fooddelivery.core.dto.response.ItemDto;
import com.fooddelivery.core.dto.response.ItemOptionDto;
import com.fooddelivery.core.dto.response.ShopDetailsResponse;
import com.fooddelivery.core.entity.Category;
import com.fooddelivery.core.entity.Item;
import com.fooddelivery.core.entity.ItemOption;
import com.fooddelivery.core.entity.Shop;
import com.fooddelivery.core.enums.ItemStatus;
import com.fooddelivery.core.exception.ResourceNotFoundException;
import com.fooddelivery.core.repository.CategoryRepository;
import com.fooddelivery.core.repository.ItemOptionRepository;
import com.fooddelivery.core.repository.ItemPriceRepository;
import com.fooddelivery.core.repository.ItemRepository;
import com.fooddelivery.core.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;
    private final ItemOptionRepository itemOptionRepository;
    private final ItemService itemService;
    private final ItemPriceRepository itemPriceRepository;

    public List<Shop> getAllShops() {
        return shopRepository.findAll();
    }

    public ShopDetailsResponse getShopDetails(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found"));

        List<Category> categories = categoryRepository.findByShopIdAndIsActiveTrueOrderBySortOrderAsc(shopId);
        List<Item> allItems = itemRepository.findByShopIdAndStatusIn(shopId, 
                java.util.Arrays.asList(com.fooddelivery.core.enums.ItemStatus.AVAILABLE, com.fooddelivery.core.enums.ItemStatus.SOLD_OUT));
        
        List<Long> itemIds = allItems.stream().map(Item::getId).collect(Collectors.toList());
        List<ItemOption> allOptions = itemIds.isEmpty() ? new ArrayList<>() : 
                itemOptionRepository.findByItemIdInAndIsActiveTrueOrderBySortOrderAsc(itemIds);

        Map<Long, List<ItemOption>> optionsByItemId = allOptions.stream()
                .collect(Collectors.groupingBy(opt -> opt.getItem().getId()));

        Map<Long, List<Item>> itemsByCategoryId = allItems.stream()
                .filter(i -> i.getCategory() != null)
                .collect(Collectors.groupingBy(i -> i.getCategory().getId()));

        // Fetch all active prices for all items in one query to avoid N+1 problem
        List<com.fooddelivery.core.entity.ItemPrice> allActivePrices = itemIds.isEmpty() ? new ArrayList<>() :
                itemPriceRepository.findByItemIdInAndIsActiveTrueOrderByPriorityDesc(itemIds);
        Map<Long, List<com.fooddelivery.core.entity.ItemPrice>> activePricesByItemId = allActivePrices.stream()
                .collect(Collectors.groupingBy(ip -> ip.getItem().getId()));

        List<CategoryDto> categoryDtos = categories.stream().map(cat -> {
            List<Item> catItems = itemsByCategoryId.getOrDefault(cat.getId(), new ArrayList<>());
            List<ItemDto> itemDtos = catItems.stream().map(item -> {
                List<ItemOption> itemOptions = optionsByItemId.getOrDefault(item.getId(), new ArrayList<>());
                List<ItemOptionDto> optionDtos = itemOptions.stream().map(opt -> 
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
                        .categoryId(item.getCategory().getId())
                        .categoryName(item.getCategory().getName())
                        .name(item.getName())
                        .description(item.getDescription())
                        .imageUrl(item.getImageUrl())
                        .status(item.getStatus().name())
                        .avgRating(item.getAvgRating())
                        .totalReviews(item.getTotalReviews())
                        .dailyLimit(item.getDailyLimit())
                        .prepTimeMinutes(item.getPrepTimeMinutes())
                        .tags(item.getTags() != null ? java.util.Arrays.asList(item.getTags()) : java.util.List.of())
                        .sortOrder(item.getSortOrder())
                        .discountPrice(item.getDiscountPrice())
                        .options(optionDtos);
                
                List<com.fooddelivery.core.entity.ItemPrice> itemPrices = activePricesByItemId.getOrDefault(item.getId(), new ArrayList<>());
                itemService.applyDynamicPricing(item, builder, itemPrices);
                return builder.build();
            }).collect(Collectors.toList());

            return CategoryDto.builder()
                    .id(cat.getId())
                    .name(cat.getName())
                    .description(cat.getDescription())
                    .items(itemDtos)
                    .build();
        }).collect(Collectors.toList());

        return ShopDetailsResponse.builder()
                .id(shop.getId())
                .shopName(shop.getShopName())
                .locationDetail(shop.getLocationDetail())
                .shopLat(shop.getShopLat())
                .shopLng(shop.getShopLng())
                .avgRating(shop.getAvgRating())
                .totalReviews(shop.getTotalReviews())
                .categories(categoryDtos)
                .build();
    }
}

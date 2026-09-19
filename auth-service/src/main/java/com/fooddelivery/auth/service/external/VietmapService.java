package com.fooddelivery.auth.service.external;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VietmapService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${vietmap.api.key}")
    private String apiKey;

    @Value("${vietmap.api.url}")
    private String apiUrl;

    public Coordinates geocodeAddress(String addressLine) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("${")) {
            log.warn("VietMap API Key is not configured. Returning null.");
            return null;
        }

        try {
            // Step 1: Search for address to get ref_id
            String searchUrl = apiUrl + "?apikey=" + apiKey + "&text=" + addressLine;
            ResponseEntity<VietmapSearchResponse[]> searchResponse = restTemplate.getForEntity(searchUrl, VietmapSearchResponse[].class);

            if (searchResponse.getStatusCode().is2xxSuccessful() && searchResponse.getBody() != null && searchResponse.getBody().length > 0) {
                String refId = searchResponse.getBody()[0].getRef_id();

                if (refId != null && !refId.isEmpty()) {
                    // Step 2: Get coordinates using ref_id via place API
                    String placeUrl = "https://maps.vietmap.vn/api/place/v3?apikey=" + apiKey + "&refid=" + refId;
                    ResponseEntity<VietmapPlaceResponse> placeResponse = restTemplate.getForEntity(placeUrl, VietmapPlaceResponse.class);

                    if (placeResponse.getStatusCode().is2xxSuccessful() && placeResponse.getBody() != null) {
                        double lat = placeResponse.getBody().getLat();
                        double lng = placeResponse.getBody().getLng();
                        
                        if (lat != 0.0 && lng != 0.0) {
                            return new Coordinates(
                                    new BigDecimal(String.valueOf(lat)),
                                    new BigDecimal(String.valueOf(lng))
                            );
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to call VietMap API for address: " + addressLine, e);
        }
        
        log.warn("VietMap returned no result or failed. Returning null.");
        return null;
    }

    @Data
    public static class Coordinates {
        private final BigDecimal latitude;
        private final BigDecimal longitude;
    }

    @Data
    public static class VietmapSearchResponse {
        private String ref_id;
        private String address;
        private String name;
    }

    @Data
    public static class VietmapPlaceResponse {
        private double lat;
        private double lng;
    }
}

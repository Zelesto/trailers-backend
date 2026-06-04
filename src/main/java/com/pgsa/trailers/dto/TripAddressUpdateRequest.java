package com.pgsa.trailers.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripAddressUpdateRequest {

    // Origin address details
    private String originStreetAddress;
    private String originCity;
    private String originZipCode;
    private String originProvince;
    private Double originLatitude;
    private Double originLongitude;

    // Destination address details
    private String destinationStreetAddress;
    private String destinationCity;
    private String destinationZipCode;
    private String destinationProvince;
    private Double destinationLatitude;
    private Double destinationLongitude;

    // Whether to auto-geocode missing coordinates
    private boolean autoGeocode = true;

    // Whether to update the legacy location fields
    private boolean updateLegacyFields = true;
}

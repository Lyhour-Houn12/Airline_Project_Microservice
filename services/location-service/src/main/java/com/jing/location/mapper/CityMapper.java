package com.jing.location.mapper;

import com.jing.common.dto.location.CityRequest;
import com.jing.common.dto.location.CityResponse;
import com.jing.location.model.City;

public class CityMapper {

    public static City toEntity(CityRequest cityRequest){
        if(cityRequest == null) return null;

        return City.builder()
                .cityName(cityRequest.getCityName())
                .cityCode(cityRequest.getCityCode())
                .countryCode(cityRequest.getCountryCode())
                .countryName(cityRequest.getCountryName())
                .regionCode(cityRequest.getRegionCode())
                .timeZoneId(cityRequest.getTimeZoneOffset())
                .build();
    }


    public static CityResponse toResponse(City city){
        if (city == null) return null;

        return CityResponse.builder()
                .id(city.getId())
                .name(city.getCityName())
                .cityCode(city.getCityCode())
                .countryCode(city.getCountryCode())
                .countryName(city.getCountryName())
                .regionCode(city.getRegionCode())
                .timeZoneOffset(city.getTimeZoneId())
                .build();
    }

    public static City cityUpdate(City city, CityRequest request){
        if (request.getCityName() != null) {
            city.setCityName(request.getCityName().trim());
        }
        if (request.getCityCode() != null) {
            city.setCityCode(request.getCityCode().toUpperCase().trim());
        }
        if (request.getCountryCode() != null) {
            city.setCountryCode(request.getCountryCode().toUpperCase().trim());
        }
        if (request.getCountryName() != null) {
            city.setCountryName(request.getCountryName().trim());
        }
        if (request.getRegionCode() != null) {
            city.setRegionCode(request.getRegionCode().toUpperCase().trim());
        }
        if (request.getTimeZoneOffset() != null) {
            city.setTimeZoneId(request.getTimeZoneOffset().trim());
        }
        return city;
    }
}

package com.rudyii.hsw.objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class WanIp {
    private String as, city, country, countryCode, isp,lat, lon, org, query, region, regionName, status, timeZone, zip;
}

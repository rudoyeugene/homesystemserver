package com.rudyii.hsw.objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class WanIp {
    @Builder.Default
    private String as = "";
    @Builder.Default
    private String city = "";
    @Builder.Default
    private String country = "";
    @Builder.Default
    private String countryCode = "";
    @Builder.Default
    private String isp = "";
    @Builder.Default
    private String lat = "";
    @Builder.Default
    private String lon = "";
    @Builder.Default
    private String org = "";
    @Builder.Default
    private String query = "";
    @Builder.Default
    private String region = "";
    @Builder.Default
    private String regionName = "";
    @Builder.Default
    private String status = "";
    @Builder.Default
    private String timeZone = "";
    @Builder.Default
    private String zip = "";
}

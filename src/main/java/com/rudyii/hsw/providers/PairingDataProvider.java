package com.rudyii.hsw.providers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rudyii.hs.common.objects.PairingData;
import com.rudyii.hsw.services.internet.IspService;
import com.rudyii.hsw.services.system.ServerKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PairingDataProvider {
    private final ServerKeyService serverKeyService;
    private final IspService ispService;
    @Value("#{hswProperties['server.port']}")
    private String serverPort;

    public PairingData getPairingData() {
        return PairingData.builder()
                .serverIp(ispService.getLocalIpAddress())
                .serverKey(serverKeyService.getServerKey().toString())
                .serverAlias(serverKeyService.getServerAlias())
                .serverPort(Integer.parseInt(serverPort))
                .build();
    }

    public String getQRCodeImageUrl() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return "https://api.qrserver.com/v1/create-qr-code/?size=256x256&data=" + objectMapper.writeValueAsString(getPairingData());
    }
}

package com.rudyii.hsw.providers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rudyii.hsw.objects.PairingData;
import com.rudyii.hsw.services.IspService;
import com.rudyii.hsw.services.UuidService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PairingDataProvider {

    private String serverPort;
    private UuidService uuidService;
    private IspService ispService;

    public PairingDataProvider(@Value("#{hswProperties['server.port']}") String serverPort,
                               UuidService uuidService, IspService ispService) {
        this.serverPort = serverPort;
        this.uuidService = uuidService;
        this.ispService = ispService;
    }

    public PairingData getPairingData() {
        return PairingData.builder()
                .serverIp(ispService.getLocalIpAddress())
                .serverKey(uuidService.getServerKey())
                .serverAlias(uuidService.getServerAlias())
                .serverPort(Integer.parseInt(serverPort))
                .build();
    }

    public String getQRCodeImageUrl() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return "https://api.qrserver.com/v1/create-qr-code/?size=256x256&data=" + objectMapper.writeValueAsString(getPairingData());
    }
}

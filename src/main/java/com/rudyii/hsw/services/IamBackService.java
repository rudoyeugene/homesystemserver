package com.rudyii.hsw.services;

import com.rudyii.hs.common.objects.IamBack;
import com.rudyii.hsw.services.system.ServerKeyService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class IamBackService {
    private final ArmedStateService armedStateService;
    private final ClientsService clientsService;
    private final ServerKeyService serverKeyService;

    public void disarmMeBy(IamBack iamBack) {
        boolean targetThis = serverKeyService.getServerKey().equals(iamBack.getServerKey());
        if (clientsService.getClients().stream().anyMatch(client -> client.getEmail().equals(iamBack.getEmail()))
                && targetThis) {
            armedStateService.disarmBy(iamBack.getEmail());
            log.info("System disarmed by {}", iamBack.getEmail());
        } else {
            log.warn("Skipping disarm request by {} for {} server", iamBack.getEmail(), iamBack.getServerKey());
        }
    }
}

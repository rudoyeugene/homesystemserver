package com.rudyii.hsw.actions;

import com.rudyii.hs.common.objects.message.FcmMessage;
import com.rudyii.hsw.actions.base.InternetBasedAction;
import com.rudyii.hsw.enums.FcmMessageEnum;
import com.rudyii.hsw.helpers.FCMSender;
import com.rudyii.hsw.services.internet.IspService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.rudyii.hsw.helpers.FCMSender.TYPE_TO;

@Slf4j
@Component
@Scope(value = "prototype")
public class FcmMessageSendAction extends InternetBasedAction implements Runnable {
    private final FCMSender fcmSender;
    private String recipientToken, name;
    private FcmMessage message;

    @Autowired
    public FcmMessageSendAction(FCMSender fcmSender, IspService ispService) {
        super(ispService);
        this.fcmSender = fcmSender;
    }

    public FcmMessageSendAction withData(String name, String recipientToken, FcmMessage message) {
        this.recipientToken = recipientToken;
        this.message = message;
        this.name = name;
        return this;
    }

    @Override
    public void run() {

        ensureInternetIsAvailable();

        try {
            FcmMessageEnum result = fcmSender.sendData(TYPE_TO, recipientToken, message);
            switch (result) {
                case SUCCESS:
                    log.info("FCMessage successfully sent to: {}", name);
                    break;
                case WARNING:
                    log.warn("FCMessage was not sent to: {} due to some internal Google issue.", name);
                    break;
            }
        } catch (IOException e) {
            log.error("Failed to send message to: {}", name, e);
        }
    }
}

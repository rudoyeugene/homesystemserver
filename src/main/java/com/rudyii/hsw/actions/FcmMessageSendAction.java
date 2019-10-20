package com.rudyii.hsw.actions;

import com.google.gson.JsonObject;
import com.rudyii.hsw.actions.base.InternetBasedAction;
import com.rudyii.hsw.enums.FcmMessageEnum;
import com.rudyii.hsw.helpers.FCMSender;
import com.rudyii.hsw.services.IspService;
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
    private String recipientToken, name;
    private JsonObject messageData;
    private FCMSender fcmSender;

    @Autowired
    public FcmMessageSendAction(FCMSender fcmSender, IspService ispService) {
        super(ispService);
        this.fcmSender = fcmSender;
    }

    public FcmMessageSendAction withData(String name, String recipientToken, JsonObject messageData) {
        this.recipientToken = recipientToken;
        this.messageData = messageData;
        this.name = name;
        return this;
    }

    @Override
    public void run() {

        ensureInternetIsAvailable();

        try {
            FcmMessageEnum result = fcmSender.sendData(TYPE_TO, recipientToken, messageData);
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

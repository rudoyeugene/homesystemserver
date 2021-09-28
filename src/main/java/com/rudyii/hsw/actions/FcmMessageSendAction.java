package com.rudyii.hsw.actions;

import com.rudyii.hs.common.objects.message.FcmMessage;
import com.rudyii.hsw.actions.base.InternetBasedAction;
import com.rudyii.hsw.helpers.FCMSender;
import com.rudyii.hsw.services.internet.IspService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Scope(value = "prototype")
public class FcmMessageSendAction extends InternetBasedAction implements Runnable {
    private final FCMSender fcmSender;
    private String recipientToken;
    private FcmMessage message;

    @Autowired
    public FcmMessageSendAction(FCMSender fcmSender, IspService ispService) {
        super(ispService);
        this.fcmSender = fcmSender;
    }

    public FcmMessageSendAction withData(String recipientToken, FcmMessage message) {
        this.recipientToken = recipientToken;
        this.message = message;
        return this;
    }

    @Override
    public void run() {
        ensureInternetIsAvailable();
        fcmSender.sendData(recipientToken, message);
    }
}

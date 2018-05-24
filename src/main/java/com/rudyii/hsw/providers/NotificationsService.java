package com.rudyii.hsw.providers;

import com.google.gson.JsonObject;
import com.rudyii.hsw.actions.base.ActionsFactory;
import com.rudyii.hsw.objects.Attachment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Created by jack on 12.03.17.
 */
@Service
public class NotificationsService {
    private ActionsFactory actionsFactory;

    @Lazy
    @Autowired
    public NotificationsService(ActionsFactory actionsFactory) {
        this.actionsFactory = actionsFactory;
    }

    public void sendEmail(String subject, ArrayList<String> body, ArrayList<Attachment> attachments, boolean forAdmin) {
        actionsFactory.addToQueueMailSenderAction(subject, body, attachments, forAdmin);
    }

    public void sendFcmMessage(String name, String recipientToken, JsonObject messageData) {
        actionsFactory.addToQueueFcmMessageSendAction(name, recipientToken, messageData);
    }
}

package com.rudyii.hsw.providers;

import com.rudyii.hsw.actions.FCMSender;
import com.rudyii.hsw.actions.base.ActionsFactory;
import com.rudyii.hsw.objects.Attachment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Created by jack on 12.03.17.
 */
@Service
public class NotificationsService {
    private static Logger LOG = LogManager.getLogger(NotificationsService.class);

    private ActionsFactory actionsFactory;
    private FCMSender fcmSender;

    @Value("${destination}")
    private String destination;

    @Lazy
    @Autowired
    public NotificationsService(FCMSender fcmSender, ActionsFactory actionsFactory) {
        this.fcmSender = fcmSender;
        this.actionsFactory = actionsFactory;
    }

    public void sendMessage(String subject, ArrayList<String> body, boolean forAdmin) {
        sendMessage(subject, body, null, forAdmin);
    }

    public void sendMessage(String subject, ArrayList<String> body, ArrayList<Attachment> attachments, boolean forAdmin) {
        switch (destination) {
            case "mail":
                actionsFactory.addToQueueMailSenderAction(subject, body, attachments, forAdmin);
                break;
            case "fcm":
                fcmSender.sendMessage(subject, body);
                break;
            case "both":
                fcmSender.sendMessage(subject, body);
                actionsFactory.addToQueueMailSenderAction(subject, body, attachments, forAdmin);
                break;
            default:
                LOG.error("Wrong destination provided, nothing will be send");
        }
    }
}

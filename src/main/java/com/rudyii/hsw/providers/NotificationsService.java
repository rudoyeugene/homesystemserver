package com.rudyii.hsw.providers;

import com.rudyii.hs.common.objects.message.FcmMessage;
import com.rudyii.hsw.actions.base.ActionsFactory;
import com.rudyii.hsw.objects.Attachment;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Lazy
@Service
@AllArgsConstructor
public class NotificationsService {
    private final ActionsFactory actionsFactory;

    public void sendEmail(String subject, ArrayList<String> body, ArrayList<Attachment> attachments) {
        actionsFactory.orderMailSenderAction(subject, body, attachments);
    }

    public void sendFcmMessage(String name, String recipientToken, FcmMessage message) {
        actionsFactory.orderMessageSendAction(name, recipientToken, message);
    }
}

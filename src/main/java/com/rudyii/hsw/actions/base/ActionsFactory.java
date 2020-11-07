package com.rudyii.hsw.actions.base;

import com.google.gson.JsonObject;
import com.rudyii.hsw.actions.FcmMessageSendAction;
import com.rudyii.hsw.actions.MailSendAction;
import com.rudyii.hsw.actions.UploadAction;
import com.rudyii.hsw.objects.Attachment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

@Component
public class ActionsFactory {
    private ApplicationContext context;
    private ThreadPoolTaskExecutor hswExecutor;

    @Autowired
    public ActionsFactory(ApplicationContext context, ThreadPoolTaskExecutor hswExecutor) {
        this.context = context;
        this.hswExecutor = hswExecutor;
    }

    @Async
    public void orderMailSenderAction(String subject, ArrayList<String> body, ArrayList<Attachment> attachments) {
        hswExecutor.execute(context.getBean(MailSendAction.class).withData(subject, body, attachments));
    }

    @Async
    public void orderMessageSendAction(String name, String recipientToken, JsonObject messageData) {
        hswExecutor.execute(context.getBean(FcmMessageSendAction.class).withData(name, recipientToken, messageData));
    }

    @Async
    public void orderUploadAction(String cameraName, File uploadCandidate, BufferedImage image) {
        hswExecutor.execute(context.getBean(UploadAction.class)
                .withUploadCandidate(uploadCandidate)
                .withCameraName(cameraName)
                .andImage(image));
    }
}

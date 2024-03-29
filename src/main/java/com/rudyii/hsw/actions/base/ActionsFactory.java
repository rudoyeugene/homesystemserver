package com.rudyii.hsw.actions.base;

import com.rudyii.hs.common.type.NotificationType;
import com.rudyii.hsw.actions.MailSendAction;
import com.rudyii.hsw.actions.UploadAction;
import com.rudyii.hsw.objects.Attachment;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

@Component
@AllArgsConstructor
public class ActionsFactory {
    private final ApplicationContext context;
    private final ThreadPoolTaskExecutor hswExecutor;

    @Async
    public void orderMailSenderAction(String subject, ArrayList<String> body, ArrayList<Attachment> attachments) {
        hswExecutor.submit(context.getBean(MailSendAction.class).withData(subject, body, attachments));
    }

    @Async
    public void orderUploadAction(String cameraName, NotificationType notificationType, File uploadCandidate, BufferedImage image) {
        hswExecutor.submit(context.getBean(UploadAction.class)
                .withUploadCandidate(uploadCandidate)
                .withCameraName(cameraName)
                .withNotificationType(notificationType)
                .andImage(image));
    }
}

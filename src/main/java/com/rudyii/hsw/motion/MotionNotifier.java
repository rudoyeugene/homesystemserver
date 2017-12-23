package com.rudyii.hsw.motion;

import com.rudyii.hsw.objects.Attachment;
import com.rudyii.hsw.objects.Camera;
import com.rudyii.hsw.providers.NotificationsService;
import org.apache.commons.math3.util.Precision;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by jack on 09.05.17.
 */
public class MotionNotifier {
    private static Logger LOG = LogManager.getLogger(MotionNotifier.class);

    private String timeStamp;
    private String bodyTimeStamp;

    private NotificationsService notificationsService;

    @Autowired
    public MotionNotifier(NotificationsService notificationsService) {
        this.notificationsService = notificationsService;
    }

    @Async
    void sendMotionFrameFrom(Camera camera, BufferedImage currentImage, BufferedImage motionObject, Double area) {
        generateTimestamps();

        ArrayList<Attachment> attachments = new ArrayList<>();
        ArrayList<String> body = new ArrayList<>();

        try {
            ByteArrayOutputStream byteArrayOutputStreamForCurrentImage = new ByteArrayOutputStream();
            ByteArrayOutputStream byteArrayOutputStreamForMotionObject = new ByteArrayOutputStream();

            ImageIO.write(currentImage, "jpeg", byteArrayOutputStreamForCurrentImage);
            ImageIO.write(motionObject, "jpeg", byteArrayOutputStreamForMotionObject);

            byte[] currentImageByteArray = byteArrayOutputStreamForCurrentImage.toByteArray();
            byte[] motionObjectByteArray = byteArrayOutputStreamForMotionObject.toByteArray();

            Attachment currentImageAttachment = new Attachment(camera.getName() + "_" + timeStamp + ".jpg", currentImageByteArray, "image/jpeg");
            Attachment motionObjectAttachment = new Attachment(camera.getName() + "_Motion_Object.jpg", motionObjectByteArray, "image/jpeg");

            attachments.add(currentImageAttachment);
            attachments.add(motionObjectAttachment);
        } catch (Exception e) {
            String subject = "Error occurred on camera: " + camera.getName();

            body.add("\nPFailed to extract image from camera: " + camera.getName());
            body.add("Failure occurred at " + new Date() + ", see the stacktrace for details below:\n\n");
            body.add("\n");
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                body.add(stackTraceElement.toString());
            }
            body.add("\n");

            notificationsService.sendMessage(subject, body, true);
            LOG.error("Failed to extract image from camera: " + camera.getName(), e);
            return;
        }

        body.add("Motion detected at: " + bodyTimeStamp + " from camera: " + camera.getName());
        body.add("Motion area size: " + Precision.round(area, 2) + "%");
        notificationsService.sendMessage("Motion detected!", body, attachments, false);
    }

    private void generateTimestamps() {
        timeStamp = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss.SSS").format(new Date());
        bodyTimeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
    }
}

package com.rudyii.hsw.motion;

import com.rudyii.hs.common.objects.settings.CameraSettings;
import com.rudyii.hsw.objects.events.CaptureEvent;
import com.rudyii.hsw.services.system.EventService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.apache.commons.lang.SystemUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Lazy
@Slf4j
@Component
@Scope(value = "prototype")
@RequiredArgsConstructor
public class VideoCaptor {
    private final EventService eventService;
    private CameraSettings cameraSettings;
    private String cameraName;
    private String rtspUrl;
    private File result;
    private BufferedImage image;
    private long eventTimeMillis;

    @Async
    public void startCaptureFrom(Camera camera) {
        this.eventTimeMillis = System.currentTimeMillis();
        this.cameraSettings = camera.getCameraSettings();
        this.cameraName = camera.getCameraName();
        this.rtspUrl = camera.getRtspUrl();

        this.result = new File(System.getProperty("java.io.tmpdir") + "/" + eventTimeMillis + ".mp4");

        log.info("A new motion detected: {}" + new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss.SSS").format(new Date()));

        try {
            this.image = ImageIO.read(new URL(camera.getJpegUrl()));
            startRecording();
        } catch (IOException e) {
            log.error("Failed to process output file", e);
        }

        publishCaptureEvent();
        camera.resetVideoCaptor();
    }

    @SneakyThrows
    private void startRecording() {
        FFmpeg ffmpeg;
        FFprobe ffprobe;
        if (SystemUtils.IS_OS_LINUX) {
            if (new File("/usr/bin/ffmpeg").exists() && new File("/usr/bin/ffprobe").exists()) {
                ffmpeg = new FFmpeg("/usr/bin/ffmpeg");
                ffprobe = new FFprobe("/usr/bin/ffprobe");
            } else {
                throw new IOException("/usr/bin/ffmpeg & /usr/bin/ffprobe are not found, can't capture");
            }
        } else if (SystemUtils.IS_OS_WINDOWS) {
            if (new File("C:/Windows/System32/ffmpeg.exe").exists() && new File("C:/Windows/System32/ffprobe.exe").exists()) {
                ffmpeg = new FFmpeg("C:/Windows/System32/ffmpeg.exe");
                ffprobe = new FFprobe("C:/Windows/System32/ffprobe.exe");
            } else {
                throw new IOException("C:/Windows/System32/ffmpeg.exe & C:/Windows/System32/ffprobe.exe are not found, can't capture");
            }
        } else {
            log.error("Unsupported OS detected, ignoring video capture");
            return;
        }

        FFmpegProbeResult probeResult = ffprobe.probe(rtspUrl);

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(probeResult)     // Filename, or a FFmpegProbeResult
                .overrideOutputFiles(true) // Override the output if it exists

                .addOutput(result.getCanonicalPath())   // Filename for the destination
                .setFormat("mp4")        // Format is inferred from filename, or can be set

                .setAudioCodec("aac")        // using the aac codec
                .setVideoCodec("libx264")     // Video using x264

                .setDuration(cameraSettings.getRecordLengthSec(), TimeUnit.SECONDS)

                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL) // Allow FFmpeg to use experimental specs
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

        // Run a one-pass encode
        long recordStarted = System.currentTimeMillis();
        executor.createJob(builder).run();
        log.info("Recording completed in {} ms", System.currentTimeMillis() - recordStarted);
        // Or run a two-pass encode (which is better quality at the cost of being slower)
        // executor.createTwoPassJob(builder).run();
    }

    private void publishCaptureEvent() {
        eventService.publish(CaptureEvent.builder()
                .cameraName(cameraName)
                .uploadCandidate(result)
                .image(image)
                .eventId(eventTimeMillis).build());
    }
}

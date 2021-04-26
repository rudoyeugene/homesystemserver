package com.rudyii.hsw.actions;

import com.rudyii.hsw.actions.base.InternetBasedAction;
import com.rudyii.hsw.objects.Attachment;
import com.rudyii.hsw.objects.Client;
import com.rudyii.hsw.providers.EmailDetailsProvider;
import com.rudyii.hsw.services.ClientsService;
import com.rudyii.hsw.services.IspService;
import lombok.extern.slf4j.Slf4j;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.email.EmailPopulatingBuilder;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.mail.Message.RecipientType;
import java.util.ArrayList;

import static com.rudyii.hsw.helpers.StringUtils.stringIsNotEmptyOrNull;

@Slf4j
@Component
@Scope(value = "prototype")
public class MailSendAction extends InternetBasedAction implements Runnable {
    private final EmailDetailsProvider emailDetailsProvider;
    private final ClientsService clientsService;
    private String subject;
    private ArrayList<String> body;
    private ArrayList<Attachment> attachments;

    public MailSendAction(IspService ispService, EmailDetailsProvider emailDetailsProvider,
                          ClientsService clientsService) {
        super(ispService);
        this.emailDetailsProvider = emailDetailsProvider;
        this.clientsService = clientsService;
    }

    public MailSendAction withData(String subject, ArrayList<String> body, ArrayList<Attachment> attachments) {
        this.subject = subject;
        this.body = body;
        this.attachments = attachments;

        return this;
    }

    @Override
    public void run() {
        Email email = buildEmail();

        ensureInternetIsAvailable();

        if (email != null) buildMailer().sendMail(email);
    }

    private Mailer buildMailer() {
        return MailerBuilder
                .withSMTPServer(emailDetailsProvider.getSmptServer(), emailDetailsProvider.getSmtpPort())
                .withSMTPServerUsername(emailDetailsProvider.getUsername())
                .withSMTPServerPassword(emailDetailsProvider.getPassword())
                .withTransportStrategy(TransportStrategy.SMTP_TLS)
                .buildMailer();
    }

    public Email buildEmail() {
        EmailPopulatingBuilder emailPopulatingBuilder = EmailBuilder
                .startingBlank()
                .from("Home System", emailDetailsProvider.getUsername());

        for (Client client : clientsService.getClients()) {
            if (!client.getHourlyReportMuted() && stringIsNotEmptyOrNull(client.getEmail())) {
                emailPopulatingBuilder.withRecipient(null, client.getEmail(), RecipientType.TO);
            }
        }

        if (emailPopulatingBuilder.getRecipients().isEmpty()) {
            return null;
        } else {
            emailPopulatingBuilder.withSubject(subject);

            if (body != null) {
                emailPopulatingBuilder.withHTMLText(String.join("<br>", body));
            } else {
                emailPopulatingBuilder.withPlainText("Empty body");
            }

            if (attachments != null) {
                attachments.forEach(attachment -> emailPopulatingBuilder.withAttachment(attachment.getName(), attachment.getData(), attachment.getMimeType()));
            }
        }

        return emailPopulatingBuilder.buildEmail();
    }
}

package com.rudyii.hsw.actions;

import com.rudyii.hsw.actions.base.InternetBasedAction;
import com.rudyii.hsw.objects.Attachment;
import com.rudyii.hsw.objects.Client;
import com.rudyii.hsw.providers.EmailDetailsProvider;
import com.rudyii.hsw.services.ClientsService;
import com.rudyii.hsw.services.IspService;
import lombok.extern.slf4j.Slf4j;
import org.simplejavamail.email.Email;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.config.TransportStrategy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.mail.Message.RecipientType;
import java.util.ArrayList;

import static com.rudyii.hsw.helpers.StringUtils.stringIsNotEmptyOrNull;

@Slf4j
@Component
@Scope(value = "prototype")
public class MailSendAction extends InternetBasedAction implements Runnable {
    private EmailDetailsProvider emailDetailsProvider;
    private ClientsService clientsService;
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
        return new Mailer(emailDetailsProvider.getSmptServer(), emailDetailsProvider.getSmptPort(), emailDetailsProvider.getUsername(), emailDetailsProvider.getPassword(), TransportStrategy.SMTP_TLS);
    }

    public Email buildEmail() {
        Email email = new Email();
        email.setFromAddress("Home System", emailDetailsProvider.getUsername());

        for (Client client : clientsService.getClients()) {
            if (!client.getHourlyReportMuted() && stringIsNotEmptyOrNull(client.getEmail())) {
                email.addRecipient(null, client.getEmail(), RecipientType.TO);
            }
        }

        if (email.getRecipients().isEmpty()) {
            return null;
        } else {
            email.setSubject(subject);

            if (body != null) {
                email.setTextHTML(String.join("<br>", body));
            } else {
                email.setText("Empty body");
            }

            if (attachments != null) {
                attachments.forEach(attachment -> email.addAttachment(attachment.getName(), attachment.getData(), attachment.getMimeType()));
            }
        }

        return email;
    }
}

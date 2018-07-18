package com.rudyii.hsw.actions;

import com.rudyii.hsw.actions.base.Action;
import com.rudyii.hsw.objects.Attachment;
import com.rudyii.hsw.objects.Client;
import com.rudyii.hsw.providers.EmailDetailsProvider;
import com.rudyii.hsw.services.ClientsService;
import com.rudyii.hsw.services.IspService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.simplejavamail.email.Email;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.config.TransportStrategy;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.mail.Message.RecipientType;
import java.util.ArrayList;

@Component
@Scope(value = "prototype")
public class MailSendAction implements Action {
    private static Logger LOG = LogManager.getLogger(MailSendAction.class);

    private EmailDetailsProvider emailDetailsProvider;
    private ClientsService clientsService;
    private String subject;
    private ArrayList<String> body;
    private ArrayList<Attachment> attachments;

    private IspService ispService;

    public MailSendAction(IspService ispService, EmailDetailsProvider emailDetailsProvider,
                          ClientsService clientsService) {
        this.ispService = ispService;
        this.emailDetailsProvider = emailDetailsProvider;
        this.clientsService = clientsService;
    }

    @Override
    public boolean fireAction() {
        if (ispService.internetIsAvailable()) {
            sendMessage();
            return true;
        } else {
            return false;
        }
    }

    public MailSendAction withData(String subject, ArrayList<String> body, ArrayList<Attachment> attachments) {
        this.subject = subject;
        this.body = body;
        this.attachments = attachments;

        return this;
    }

    @Async
    public void sendMessage() {
        Mailer mailer = new Mailer(emailDetailsProvider.getSmptServer(), emailDetailsProvider.getSmptPort(), emailDetailsProvider.getUsername(), emailDetailsProvider.getPassword(), TransportStrategy.SMTP_TLS);
        Email email = new Email();
        email.setSubject(subject);
        email.setFromAddress("Home System", emailDetailsProvider.getUsername());

        for (Client client : clientsService.getClients()) {
            if (!client.isHourlyReportMuted()) {
                email.addRecipient(null, client.getEmail(), RecipientType.TO);
            }
        }

        if (body != null) {
            email.setTextHTML(String.join("<br>", body));
        } else {
            email.setTextHTML(String.join("<br>", new ArrayList<>()));
        }

        if (attachments != null) {

            for (Attachment attachment : attachments) {
                email.addAttachment(attachment.getName(), attachment.getData(), attachment.getMimeType());
            }
        }

        try {
            if (subject != null && email.getRecipients().size() > 0) {
                mailer.sendMail(email);
            }
        } catch (Exception e) {
            LOG.error("Failed to send mail: ", e);
        }
    }
}
package com.rudyii.hsw.actions;

import com.rudyii.hsw.actions.base.Action;
import com.rudyii.hsw.objects.Attachment;
import com.rudyii.hsw.providers.EmailDetailsProvider;
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
    private String subject;
    private ArrayList<String> body;
    private ArrayList<Attachment> attachments;

    private IspService ispService;
    private boolean forAdmin;

    public MailSendAction(IspService ispService, EmailDetailsProvider emailDetailsProvider) {
        this.ispService = ispService;
        this.emailDetailsProvider = emailDetailsProvider;
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

    public MailSendAction withData(String subject, ArrayList<String> body, ArrayList<Attachment> attachments, boolean forAdmin) {
        this.subject = subject;
        this.body = body;
        this.attachments = attachments;
        this.forAdmin = forAdmin;

        return this;
    }

    @Async
    public void sendMessage() {
        String currentRecipients;

        if (forAdmin) {
            currentRecipients = emailDetailsProvider.getAdmin();
        } else {
            currentRecipients = emailDetailsProvider.getRecipients();
        }

        String logLine = "Email successfully sent to " + emailDetailsProvider.getRecipients() + " with subject: " + subject + ".";

        Mailer mailer = new Mailer(emailDetailsProvider.getSmptServer(), emailDetailsProvider.getSmptPort(), emailDetailsProvider.getUsername(), emailDetailsProvider.getPassword(), TransportStrategy.SMTP_TLS);
        Email email = new Email();
        email.setSubject(subject);
        email.setFromAddress("Home System", emailDetailsProvider.getUsername());
        String[] recipientsList = currentRecipients.split(",");

        for (String recipient : recipientsList) {
            email.addRecipient(null, recipient, RecipientType.TO);
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

            logLine = "Email successfully sent to " + emailDetailsProvider.getRecipients() + " with subject: " + subject + " and " + attachments.size() + " attachment(s)";
        }

        try {
            if (subject != null) {
                mailer.sendMail(email);
            } else {
                logLine = "Mail sending skipped due to subject is null";
            }
        } catch (Exception e) {
            logLine = "ERROR during sending mail:";
            LOG.error(logLine, e);
            return;
        }

        LOG.info(logLine);
    }

}
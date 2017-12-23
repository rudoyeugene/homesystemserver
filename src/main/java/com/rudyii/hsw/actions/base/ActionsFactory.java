package com.rudyii.hsw.actions.base;

import com.rudyii.hsw.actions.DropboxUploadAction;
import com.rudyii.hsw.actions.MailSendAction;
import com.rudyii.hsw.objects.Attachment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by jack on 06.06.17.
 */
@Component
public class ActionsFactory {
    private static Logger LOG = LogManager.getLogger(ActionsFactory.class);

    private List<Action> actionsListToBeFired;

    @Autowired
    private MailSendAction sendAction;

    @Autowired
    private DropboxUploadAction uploadAction;

    public ActionsFactory() {
        this.actionsListToBeFired = Collections.synchronizedList(new ArrayList());
    }

    @Async
    public void addToQueueMailSenderAction(String subject, ArrayList<String> body, ArrayList<Attachment> attachments, boolean forAdmin) {
        MailSendAction currentAction = sendAction.withData(subject, body, attachments, forAdmin);
        if (!currentAction.fireAction()) {
            actionsListToBeFired.add(currentAction);
        }
    }

    @Async
    public void addToQueueFCMSenderAction(File uploadCandidate) {

    }

    @Async
    public void addToQueueDropboxUploadAction(File uploadCandidate) {
        DropboxUploadAction currentAction = uploadAction.withUploadCandidate(uploadCandidate);
        if (!currentAction.fireAction()) {
            actionsListToBeFired.add(currentAction);
        }
    }

    @Scheduled(cron = "0 */1 * * * *")
    public void retryActions() {
        if (actionsListToBeFired.size() > 0) {
            LOG.info("Actions queue size = " + actionsListToBeFired.size());
            for (Action action : actionsListToBeFired){
                if (action.fireAction()) {
                    LOG.info(action.getClass().getSimpleName() + " successfully fired");
                    actionsListToBeFired.remove(action);
                } else {
                    LOG.error(action.getClass().getSimpleName() + " action firing failed, will retry next time");
                }
            }
        }
    }
}

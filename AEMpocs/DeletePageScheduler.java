package com.adobe.practise.website.core.schedulers;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.model.WorkflowModel;
import com.adobe.practise.website.core.schedulers.config.DeleteSchedulerConfig;
import com.day.cq.workflow.WorkflowService;

@Component(service = Runnable.class,
        immediate = true,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Scheduler to launch Delete Expired Pages workflow"
        }
)
@Designate(ocd = DeleteSchedulerConfig.class)
public class DeletePageScheduler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(DeletePageScheduler.class);

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private WorkflowService workflowService;

    private String cronExpression;
    private boolean isEnabled;
    private String workflowModelPath;
    private String pagePath;

    @Activate
    @Modified
    protected void activate(DeleteSchedulerConfig config) {
        this.cronExpression = config.updateExpression();
        this.isEnabled = config.enable();
        this.workflowModelPath = config.workflowModelPath();
        this.pagePath = config.pagePath();
        LOG.info("Scheduler activated with cron expression: {}, workflow model: {}, and page path: {}",
                cronExpression, workflowModelPath, pagePath);
        if (isEnabled && StringUtils.isNotBlank(cronExpression)) {
            ScheduleOptions options = scheduler.EXPR(cronExpression).name("DeleteExpiredPagesScheduler").canRunConcurrently(false);
            scheduler.schedule(this, options);
            LOG.info("Scheduler configured and scheduled with cron expression: {}", cronExpression);
        } else {
            LOG.warn("Scheduler is disabled or cron expression is blank.");
        }
    }

    @Override
    public void run() {
        if (!isEnabled) {
            LOG.warn("Scheduler is disabled.");
            return;
        }

        try (ResourceResolver resolver = getServiceResourceResolver()) {
            if (resolver == null) {
                LOG.error("Could not obtain Service ResourceResolver.");
                return;
            }

            WorkflowSession workflowSession = resolver.adaptTo(WorkflowSession.class);
            if (workflowSession == null) {
                LOG.error("Unable to adapt ResourceResolver to WorkflowSession.");
                return;
            }

            WorkflowModel workflowModel = workflowSession.getModel(workflowModelPath);
            if (workflowModel == null) {
                LOG.error("Workflow model not found at path: {}", workflowModelPath);
                return;
            }

            WorkflowData workflowData = workflowSession.newWorkflowData("JCR_PATH", pagePath);
            workflowSession.startWorkflow(workflowModel, workflowData);
            LOG.info("Workflow to delete old pages launched for page path: {}", pagePath);

        } catch (LoginException e) {
            LOG.error("Error obtaining service ResourceResolver: ", e);
        } catch (WorkflowException e) {
            LOG.error("Error launching workflow: ", e);
        } catch (Exception e) {
            LOG.error("Unexpected error: ", e);
        }
    }

    private ResourceResolver getServiceResourceResolver() throws LoginException {
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "Approver");
        return resolverFactory.getServiceResourceResolver(authInfo);
    }
}

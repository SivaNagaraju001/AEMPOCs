package com.adobe.practise.website.core.workflow;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.practise.website.core.service.impl.DeleteOldPagesServiceImpl;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;

@Component(
    service = WorkflowProcess.class,
    immediate = true,
    property = {"process.label=Delete Expired Pages"}
)
public class DeletePageWorkflow implements WorkflowProcess {

    @Reference
    private DeleteOldPagesServiceImpl deleteOldPagesService;

    private static final Logger LOG = LoggerFactory.getLogger(DeletePageWorkflow.class);

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) {
        try {
        	 String payloadPath = workItem.getWorkflowData().getPayload().toString();
        	 LOG.info(payloadPath);
        	 deleteOldPagesService.deleteOldPagesOneMonthAgo(payloadPath);
        } catch (Exception e) {
            LOG.error("Failed to delete old pages due to exception: ", e);
        }
    }
    
}

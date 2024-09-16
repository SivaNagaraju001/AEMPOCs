package com.adobe.practise.website.core.schedulers.config;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "DeleteScheduler Configuration")
public @interface DeleteSchedulerConfig {

    @AttributeDefinition(name = "Cron Expression", description = "Cron Expression for the scheduler")
    String updateExpression() default StringUtils.EMPTY;

    @AttributeDefinition(name = "Enable", description = "Enable or disable the delete page scheduler")
    boolean enable() default false;

    @AttributeDefinition(name = "Workflow Model Path", description = "Path to the workflow model for deleting expired pages")
    String workflowModelPath() default "/var/workflow/models/deleteexpiredpages";

    @AttributeDefinition(name = "Page Path", description = "Path to the pages to check for expiry")
    String pagePath() default StringUtils.EMPTY;
}

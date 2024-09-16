package com.adobe.practise.website.core.service.impl;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.practise.website.core.service.DeleteOldPagesService;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;

@Component(service = DeleteOldPagesServiceImpl.class)
public class DeleteOldPagesServiceImpl implements DeleteOldPagesService {

    @Reference
    private ResourceResolverFactory resolverFactory;
//    @Reference
//    private WorkItem workItem;
    @Reference
    private QueryBuilder queryBuilder;
//    private String payloadPath = workItem.getWorkflowData().getPayload().toString();
    private static final Logger LOG = LoggerFactory.getLogger(DeleteOldPagesServiceImpl.class);

    @Override
    public void deleteOldPagesOneMinuteAgo(String path) {
        ResourceResolver resolver = null;
        Session session = null;

        try {
          
            Map<String, Object> authInfo = new HashMap<>();
            authInfo.put(ResourceResolverFactory.SUBSERVICE, "Approver");
            resolver = resolverFactory.getServiceResourceResolver(authInfo);
            session = resolver.adaptTo(Session.class);

          
            SearchResult result = getSearchResult(session, path);

            if (result != null) {
                LOG.info("Found {} pages older than one minute.", result.getHits().size());

             
                Iterator<Node> nodes = result.getNodes();
                while (nodes.hasNext()) {
                    Node node = nodes.next();
                    LOG.info("Deleting node: {}", node.getPath());
//                    node.remove();
                }

            
                resolver.commit();
                LOG.info("Successfully deleted pages older than one minute.");
            }
        } catch (Exception e) {
            LOG.error("Error while deleting old pages: ", e);
        } finally {
            if (resolver != null) {
                resolver.close();
                LOG.info("ResourceResolver closed.");
            }
        }
    }
    
    @Override
    public void deleteOldPagesOneMonthAgo(String path) {
    	   ResourceResolver resolver = null;
           Session session = null;

        try {
        	 Map<String, Object> authInfo = new HashMap<>();
             authInfo.put(ResourceResolverFactory.SUBSERVICE, "Approver");
             resolver = resolverFactory.getServiceResourceResolver(authInfo);
             session = resolver.adaptTo(Session.class);

            Calendar now = Calendar.getInstance();
            Calendar oneMonthAgo = (Calendar) now.clone();
            oneMonthAgo.add(Calendar.MONTH, -1);

          
            String oneMonthAgoDate = String.format("%04d-%02d-%02dT00:00:00.000Z", 
                    oneMonthAgo.get(Calendar.YEAR), 
                    oneMonthAgo.get(Calendar.MONTH) + 1,  
                    oneMonthAgo.get(Calendar.DAY_OF_MONTH));

            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("path", path);
            queryMap.put("type", "cq:Page");
            queryMap.put("1_daterange.property", "jcr:created");
            queryMap.put("1_daterange.upperBound", oneMonthAgoDate);
            queryMap.put("1_daterange.upperOperation=","<=");
            
//            queryMap.put("1_daterange.upperBound", now.getTimeInMillis() + "");
            
            LOG.info(oneMonthAgoDate);
            Query query = queryBuilder.createQuery(PredicateGroup.create(queryMap), session);
            SearchResult result = query.getResult();

           
            Iterator<Node> nodes = result.getNodes();
            while (nodes.hasNext()) {
                Node node = nodes.next();
                LOG.info("Deleting node: {}", node.getPath());
//                node.remove();
            }
            session.save();
            LOG.info("Successfully deleted nodes created one month ago.");

        } catch (Exception e) {
            LOG.error("Error deleting pages: ", e);
        }
    }
    
    
    
    private SearchResult getSearchResult(Session session, String path) {
        try {
            Calendar now = Calendar.getInstance();
            Calendar oneMinuteAgo = (Calendar) now.clone();
            oneMinuteAgo.add(Calendar.MINUTE, -1);

            String oneMinuteAgoTimestamp = Long.toString(oneMinuteAgo.getTimeInMillis());
            String nowTimestamp = Long.toString(now.getTimeInMillis());

            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("path", path);
            queryMap.put("type", "cq:Page");
            queryMap.put("1_daterange.property", "jcr:created");
            queryMap.put("1_daterange.lowerBound", oneMinuteAgoTimestamp);
            queryMap.put("1_daterange.upperBound", nowTimestamp);

            Query query = queryBuilder.createQuery(PredicateGroup.create(queryMap), session);
            return query.getResult();
        } catch (Exception e) {
            LOG.error("Error building query: ", e);
            return null;
        }
    }

	public void startFlow() {
		LOG.info("WorkFlow Called From Scheduler,Deleting Pages");
//		deleteOldPagesOneMonthAgo(payloadPath);
		
	}
}

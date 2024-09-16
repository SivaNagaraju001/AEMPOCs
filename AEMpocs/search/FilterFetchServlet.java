package com.adobe.practise.website.core.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;


@Component(service = Servlet.class,
			property = {
					"sling.servlet.selector=tags",
					"sling.servlet.extension=json",
					"sling.servlet.resourceTypes=PractiseWebsite/components/searchbar",
					"sling.servlet.methods=GET"
			})
public class FilterFetchServlet extends SlingSafeMethodsServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

    @Reference
    private QueryBuilder queryBuilder;

    private static final Logger logger = LoggerFactory.getLogger(TagSearchServlet.class);

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        String path = request.getParameter("path");
        if (path == null || path.isEmpty()) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Path parameter is required\"}");
            return;
        }
        String resultJson = getAllTags(request, path);
        response.getWriter().write(resultJson);
    }

    private String getAllTags(SlingHttpServletRequest request, String path) {
        Session session = null;
        Set<String> allTags = new HashSet<>();
        StringBuilder jsonResult = new StringBuilder();
        
        try {
            session = request.getResourceResolver().adaptTo(Session.class);
            if (session != null) {
                Map<String, String> queryMap = new HashMap<>();
                queryMap.put("path", path);
                queryMap.put("type", "cq:Page");
                queryMap.put("property", "jcr:content/cq:tags");
                queryMap.put("property.operation", "exists");
                queryMap.put("p.limit", "-1");
                
                Query query = queryBuilder.createQuery(PredicateGroup.create(queryMap), session);
                SearchResult searchResult = query.getResult();
                
                List<Hit> hits = searchResult.getHits();
                for (Hit hit : hits) {
                    String pagePath = hit.getPath();
                    PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
                    Page page = pageManager.getPage(pagePath);
                    
                    if (page != null) {
                        String[] tags = page.getProperties().get("cq:tags", String[].class);
                        if (tags != null) {
                            for (String tag : tags) {
                                allTags.add(tag);
                            }
                        }
                    }
                }

                jsonResult.append("{\"tags\": [");
                allTags.forEach(tag -> jsonResult.append("\"").append(tag).append("\","));
                if (jsonResult.length() > 8 && jsonResult.charAt(jsonResult.length() - 1) == ',') {
                    jsonResult.deleteCharAt(jsonResult.length() - 1);
                }
                jsonResult.append("]}");
            } else {
                logger.error("Session is null, unable to execute the query.");
            }
        } catch (Exception e) {
            logger.error("Error executing query", e);
        } finally {
            if (session != null && session.isLive()) {
                session.logout();
            }
        }

        return jsonResult.toString();
    }

}

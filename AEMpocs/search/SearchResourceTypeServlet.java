package com.adobe.practise.website.core.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.selectors=search",
        "sling.servlet.resourceTypes=PractiseWebsite/components/searchbar",
        "sling.servlet.extensions=json",
        "sling.servlet.methods=GET"
    }
)
public class SearchResourceTypeServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(SearchResourceTypeServlet.class);

    @Reference
    private QueryBuilder queryBuilder;

    private static final int PAGE_SIZE = 8;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        String searchRoot = request.getParameter("root");
        String searchTerm = request.getParameter("q");
        String tags = request.getParameter("tags");
        int currentPage = Integer.parseInt(request.getParameter("page"));
        int offset = (currentPage - 1) * PAGE_SIZE;

        logger.debug("Received parameters - root: {}, searchTerm: {}, tags: {}, currentPage: {}", searchRoot, searchTerm, tags, currentPage);

        Session session = request.getResourceResolver().adaptTo(Session.class);

        if (session != null) {
            try {
                // Get the total number of results
                SearchResult result = executeSearchQuery(session, searchRoot, searchTerm, tags, 0); // Offset 0 for total count
                int totalResults = (int) result.getTotalMatches();

                // Get results for the current page
                result = executeSearchQuery(session, searchRoot, searchTerm, tags, offset);
                JSONArray pagesArray = createSearchResultJson(result);

                // Calculate total pages
                int totalPages = (int) Math.ceil((double) totalResults / PAGE_SIZE);

                // Create response JSON
                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("results", pagesArray);
                jsonResponse.put("totalResults", totalResults);
                jsonResponse.put("totalPages", totalPages);
                jsonResponse.put("currentPage", currentPage);
                jsonResponse.put("pageSize", PAGE_SIZE);

                sendJsonResponse(response, jsonResponse);
            } catch (Exception e) {
                handleError(response, "Failed to create JSON response", e);
            } finally {
                closeSession(session);
            }
        } else {
            handleError(response, "Session is null, unable to perform search", null);
        }
    }

    /**
     * Builds and executes the query using QueryBuilder.
     */
    private SearchResult executeSearchQuery(Session session, String searchRoot, String searchTerm, String tags, int offset) throws RepositoryException {
        Map<String, String> queryMap = buildQueryMap(searchRoot, searchTerm, tags, offset);
        Query query = queryBuilder.createQuery(PredicateGroup.create(queryMap), session);
        return query.getResult();
    }

    /**
     * Builds the query parameters for the QueryBuilder.
     */
    private Map<String, String> buildQueryMap(String searchRoot, String searchTerm, String tags, int offset) {
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("path", searchRoot);
        queryMap.put("type", "cq:Page");
        queryMap.put("fulltext.relPath", "jcr:content");

        if (searchTerm != null && !searchTerm.isEmpty()) {
            queryMap.put("fulltext", searchTerm);
        }

        queryMap.put("p.offset", String.valueOf(offset));
        queryMap.put("p.limit", String.valueOf(PAGE_SIZE));

        if (tags != null && !tags.isEmpty()) {
            String[] tagArray = tags.split(",");
            for (int i = 0; i < tagArray.length; i++) {
                queryMap.put("property." + (i + 1) + "_value", tagArray[i].trim());
            }
            queryMap.put("property.operation", "or");
            queryMap.put("property", "jcr:content/cq:tags");
        }
        return queryMap;
    }

    /**
     * Creates a JSON array from the search results.
     */
    private JSONArray createSearchResultJson(SearchResult result) throws RepositoryException, JSONException {
        JSONArray pagesArray = new JSONArray();
        for (Hit hit : result.getHits()) {
            JSONObject pageObject = createPageJsonObject(hit);
            pagesArray.put(pageObject);
        }
        return pagesArray;
    }

    /**
     * Extracts page details and returns them as a JSON object.
     */
    private JSONObject createPageJsonObject(Hit hit) throws RepositoryException, JSONException {
        String path = hit.getPath();
        Node pageNode = hit.getResource().adaptTo(Node.class);

        String title = "";
        String thumbnail = "";

        if (pageNode != null && pageNode.hasNode("jcr:content")) {
            Node contentNode = pageNode.getNode("jcr:content");
            title = getProperty(contentNode, "jcr:title", "");
            thumbnail = getThumbnail(contentNode);
        }

        JSONObject pageObject = new JSONObject();
        pageObject.put("path", path);
        pageObject.put("title", title);
        pageObject.put("thumbnail", thumbnail);
        return pageObject;
    }

    /**
     * Helper function to retrieve the thumbnail image.
     */
    private String getThumbnail(Node contentNode) throws RepositoryException {
        if (contentNode.hasNode("image") && contentNode.getNode("image").hasProperty("fileReference")) {
            return contentNode.getNode("image").getProperty("fileReference").getString();
        }
        return "";
    }

    /**
     * Helper function to get a property from a JCR Node.
     */
    private String getProperty(Node node, String propertyName, String defaultValue) throws RepositoryException {
        return node.hasProperty(propertyName) ? node.getProperty(propertyName).getString() : defaultValue;
    }

    /**
     * Sends the JSON response to the client.
     */
    private void sendJsonResponse(SlingHttpServletResponse response, JSONObject jsonResponse) throws IOException {
        logger.debug("JSON Response: {}", jsonResponse.toString());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonResponse.toString());
    }

    /**
     * Closes the JCR session.
     */
    private void closeSession(Session session) {
        if (session != null && session.isLive()) {
            session.logout();
        }
    }

    /**
     * Handles error scenarios by logging and sending an appropriate error response.
     */
    private void handleError(SlingHttpServletResponse response, String errorMessage, Exception e) throws IOException {
        logger.error(errorMessage, e);
        response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().write("{\"error\": \"" + errorMessage + "\"}");
    }
}

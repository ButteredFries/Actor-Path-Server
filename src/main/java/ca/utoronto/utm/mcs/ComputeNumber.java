package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.plaf.basic.BasicDirectoryModel;

import org.json.*;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ComputeNumber implements HttpHandler
{
    private static Memory memory;

    public ComputeNumber(Memory mem) {
        memory = mem;
    }

    public void handle(HttpExchange r) throws IOException {
        try {
            if (r.getRequestMethod().equals("GET")) {
                handleGet(r);
            }
            else {
            	String response = "";
            	r.sendResponseHeaders(404, response.length());
            	OutputStream os = r.getResponseBody();
            	os.write(response.getBytes());
            	os.close();
            }
        } catch (JSONException e) {
        	String response = "";
        	r.sendResponseHeaders(400, response.length());
        	OutputStream os = r.getResponseBody();
        	os.write(response.getBytes());
        	os.close();
        	
        } catch (Exception e) {
        	String response = "";
        	r.sendResponseHeaders(500, response.length());
        	OutputStream os = r.getResponseBody();
        	os.write(response.getBytes());
        	os.close();
        }
    }

    public void handleGet(HttpExchange r) throws IOException, JSONException {
    	
    	boolean badRequest = false;
    	
    	String body = Utils.convert(r.getRequestBody());
    	JSONObject deserialized = new JSONObject(body);
    	
        String actorId1 = "";
		String actorId2 = "";
       	
       	if (deserialized.has("actorId1") && deserialized.has("actorId2")) {
            actorId1 = deserialized.getString("actorId1");
			actorId2 = deserialized.getString("actorId2");
		}
        else
        	badRequest = true;
        
        
        if (!badRequest) {
        	Driver driver = GraphDatabase.driver("bolt://127.0.0.1:7687", AuthTokens.basic("neo4j", "1234"));
        	
        	boolean exist1 = false, exist2 = false, path = false;
    		String distance = "0";
        	
        	String transExists1 = "MATCH(a:actor) "
    				+ "WHERE a.id=\""+actorId1+"\""
    				+ "RETURN a.id";
			String transExists2 = "MATCH(a:actor) "
    				+ "WHERE a.id=\""+actorId2+"\""
    				+ "RETURN a.id";
        	
        	try (Session session = driver.session()) {
    			StatementResult result1 = session.run(transExists1);
				StatementResult result2 = session.run(transExists2);
				
    			if (result1.hasNext()) {
    				exist1 = true;
        		}
    			if (result2.hasNext()) {
    				exist2 = true;
        		}
    		}
        	
        	if (exist1 && exist2) {
        		String transDistance = "MATCH p=shortestPath("
        				+ "(b:actor {id:\""+actorId1+"\"})-[*]-(a:actor {id:\""+actorId2+"\"})"
        				+ ") RETURN length(p)";
        		
        		if(!actorId1.equals(actorId2)) {
        			try (Session session = driver.session()) {
        				StatementResult result = session.run(transDistance);
        				
        				while (result.hasNext()) {
        					distance = result.next().values().get(0).toString();
        					path = true;
        				}
        			}
        		}
        		if(path || actorId1.equals(actorId2)) {
        			
        			String output = "{\n\t\"Distance\": \""+Integer.parseInt(distance)/2+"\"\n}";
        			
        			String response = output;
        			r.sendResponseHeaders(200, response.length());
        			OutputStream os = r.getResponseBody();
        			os.write(response.getBytes());
        			os.close();
        		}
        		else {
        			String output = "{\n\t\"Distance\": \"undefined\"\n}";
        			String response = output;
        			r.sendResponseHeaders(200, response.length());
        			OutputStream os = r.getResponseBody();
        			os.write(response.getBytes());
        			os.close();
        		}
        	}
        	else {
        		String response = "";
        		r.sendResponseHeaders(400, response.length());
        		OutputStream os = r.getResponseBody();
        		os.write(response.getBytes());
        		os.close();
        	}
        }
        else {
        	String response = "";
        	r.sendResponseHeaders(400, response.length());
        	OutputStream os = r.getResponseBody();
        	os.write(response.getBytes());
        	os.close();		
        }
    }
}

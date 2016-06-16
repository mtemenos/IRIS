package com.temenos.interaction.winkext;

/*
 * #%L
 * interaction-winkext
 * %%
 * Copyright (C) 2012 - 2016 Temenos Holdings N.V.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.wink.common.DynamicResource;
import org.apache.wink.common.model.multipart.InMultiPart;

import com.temenos.interaction.core.command.CommandController;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.hypermedia.MethodNotAllowedException;
import com.temenos.interaction.core.hypermedia.ResourceState;
import com.temenos.interaction.core.hypermedia.ResourceStateMachine;
import com.temenos.interaction.core.hypermedia.ResourceStateProvider;
import com.temenos.interaction.core.resource.EntityResource;
import com.temenos.interaction.core.rim.HTTPHypermediaRIM;
import com.temenos.interaction.core.rim.HTTPResourceInteractionModel;
import com.temenos.interaction.core.rim.ResourceInteractionModel;

public class LazyResourceDelegate implements HTTPResourceInteractionModel, DynamicResource {

	private ResourceStateMachine hypermediaEngine;
	private ResourceStateProvider resourceStateProvider;
	private CommandController commandController;
	private Metadata metadata;

	private Map<String, Set<String>> resourceNamesToMethods = new HashMap<String, Set<String>>();
	private String path = null;	
		
	/**
	 * The class binding an instance of a ResourceState to a path
	 * @param hypermediaEngine registry of all resources
	 * @param resourceStateProvider interface that is used to lazily lookup/create ResourceState instance
	 * @param resourceName the state name
	 * @param path the resource path
	 */
	public LazyResourceDelegate(ResourceStateMachine hypermediaEngine,
			ResourceStateProvider resourceStateProvider, 
			CommandController commandController,
			Metadata metadata,
			String resourceName, 
			String path,
			Set<String> methods) {
		this.hypermediaEngine = hypermediaEngine;
		this.commandController = commandController;
		this.metadata = metadata;
		this.resourceStateProvider = resourceStateProvider;
		this.resourceNamesToMethods.put(resourceName, methods);
		this.path = path;
	}
	
	public void addResource(String name, Set<String> newMethods) {
		synchronized (resourceNamesToMethods) {
			Set<String> methods = resourceNamesToMethods.get(name);
			if (methods == null) {
				methods = new HashSet<String>();
			}
			methods.addAll(newMethods);
			resourceNamesToMethods.put(name, methods);
		}
	}
	
	@Override
    public String getBeanName() {
		return resourceNamesToMethods.toString();
    }

	@Override
    public void setBeanName(String beanName) {
        throw new AssertionError("Not supported");
    }

    public void setWorkspaceTitle(String workspaceTitle) {
        throw new AssertionError("Not supported");
    }

	@Override
    public String getWorkspaceTitle() {
		return "DefaultWorkspace";
    }

    public void setCollectionTitle(String collectionTitle) {
        throw new AssertionError("Not supported");
    }

	@Override
    public String getCollectionTitle() {
		return path;
    }

	@Override
    public String getPath() {
		return path;
    }

	@Override
    public void setParent(Object parent) {
        throw new AssertionError("Not supported");
    }

	@Override
    public HTTPResourceInteractionModel getParent() {
		return null;
    }
	
	private HTTPHypermediaRIM getResource(UriInfo uriInfo, String httpMethod) throws MethodNotAllowedException {
        ResourceState resourceState = resourceStateProvider.getResourceState(httpMethod, "/" + uriInfo.getPath(false));
        
        if(resourceState == null) {
        	return null;
        } else {
            String stateName = resourceState.getName();
            
            boolean loaded = resourceStateProvider.isLoaded(stateName);
            
            if(!loaded) {
                Map<String, Set<String>> stateNameToHttpMethods = resourceStateProvider.getResourceMethodsByState();
                Set<String> httpMethods = stateNameToHttpMethods.get(stateName);

                if(httpMethods == null) {
                    // Load a "real" IRIS state
                    hypermediaEngine.register(resourceState, httpMethod);                    
                } else {
                    for (String tmpHttpMethod : httpMethods) {
                        // We're dealing with a lazy state - Unload placeholder state
                        hypermediaEngine.unregister(resourceState, tmpHttpMethod);

                        // Load a "real" IRIS state
                        hypermediaEngine.register(resourceState, tmpHttpMethod);
                    }                    
                }
            }
        }
        
        return new HTTPHypermediaRIM(null, commandController, hypermediaEngine, metadata, resourceState.getPath(), false);        	    
	}

	@Override
	public Response get(HttpHeaders headers, String id, UriInfo uriInfo) {		
        try {
            HTTPHypermediaRIM rim = getResource(uriInfo, "GET");
            
            if(rim == null) {
                return Response.status(404).build();
            }
            
			return rim.get(headers, id, uriInfo);
		} catch (MethodNotAllowedException e) {
			return handleMethodNotAllowedException(e);			
		}
	}

	@Override
	public Response post(HttpHeaders headers, UriInfo uriInfo, InMultiPart inMP) {
	    
		try {
		    HTTPHypermediaRIM rim = getResource(uriInfo, "POST");
		    
            if(rim == null) {
                return Response.status(404).build();
            }
		    
			return rim.post(headers, uriInfo, inMP);
		} catch (MethodNotAllowedException e) {
			return handleMethodNotAllowedException(e);			
		}
	}	
	
	@Override
    public Response post( @Context HttpHeaders headers, @PathParam("id") String id, @Context UriInfo uriInfo, 
    		MultivaluedMap<String, String> formParams) {
		try {
		    HTTPHypermediaRIM rim = getResource(uriInfo, "POST");
		    
            if(rim == null) {
                return Response.status(404).build();
            }
		    
			return rim.post(headers, id, uriInfo, formParams);
		} catch (MethodNotAllowedException e) {
			return handleMethodNotAllowedException(e);			
		}
	}

	@Override
	public Response post(HttpHeaders headers, String id, UriInfo uriInfo, EntityResource<?> eresource) {
        try {
            HTTPHypermediaRIM rim = getResource(uriInfo, "POST");

            if(rim == null) {
                return Response.status(404).build();
            }
            
			return rim.post(headers, id, uriInfo, eresource);
		} catch (MethodNotAllowedException e) {
			return handleMethodNotAllowedException(e);			
		}
	}
	
	@Override
	public Response put(HttpHeaders headers, UriInfo uriInfo, InMultiPart inMP) {
		try {
		    HTTPHypermediaRIM rim = getResource(uriInfo, "PUT");
		    
            if(rim == null) {
                return Response.status(404).build();
            }
		    
			return rim.put(headers, uriInfo, inMP);
		} catch (MethodNotAllowedException e) {
			return handleMethodNotAllowedException(e);			
		}
	}	

	@Override
	public Response put(HttpHeaders headers, String id, UriInfo uriInfo, EntityResource<?> eresource) {
		try {
            HTTPHypermediaRIM rim = getResource(uriInfo, "PUT"); 
                    
            if(rim == null) {
                return Response.status(404).build();
            }
		    
			return rim.put(headers, id, uriInfo, eresource);
		} catch (MethodNotAllowedException e) {
			return handleMethodNotAllowedException(e);			
		}
		
	}

	@Override
	public Response delete(HttpHeaders headers, String id, UriInfo uriInfo) {	    
		try {
            HTTPHypermediaRIM rim = getResource(uriInfo, "DELETE");
            
            if(rim == null) {
                return Response.status(404).build();
            }
            
			return rim.delete(headers, id, uriInfo);
		} catch (MethodNotAllowedException e) {
			return handleMethodNotAllowedException(e);			
		}
	}

	private Response handleMethodNotAllowedException(MethodNotAllowedException e) {
		StringBuilder allowHeader = new StringBuilder();
		
		Set<String> allowedMethods = new HashSet<String>(e.getAllowedMethods());
		allowedMethods.add("HEAD");
		allowedMethods.add("OPTIONS");
		
		for(String method: allowedMethods) {				
			allowHeader.append(method);
			allowHeader.append(", ");
		}
		
		return Response.status(405).header("Allow", allowHeader.toString().substring(0, allowHeader.length() - 2)).build();
	}

	@Override
	public Response options(@Context HttpHeaders headers, @PathParam("id") String id, @Context UriInfo uriInfo) {	    
		try {
		    HTTPHypermediaRIM rim = getResource(uriInfo, "OPTIONS");

            if(rim == null) {
                return Response.status(404).build();
            }
		    
			return rim.options(headers, id, uriInfo);
		} catch (MethodNotAllowedException e) {
			return handleMethodNotAllowedException(e);			
		}
	}
	
	@Override
	public ResourceState getCurrentState() {
		return null;
	}

	@Override
	public String getResourcePath() {
		return path;
	}

	@Override
	public String getFQResourcePath() {
		return path;
	}

	@Override
	public Collection<ResourceInteractionModel> getChildren() {
		return null;
	}    
}

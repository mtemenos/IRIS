package com.temenos.interaction.winkext;

/*
 * #%L
 * interaction-winkext
 * %%
 * Copyright (C) 2012 - 2015 Temenos Holdings N.V.
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


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.wink.common.model.multipart.InMultiPart;
import org.junit.Test;

import com.temenos.interaction.core.command.CommandController;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.hypermedia.Event;
import com.temenos.interaction.core.hypermedia.MethodNotAllowedException;
import com.temenos.interaction.core.hypermedia.ResourceState;
import com.temenos.interaction.core.hypermedia.ResourceStateMachine;
import com.temenos.interaction.core.hypermedia.ResourceStateProvider;
import com.temenos.interaction.core.resource.EntityResource;

/**
 * @author mlambert
 *
 */
public class TestLazyResourceDelegate {
    @Test
    public void testRegularGet() throws MethodNotAllowedException {             
        ResourceStateProvider resourceStateProvider = mock(ResourceStateProvider.class);
        
        ResourceState myResourceState = mock(ResourceState.class);
        when(myResourceState.getName()).thenReturn("resource");
        when(myResourceState.getPath()).thenReturn("/myResource");    
        when(resourceStateProvider.getResourceState(eq("GET"), eq("/myResource"))).thenReturn(myResourceState);
        
        ResourceState rootState = mock(ResourceState.class);
        when(rootState.getName()).thenReturn("root");
        when(rootState.getPath()).thenReturn("/");    
        when(resourceStateProvider.getResourceState(eq("GET"), eq("/"))).thenReturn(rootState);
        
        Map<String,Set<String>> resourceMethodsByState = new HashMap<String, Set<String>>();
        
        Set<String> myResourceMethods = new HashSet<String>();
        myResourceMethods.add("GET");
        
        resourceMethodsByState.put("resource", myResourceMethods);
        resourceMethodsByState.put("root", myResourceMethods);
        when(resourceStateProvider.getResourceMethodsByState()).thenReturn(resourceMethodsByState);
        
        ResourceStateMachine resourceStateMachine = mock(ResourceStateMachine.class);
        Map<String, Set<String>> interactionsByPath = mock(HashMap.class);
        
        when(interactionsByPath.get(eq("/myResource"))).thenReturn(myResourceMethods);
        
        Set<String> rootMethods = new HashSet<String>();
        rootMethods.add("GET");
        when(interactionsByPath.get(eq("/"))).thenReturn(rootMethods);
                
        when(resourceStateMachine.getInteractionByPath()).thenReturn(interactionsByPath);
        
        LazyResourceDelegate lazyResourceDelegate = new LazyResourceDelegate(resourceStateMachine,
                resourceStateProvider, mock(CommandController.class), mock(Metadata.class), "test", "/", mock(HashSet.class));  
        
        HttpHeaders headers = mock(HttpHeaders.class);
        String id = "123";
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPathParameters(eq(false))).thenReturn(mock(MultivaluedMap.class));
        when(uriInfo.getQueryParameters(eq(false))).thenReturn(mock(MultivaluedMap.class));        
        when(uriInfo.getPath(eq(false))).thenReturn("myResource");
        
        lazyResourceDelegate.get(headers, id, uriInfo);
        
        verify(resourceStateProvider).getResourceState("GET", "/myResource");
        
        uriInfo = mock(UriInfo.class);
        when(uriInfo.getPathParameters(eq(false))).thenReturn(mock(MultivaluedMap.class));
        when(uriInfo.getQueryParameters(eq(false))).thenReturn(mock(MultivaluedMap.class));        
        when(uriInfo.getPath(eq(false))).thenReturn("");
        
        lazyResourceDelegate.get(headers, id, uriInfo);
        
        verify(resourceStateProvider).getResourceState("GET", "/");        
    }
    
	@Test
	public void testRegularPost() throws MethodNotAllowedException {				
		ResourceStateProvider resourceStateProvider = mock(ResourceStateProvider.class);
		ResourceState state = mock(ResourceState.class);
        when(state.getName()).thenReturn("resource");		
		when(state.getPath()).thenReturn("/myResource");	
		when(resourceStateProvider.getResourceState(eq("POST"), anyString())).thenReturn(state);		
		ResourceStateMachine resourceStateMachine = mock(ResourceStateMachine.class);
		Map<String, Set<String>> interactionsByPath = mock(HashMap.class);
		Set<String> methods = new HashSet<String>();
		methods.add("POST");
		
        Map<String,Set<String>> resourceMethodsByState = new HashMap<String, Set<String>>();               
        resourceMethodsByState.put("resource", methods);
        when(resourceStateProvider.getResourceMethodsByState()).thenReturn(resourceMethodsByState);
		
		when(interactionsByPath.get(anyString())).thenReturn(methods);
		when(resourceStateMachine.getInteractionByPath()).thenReturn(interactionsByPath);
		
		LazyResourceDelegate lazyResourceDelegate = new LazyResourceDelegate(resourceStateMachine,
				resourceStateProvider, mock(CommandController.class), mock(Metadata.class), "test", "/", mock(HashSet.class));	
		
		HttpHeaders headers = mock(HttpHeaders.class);
		String id = "123";
		UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPathParameters(eq(false))).thenReturn(mock(MultivaluedMap.class));	
        when(uriInfo.getQueryParameters(eq(false))).thenReturn(mock(MultivaluedMap.class));        
		when(uriInfo.getPath(eq(false))).thenReturn("myResource");
		EntityResource resource = mock(EntityResource.class);
	
		lazyResourceDelegate.post(headers, id, uriInfo, resource);
		
		verify(resourceStateProvider).getResourceState("POST", "/myResource");		
	}

	@Test
	public void testMultiPartPost() throws MethodNotAllowedException {				
        ResourceStateProvider resourceStateProvider = mock(ResourceStateProvider.class);
        ResourceState state = mock(ResourceState.class);
        when(state.getName()).thenReturn("resource");        
        when(state.getPath()).thenReturn("/myResource");
        when(state.getEntityName()).thenReturn("dummy");
        when(resourceStateProvider.getResourceState(eq("POST"), anyString())).thenReturn(state);
        ResourceStateMachine resourceStateMachine = mock(ResourceStateMachine.class);
        Map<String, Set<String>> interactionsByPath = mock(HashMap.class);
        Set<String> methods = new HashSet<String>();
        methods.add("POST");
        
        Map<String,Set<String>> resourceMethodsByState = new HashMap<String, Set<String>>();        
        resourceMethodsByState.put("resource", methods);
        when(resourceStateProvider.getResourceMethodsByState()).thenReturn(resourceMethodsByState);
        
        when(interactionsByPath.get(anyString())).thenReturn(methods);
        when(resourceStateMachine.getInteractionByPath()).thenReturn(interactionsByPath);

        when(resourceStateMachine.determineState(any(Event.class), anyString())).thenReturn(state);
        
        LazyResourceDelegate lazyResourceDelegate = new LazyResourceDelegate(resourceStateMachine,
                resourceStateProvider, mock(CommandController.class), mock(Metadata.class), "test", "/", mock(HashSet.class));  
        
        HttpHeaders headers = mock(HttpHeaders.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPathParameters(eq(false))).thenReturn(mock(MultivaluedMap.class));     
        when(uriInfo.getQueryParameters(eq(false))).thenReturn(mock(MultivaluedMap.class));        
        when(uriInfo.getPath(eq(false))).thenReturn("myResource");
        
        InMultiPart inMP = mock(InMultiPart.class);        
        lazyResourceDelegate.post(headers, uriInfo, inMP);
        
        verify(resourceStateProvider).getResourceState("POST", "/myResource");      			
	}

	@Test
	public void testRegularPut() throws MethodNotAllowedException {				
        ResourceStateProvider resourceStateProvider = mock(ResourceStateProvider.class);
        ResourceState state = mock(ResourceState.class);
        when(state.getName()).thenReturn("resource");        
        when(state.getPath()).thenReturn("/myResource");    
        when(resourceStateProvider.getResourceState(eq("PUT"), anyString())).thenReturn(state);
        ResourceStateMachine resourceStateMachine = mock(ResourceStateMachine.class);
        Map<String, Set<String>> interactionsByPath = mock(HashMap.class);
        Set<String> methods = new HashSet<String>();
        methods.add("PUT");
        
        Map<String,Set<String>> resourceMethodsByState = new HashMap<String, Set<String>>();
        
        resourceMethodsByState.put("resource", methods);
        when(resourceStateProvider.getResourceMethodsByState()).thenReturn(resourceMethodsByState);
        
        when(interactionsByPath.get(anyString())).thenReturn(methods);
        when(resourceStateMachine.getInteractionByPath()).thenReturn(interactionsByPath);
        
        LazyResourceDelegate lazyResourceDelegate = new LazyResourceDelegate(resourceStateMachine,
                resourceStateProvider, mock(CommandController.class), mock(Metadata.class), "test", "/", mock(HashSet.class));  
        
        HttpHeaders headers = mock(HttpHeaders.class);
        String id = "123";
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPathParameters(eq(false))).thenReturn(mock(MultivaluedMap.class)); 
        when(uriInfo.getQueryParameters(eq(false))).thenReturn(mock(MultivaluedMap.class));        
        when(uriInfo.getPath(eq(false))).thenReturn("myResource");
        EntityResource resource = mock(EntityResource.class);
        
        lazyResourceDelegate.put(headers, id, uriInfo, resource);  
        
        verify(resourceStateProvider).getResourceState("PUT", "/myResource");
	}
	
    @Test
    public void testDelete() throws MethodNotAllowedException {              
        ResourceStateProvider resourceStateProvider = mock(ResourceStateProvider.class);
        ResourceState state = mock(ResourceState.class);
        when(state.getName()).thenReturn("resource");        
        when(state.getPath()).thenReturn("/myResource");    
        when(resourceStateProvider.getResourceState(eq("DELETE"), anyString())).thenReturn(state);
        ResourceStateMachine resourceStateMachine = mock(ResourceStateMachine.class);
        Map<String, Set<String>> interactionsByPath = mock(HashMap.class);
        Set<String> methods = new HashSet<String>();
        methods.add("DELETE");
        
        Map<String,Set<String>> resourceMethodsByState = new HashMap<String, Set<String>>();               
        resourceMethodsByState.put("resource", methods);
        when(resourceStateProvider.getResourceMethodsByState()).thenReturn(resourceMethodsByState);
        
        when(interactionsByPath.get(anyString())).thenReturn(methods);
        when(resourceStateMachine.getInteractionByPath()).thenReturn(interactionsByPath);
        
        LazyResourceDelegate lazyResourceDelegate = new LazyResourceDelegate(resourceStateMachine,
                resourceStateProvider, mock(CommandController.class), mock(Metadata.class), "test", "/", mock(HashSet.class));  
        
        HttpHeaders headers = mock(HttpHeaders.class);
        String id = "123";
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPathParameters(eq(false))).thenReturn(mock(MultivaluedMap.class));    
        when(uriInfo.getQueryParameters(eq(false))).thenReturn(mock(MultivaluedMap.class));        
        when(uriInfo.getPath(eq(false))).thenReturn("myResource");
        EntityResource resource = mock(EntityResource.class);
        
        lazyResourceDelegate.delete(headers, id, uriInfo);  
        
        verify(resourceStateProvider).getResourceState("DELETE", "/myResource");
    }
    
    @Test
    public void testOptions() throws MethodNotAllowedException {              
        ResourceStateProvider resourceStateProvider = mock(ResourceStateProvider.class);
        ResourceState state = mock(ResourceState.class);
        when(state.getName()).thenReturn("resource");        
        when(state.getPath()).thenReturn("/myResource");    
        when(resourceStateProvider.getResourceState(eq("OPTIONS"), anyString())).thenReturn(state);
        ResourceStateMachine resourceStateMachine = mock(ResourceStateMachine.class);
        Map<String, Set<String>> interactionsByPath = mock(HashMap.class);
        Set<String> methods = new HashSet<String>();
        methods.add("OPTIONS");
        
        Map<String,Set<String>> resourceMethodsByState = new HashMap<String, Set<String>>();               
        resourceMethodsByState.put("resource", methods);
        when(resourceStateProvider.getResourceMethodsByState()).thenReturn(resourceMethodsByState);
        
        when(interactionsByPath.get(anyString())).thenReturn(methods);
        when(resourceStateMachine.getInteractionByPath()).thenReturn(interactionsByPath);
        
        LazyResourceDelegate lazyResourceDelegate = new LazyResourceDelegate(resourceStateMachine,
                resourceStateProvider, mock(CommandController.class), mock(Metadata.class), "test", "/", mock(HashSet.class));  
        
        HttpHeaders headers = mock(HttpHeaders.class);
        String id = "123";
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPathParameters(eq(false))).thenReturn(mock(MultivaluedMap.class));     
        when(uriInfo.getQueryParameters(eq(false))).thenReturn(mock(MultivaluedMap.class));                
        when(uriInfo.getPath(eq(false))).thenReturn("myResource");
        EntityResource resource = mock(EntityResource.class);
        
        lazyResourceDelegate.options(headers, id, uriInfo);  
        
        verify(resourceStateProvider).getResourceState("OPTIONS", "/myResource");
    }    	
	
	@Test
	public void testMultiPartPut() throws MethodNotAllowedException {				
        ResourceStateProvider resourceStateProvider = mock(ResourceStateProvider.class);
        ResourceState state = mock(ResourceState.class);
        when(state.getPath()).thenReturn("/myResource");
        when(state.getName()).thenReturn("resource");        
        when(state.getEntityName()).thenReturn("dummy");
        when(resourceStateProvider.getResourceState(eq("PUT"), anyString())).thenReturn(state);
        ResourceStateMachine resourceStateMachine = mock(ResourceStateMachine.class);
        Map<String, Set<String>> interactionsByPath = mock(HashMap.class);
        Set<String> methods = new HashSet<String>();
        methods.add("PUT");
        
        Map<String,Set<String>> resourceMethodsByState = new HashMap<String, Set<String>>();        
        resourceMethodsByState.put("resource", methods);
        when(resourceStateProvider.getResourceMethodsByState()).thenReturn(resourceMethodsByState);
        
        when(interactionsByPath.get(anyString())).thenReturn(methods);
        when(resourceStateMachine.getInteractionByPath()).thenReturn(interactionsByPath);

        when(resourceStateMachine.determineState(any(Event.class), anyString())).thenReturn(state);
        
        LazyResourceDelegate lazyResourceDelegate = new LazyResourceDelegate(resourceStateMachine,
                resourceStateProvider, mock(CommandController.class), mock(Metadata.class), "test", "/", mock(HashSet.class));  
        
        HttpHeaders headers = mock(HttpHeaders.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPathParameters(eq(false))).thenReturn(mock(MultivaluedMap.class));    
        when(uriInfo.getQueryParameters(eq(false))).thenReturn(mock(MultivaluedMap.class));        
        when(uriInfo.getPath(eq(false))).thenReturn("myResource");
        
        InMultiPart inMP = mock(InMultiPart.class);        
        lazyResourceDelegate.put(headers, uriInfo, inMP);
        
        verify(resourceStateProvider).getResourceState("PUT", "/myResource");                  
	}
}

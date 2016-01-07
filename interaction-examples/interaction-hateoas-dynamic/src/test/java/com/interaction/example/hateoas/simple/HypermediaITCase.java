package com.interaction.example.hateoas.simple;

/*
 * #%L
 * interaction-example-hateoas-simple
 * %%
 * Copyright (C) 2012 - 2013 Temenos Holdings N.V.
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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.test.framework.JerseyTest;
import com.temenos.interaction.media.hal.MediaType;
import com.theoryinpractise.halbuilder.api.Link;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;

/**
 * This test ensures that we can navigate from one application state to another using hypermedia (links).
 * 
 * @author aphethean
 */
public class HypermediaITCase extends JerseyTest {

	public HypermediaITCase() throws Exception {
		super();
	}
	
	@Before
	public void initTest() {
		// -DTEST_ENDPOINT_URI={someurl} to test with external server 
    	webResource = Client.create().resource(ConfigurationHelper.getTestEndpointUri(Configuration.TEST_ENDPOINT_URI)); 
	}

	@After
	public void tearDown() {}
		
	@Test
	public void tesResourceWithDynamicLink() {
		ClientResponse response = webResource.path("/notesDynmc").accept(MediaType.APPLICATION_HAL_JSON).get(ClientResponse.class);
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());

		RepresentationFactory representationFactory = new StandardRepresentationFactory();
		ReadableRepresentation resource = representationFactory.readRepresentation(MediaType.APPLICATION_HAL_JSON.toString(), new InputStreamReader(response.getEntityInputStream()));

		// the links from the collection
		List<Link> links = resource.getLinks();
		assertEquals(2, links.size());
		for (Link link : links) {
			if (link.getRel().equals("self")) {
				assertEquals(Configuration.TEST_ENDPOINT_URI + "/notesDynmc", link.getHref());
			} else if (link.getName().equals("DynmcNote.notesDynmc>POST>DynmcNote.createDynmcNote")) {
				assertEquals(Configuration.TEST_ENDPOINT_URI + "/notesDynmc", link.getHref());
			} else {
				fail("unexpected link [" + link.getName() + "]");
			}
		}
		
		// the items, and links on each item
		Collection<Map.Entry<String, ReadableRepresentation>> subresources = resource.getResources();
		assertNotNull(subresources);
		/*
		 * Test that there are actually some subresource returned.  If the 'self' link rel in
		 * the HALProvider is broken then we won't get any subresources here.
		 */
		assertTrue(subresources.size() > 0);
		
		boolean authorLinksFound = false;
		boolean systemLinksFound = false;
		
		for (Map.Entry<String, ReadableRepresentation> entry : subresources) {
			ReadableRepresentation item = entry.getValue();
			List<Link> itemLinks = item.getLinks();
			assertEquals(2, itemLinks.size());
						
			for (Link link : itemLinks) {
				if (link.getRel().contains("self")) {
					if(link.getHref().contains(Configuration.TEST_ENDPOINT_URI + "/authors/AU")) {
						authorLinksFound = true;
					}

					if(link.getHref().contains(Configuration.TEST_ENDPOINT_URI + "/systems/SY")) {
						systemLinksFound = true;
					}					
				} else if (link.getName().contains("DynmcNote.deletedDynmcNote")) {
					assertEquals(Configuration.TEST_ENDPOINT_URI + "/notesDynmc/" + item.getProperties().get("noteID"), link.getHref());
				} else {
					fail("unexpected link [" + link.getName() + "]");
				}
			}			
		}
		
		assertTrue(authorLinksFound);
		assertTrue(systemLinksFound);		
	}
	
	@Test
	public void testAutoTransionViaDynamicResource() {
		ClientResponse response = webResource.path("/authors/lookup/AU002").accept(MediaType.APPLICATION_HAL_JSON).get(ClientResponse.class);
        assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus()).getFamily());
        
		RepresentationFactory representationFactory = new StandardRepresentationFactory();
		ReadableRepresentation resource = representationFactory.readRepresentation(MediaType.APPLICATION_HAL_JSON.toString(), new InputStreamReader(response.getEntityInputStream()));
        
		List<Link> links = resource.getLinks();
		
		assertEquals(1, links.size());
		
		Link link = links.get(0);
		assertTrue(link.getRel().contains("self"));
		assertTrue(link.getHref().contains(Configuration.TEST_ENDPOINT_URI + "/authors/AU002"));
	}
}

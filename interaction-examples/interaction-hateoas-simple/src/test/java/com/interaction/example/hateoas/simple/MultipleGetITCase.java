package com.interaction.example.hateoas.simple;

/*
 * #%L
 * interaction-example-authorization-test
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

import java.io.InputStreamReader;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.test.framework.JerseyTest;
import com.temenos.interaction.media.hal.MediaType;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;

/**
 * Test that more than one Get command can be called from a resource. Check that
 * any stored ODataProducer remains present.
 */
public class MultipleGetITCase extends JerseyTest {

	public MultipleGetITCase() throws Exception {
		super();
	}

	@Before
	public void initTest() {
		webResource = Client.create().resource(ConfigurationHelper.getTestEndpointUri(Configuration.TEST_ENDPOINT_URI));
	}

	@After
	public void tearDown() {
	}

	@Test
	public void testGetTwiceNote() {
		// This resource uses a special test command that records how many times
		// it has been called. Note ID irrelevant.
		ClientResponse response = webResource.path("/twiceGetNote(1)").accept(MediaType.APPLICATION_HAL_JSON)
				.get(ClientResponse.class);
		assertEquals(Response.Status.Family.SUCCESSFUL, Response.Status.fromStatusCode(response.getStatus())
				.getFamily());

		RepresentationFactory representationFactory = new StandardRepresentationFactory();
		ReadableRepresentation resource = representationFactory.readRepresentation(MediaType.APPLICATION_HAL_JSON.toString(),new InputStreamReader(response
				.getEntityInputStream()));
		Map<String, Object> props = resource.getProperties();

		// Check result correct.
		assertEquals("1", props.get("noteID"));
		assertEquals("Called count = 2", props.get("body"));
	}
}

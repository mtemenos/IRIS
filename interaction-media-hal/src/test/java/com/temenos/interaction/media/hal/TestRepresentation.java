package com.temenos.interaction.media.hal;

/*
 * #%L
 * interaction-media-hal
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


import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

import com.theoryinpractise.halbuilder.api.Representation;
import com.theoryinpractise.halbuilder.api.RepresentationException;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;

/** These tests are not actually testing IRIS code, they are verifying the behaviour
 *  of the underlying halbuilder library.
 */
public class TestRepresentation {
	private RepresentationFactory representationFactory = new StandardRepresentationFactory();

	@Test
	public void testNull() throws Exception {
		URI id = new URI("http://example.org/test.svc/resource");
		Representation halResource = representationFactory.newRepresentation(id);

		String output = halResource.toString(RepresentationFactory.HAL_JSON);

                Map<String,Object> expectedData = parseJson("{\"_links\":{\"self\":{\"href\":\"http://example.org/test.svc/resource\"}}}");
		Map<String,Object> actualData   = parseJson(output);

                assertEquals( expectedData, actualData );                       
	}

	@Test
	public void testSimple() throws Exception {
		URI id = new URI("http://example.org/test.svc/resource");
		Representation halResource = representationFactory.newRepresentation(id);

		halResource.withProperty("key1", "value1");

		String output = halResource.toString(RepresentationFactory.HAL_JSON);

                Map<String,Object> expectedData = parseJson("{\"_links\":{\"self\":{\"href\":\"http://example.org/test.svc/resource\"}},\"key1\":\"value1\"}");
		Map<String,Object> actualData   = parseJson(output);

                assertEquals( expectedData, actualData );                
	}

	@Test
	public void testNested() throws Exception {
		URI id = new URI("http://example.org/test.svc/resource");
		Representation halResource = representationFactory.newRepresentation(id);

		halResource.withProperty("key1", "value1");

		HashMap<String,String> nested = new HashMap<String,String>();
		nested.put("key2a", "value2a");
		halResource.withProperty("key2", nested);

		String output = halResource.toString(RepresentationFactory.HAL_JSON);

                Map<String,Object> expectedData = parseJson("{\"_links\":{\"self\":{\"href\":\"http://example.org/test.svc/resource\"}},\"key1\":\"value1\",\"key2\":{\"key2a\":\"value2a\"}}");
		Map<String,Object> actualData   = parseJson(output);

                assertEquals( expectedData, actualData );
        }
		
	@Test
	public void testArray() throws Exception {
		URI id = new URI("http://example.org/test.svc/resource");
		Representation halResource = representationFactory.newRepresentation(id);

		halResource.withProperty("key1", "value1");

		ArrayList<Object> array = new ArrayList<Object>();
		HashMap<String,String> nest1 = new HashMap<String,String>();
		nest1.put("key2a", "value2a");
		HashMap<String,String> nest2 = new HashMap<String,String>();
		nest2.put("key2b", "value2b");
		array.add(nest1);array.add(nest2);
		
		halResource.withProperty("key2", array);

		String output = halResource.toString(RepresentationFactory.HAL_JSON);

                Map<String,Object> expectedData = parseJson("{\"_links\":{\"self\":{\"href\":\"http://example.org/test.svc/resource\"}},\"key1\":\"value1\",\"key2\":[{\"key2a\":\"value2a\"},{\"key2b\":\"value2b\"}]}");
		Map<String,Object> actualData   = parseJson(output);

                assertEquals( expectedData, actualData );
        }		
		
	@Test
	public void testArray1() throws Exception {
		URI id = new URI("http://example.org/test.svc/resource");
		Representation halResource = representationFactory.newRepresentation(id);

		halResource.withProperty("key1", "value1");

		ArrayList<Object> array = new ArrayList<Object>();
		HashMap<String,String> nest1 = new HashMap<String,String>();
		nest1.put("key2a", "value2a");
		HashMap<String,String> nest2 = new HashMap<String,String>();
		nest2.put("key2b", "value2b");
		array.add(nest1);array.add(nest2);
		
		halResource.withProperty("key2", array);

		String output = halResource.toString(RepresentationFactory.HAL_JSON);

                Map<String,Object> expectedData = parseJson("{\"_links\":{\"self\":{\"href\":\"http://example.org/test.svc/resource\"}},\"key1\":\"value1\",\"key2\":[{\"key2a\":\"value2a\"},{\"key2b\":\"value2b\"}]}");
		Map<String,Object> actualData   = parseJson(output);

                assertEquals( expectedData, actualData );
        }		

	@Test
	public void testMv() throws Exception {
		URI id = new URI("http://example.org/test.svc/resource");
		Representation halResource = representationFactory.newRepresentation(id);

		halResource.withProperty("key1", "value1");

		ArrayList<Object> array = new ArrayList<Object>();
		HashMap<String,String> nest1 = new HashMap<String,String>();
		nest1.put("valuePosition", "1");
		nest1.put("key2a", "value2a");
		HashMap<String,String> nest2 = new HashMap<String,String>();
		nest2.put("valuePosition", "2");
		nest2.put("key2b", "value2b");
		array.add(nest1);
		//array.add(nest2);
		
		halResource.withProperty("keyMvGroup", array);

		String output = halResource.toString(RepresentationFactory.HAL_JSON);
		Map<String,Object> expectedData = parseJson("{\"_links\":{\"self\":{\"href\":\"http://example.org/test.svc/resource\"}},\"key1\":\"value1\",\"keyMvGroup\":[{\"key2a\":\"value2a\",\"valuePosition\":\"1\"}]}");
		Map<String,Object> actualData   = parseJson(output);
		assertEquals( expectedData, actualData );
	}		

	public Map parseJson(String json) throws IOException {
		//converting json to Map
		byte[] mapData = json.getBytes();
		Map<String,Object> myMap = new HashMap<String, Object>();
		
		ObjectMapper objectMapper = new ObjectMapper();
		myMap = objectMapper.readValue(mapData, HashMap.class);
		return myMap;
	}        
}

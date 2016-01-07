package com.temenos.interaction.commands.mule;

/*
 * #%L
 * interaction-commands-mule
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


import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import com.temenos.interaction.core.MultivaluedMapImpl;
import com.temenos.interaction.core.entity.Entity;
import com.temenos.interaction.core.entity.EntityProperties;
import com.temenos.interaction.core.entity.EntityProperty;
import net.javacrumbs.jsonunit.core.Option;
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.examples.MultiLevelElementNameAndTextQualifier;

public class TestMule extends FunctionalTestCase {

	protected String getConfigResources() {
	    return "src/main/app/mule-conf.xml";
	}
	
	private final static String MAP_TO_XML = "<map><entry><string>description</string><string>21</string></entry><entry><string>name</string><string>Dirk</string></entry></map>";

	@Test
	public void testMapToXml() throws Exception {
	    MuleClient client = new MuleClient(muleContext);
	    Map<String,Object> inputMap = new HashMap<String,Object>();
	    inputMap.put("name", "Dirk");
	    inputMap.put("description", "21");
	    MuleMessage result = client.send("vm://maptoxml", inputMap, null);
	    assertNotNull(result);
	    assertNull(result.getExceptionPayload());
	    assertFalse(result.getPayload() instanceof NullPayload);
            Diff xmlDiff = new Diff(MAP_TO_XML, result.getPayloadAsString());
            DetailedDiff detDiff = new DetailedDiff(xmlDiff);
            detDiff.overrideElementQualifier(new MultiLevelElementNameAndTextQualifier(2));
            assertTrue(detDiff.similar());
	}

	private final static String DATAMAPPER_JSON = "{\"description\":\"21\",\"name\":\"Dirk\"}";

	@Test
	public void testDatamapper() throws Exception {
	    MuleClient client = new MuleClient(muleContext);
	    Map<String,Object> inputMap = new HashMap<String,Object>();
	    inputMap.put("name", "Dirk");
	    inputMap.put("description", "21");
	    MuleMessage result = client.send("vm://datamapper", inputMap, null);
	    assertNotNull(result);
	    assertNull(result.getExceptionPayload());
	    assertFalse(result.getPayload() instanceof NullPayload);
            JsonFluentAssert
                    .assertThatJson(DATAMAPPER_JSON)
                    .when(Option.IGNORING_ARRAY_ORDER)
                    .isEqualTo(result.getPayloadAsString());
	}
	
	private final static String VIEW_ENTITY_XML = "<?xml version='1.0' encoding='UTF-8'?><viewcommand><pathparameters><key>value</key></pathparameters><queryparameters><key>value</key></queryparameters></viewcommand>";

	@Test
	public void testViewCommandRequest() throws Exception {
	    MuleClient client = new MuleClient(muleContext);

		MultivaluedMap<String, String> pathParams = new MultivaluedMapImpl<String>();
		pathParams.putSingle("key", "value");
		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl<String>();
		queryParams.putSingle("key", "value");
		ViewCommandWrapper commandWrapper = new ViewCommandWrapper(pathParams, queryParams);

	    MuleMessage result = client.send("vm://view-command", commandWrapper, null);
	    assertNotNull(result);
	    assertNull(result.getExceptionPayload());
	    assertFalse(result.getPayload() instanceof NullPayload);
            Diff xmlDiff = new Diff(VIEW_ENTITY_XML, result.getPayloadAsString());
            DetailedDiff detDiff = new DetailedDiff(xmlDiff);
            detDiff.overrideElementQualifier(new MultiLevelElementNameAndTextQualifier(2));
            assertTrue(detDiff.similar());
	}
	
	private final static String ACTION_ON_ENTITY_XML = "<?xml version='1.0' encoding='UTF-8'?><actioncommand><pathparameters><id>CUST123</id></pathparameters><queryparameters><authorise>true</authorise></queryparameters><entity><name>Customer</name><id>123</id><name>Fred</name></entity></actioncommand>";
	
	@Test
	public void testActionCommandRequest() throws Exception {
	    MuleClient client = new MuleClient(muleContext);

		MultivaluedMap<String, String> pathParams = new MultivaluedMapImpl<String>();
		pathParams.putSingle("id", "CUST123");
		MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl<String>();
		queryParams.putSingle("authorise", "true");
		EntityProperties customerFields = new EntityProperties();
		customerFields.setProperty(new EntityProperty("id", "123"));
		customerFields.setProperty(new EntityProperty("name", "Fred"));
		Entity customer = new Entity("Customer", customerFields);
		ActionCommandWrapper commandWrapper = new ActionCommandWrapper(pathParams, queryParams, customer);

	    MuleMessage result = client.send("vm://action-command", commandWrapper, null);
	    assertNotNull(result);
	    assertNull(result.getExceptionPayload());
	    assertFalse(result.getPayload() instanceof NullPayload);
            Diff xmlDiff = new Diff(ACTION_ON_ENTITY_XML, result.getPayloadAsString());
            assertTrue(xmlDiff.similar());
	}

}

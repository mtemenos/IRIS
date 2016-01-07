package com.temenos.interaction.example.odata.notes;

/*
 * #%L
 * interaction-example-odata-notes
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Arrays;

import javax.ws.rs.core.Response;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.OEntity;
import org.odata4j.core.OProperties;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.format.Entry;
import org.odata4j.format.FormatParser;
import org.odata4j.format.FormatParserFactory;
import org.odata4j.format.FormatType;
import org.odata4j.format.Settings;
import org.odata4j.jersey.consumer.ODataJerseyConsumer;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.test.framework.JerseyTest;

public class CreateReadUpdateDeleteITCase extends JerseyTest {

	public final static String PERSONS_RESOURCE = "/Persons";
	public final static String NOTES_RESOURCE = "/Notes";
	
	private final static String NOTE_ENTITYSET_NAME = "Notes";
	private final static String PERSON_ENTITYSET_NAME = "Persons";

	@Before
	public void initTest() {
		// TODO make this configurable
		// test with external server 
    	webResource = Client.create().resource(Configuration.TEST_ENDPOINT_URI); 

    	// Create note 3, linked to person 2 if it doesn't exist
		ODataConsumer consumer = ODataJerseyConsumer.newBuilder(Configuration.TEST_ENDPOINT_URI).build();
		OEntity person = null;
		try {
				person = consumer.getEntity(PERSON_ENTITYSET_NAME, 2).execute();
		} catch (Exception e)  {
			// Ignore as Odata4j client 0.7 is expecting incorrect result
		}
		if (person == null) {
			person = consumer
						.createEntity(PERSON_ENTITYSET_NAME)
						.properties(OProperties.string("name", "Ron"))
						.execute();
		}
		OEntity note = null;
		try {
			note = consumer.getEntity(NOTE_ENTITYSET_NAME, 3).execute();
		} catch (Exception e) {
			// Ignore as Odata4j client 0.7 is expecting incorrect result
		}
		if (note == null) {
			note = consumer
					.createEntity(NOTE_ENTITYSET_NAME)
					.properties(OProperties.string("body", "test"))
					.link("NotePerson", person)
					.execute();
		}		
	}
	
	@After
	public void tearDown() {}

    public CreateReadUpdateDeleteITCase() throws Exception {
    	/* Allows standalone Jersey Test
    	super("example", "rest", "com.temenos.interaction.example");
		*/
        // enable logging on base web resource
    	System.setProperty("enableLogging", "ya");
    }
    
    @Test
	public void testOptions() {
        String noteUri = NOTES_RESOURCE + "(1)";
        ClientResponse response = webResource.path(noteUri).options(ClientResponse.class);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        assertEquals(5, response.getAllow().size());
        assertTrue(response.getAllow().contains("GET"));
        assertTrue(response.getAllow().contains("PUT"));
        assertTrue(response.getAllow().contains("DELETE"));
        assertTrue(response.getAllow().contains("OPTIONS"));
        assertTrue(response.getAllow().contains("HEAD"));
	}

    @Test
	public void testDeleteNote() {
		String noteUri = NOTES_RESOURCE + "(3)";

        // delete Note number 3 (which should now exists see initTest)
		ClientResponse response = webResource.path(noteUri).delete(ClientResponse.class);
        assertEquals(204, response.getStatus());

		// make sure Note number 3 is really gone
		ClientResponse deletedResponse = webResource.path(noteUri).get(ClientResponse.class);
        assertEquals(404, deletedResponse.getStatus());

		// delete Note number 56 (which does not exist)
		String notFoundNoteUri = NOTES_RESOURCE + "(56)";
		ClientResponse nresponse = webResource.path(notFoundNoteUri).delete(ClientResponse.class);
        assertEquals(404, nresponse.getStatus());
    }

    @Test
	public void testDeletePerson() {
		// delete Person number 1 (which exists), but we have bound a NoopDELETECommand
		String noteUri = PERSONS_RESOURCE + "(1)";
		ClientResponse response = webResource.path(noteUri).delete(ClientResponse.class);
        assertEquals(405, response.getStatus());
    }

    @Test
    public void testCreate() {
		ODataConsumer consumer = ODataJerseyConsumer.newBuilder(Configuration.TEST_ENDPOINT_URI).build();
		OEntity person = consumer.createEntity(PERSON_ENTITYSET_NAME)
				.properties(OProperties.string("name", "Ron"))
				.execute();

		assertTrue(person != null);
    }

    /**
     * Tests create with application/x-www-form-urlencoded request Content-Type.
     * 
     * @throws HttpException
     * @throws IOException
     */
    @Test
    public void testCreateUrlEncodedForm() throws HttpException, IOException {
		ODataConsumer consumer = ODataJerseyConsumer.newBuilder(Configuration.TEST_ENDPOINT_URI).build();
		EdmDataServices metadata = consumer.getMetadata();

		HttpClient client = new HttpClient();
        PostMethod postMethod = new PostMethod(Configuration.TEST_ENDPOINT_URI + PERSONS_RESOURCE);
        postMethod.setRequestEntity(new StringRequestEntity("name=RonOnForm&abcd=",
                                                            "application/x-www-form-urlencoded",
                                                            "UTF-8"));
        postMethod.addRequestHeader("Content-Type", PostMethod.FORM_URL_ENCODED_CONTENT_TYPE);
        postMethod.addRequestHeader("Accept", "application/atom+xml");
        
        String personId = null;
        try {
            client.executeMethod(postMethod);

            assertEquals(201, postMethod.getStatusCode());
            InputStream is = postMethod.getResponseBodyAsStream();

            InputStreamReader isr = new InputStreamReader(is);
            char[] buffer = new char[1];
            int read = 0;
            int offset = 0;
            while ((read = isr.read(buffer, offset, buffer.length - offset)) != -1) {
                offset += read;
                if (offset >= buffer.length) {
                	buffer = Arrays.copyOf(buffer, buffer.length * 2);
                }
            }
            char[] carr = Arrays.copyOf(buffer, offset);

            int checkEOF = is.read();
            assertEquals(-1, checkEOF);
            String str = new String(carr);

            assertEquals("application/atom+xml", postMethod.getResponseHeader("Content-Type").getValue());
            FormatParser<Entry> parser = FormatParserFactory.getParser(Entry.class,
            		FormatType.ATOM, new Settings(ODataConstants.DATA_SERVICE_VERSION, metadata, PERSON_ENTITYSET_NAME, null, null));
            Entry entry = parser.parse(new StringReader(str));
            personId = entry.getEntity().getProperty("id").getValue().toString();
            assertEquals("RonOnForm", entry.getEntity().getProperty("name").getValue().toString());
        } finally {
            postMethod.releaseConnection();
        }
        assertNotNull(personId);
        
		// read the person to check it was created ok
		OEntity person = consumer.getEntity(PERSON_ENTITYSET_NAME, Integer.valueOf(personId)).execute();
		assertTrue(person != null);
		assertEquals("RonOnForm", person.getProperty("name").getValue());
    }
    
    @Test
	public void testDelete() {
		ODataConsumer consumer = ODataJerseyConsumer.newBuilder(Configuration.TEST_ENDPOINT_URI).build();
		
		// find a person
		OEntity person = null;
		try {
			person = consumer.getEntity(PERSON_ENTITYSET_NAME, 2).execute();
		} catch (Exception e) {
			// Ignore as Odata4j client 0.7 is expecting incorrect result
		}
		if (person == null) {
			person = consumer
						.createEntity(PERSON_ENTITYSET_NAME)
						.properties(OProperties.string("name", "Ron"))
						.execute();
		}
	
		// create a note
		OEntity note = null;
		try {
			note = consumer.getEntity(NOTE_ENTITYSET_NAME, 6).execute();
		} catch (Exception e) {
			// Ignore as Odata4j client 0.7 is expecting incorrect result
		}
		if (note == null) {
			note = consumer
					.createEntity(NOTE_ENTITYSET_NAME)
					.properties(OProperties.string("body", "test"))
					.link("NotePerson", person)
					.execute();
		}		
		
		// delete one note
		consumer.deleteEntity(note).execute();

		// check its deleted
		OEntity afterDelete = null;
		boolean exceptionThrown = false;
		try {
			afterDelete = consumer.getEntity(note).execute();
		} catch (Exception e) {
			exceptionThrown = true;
		}
		assertEquals(true, exceptionThrown);
		assertEquals(null, afterDelete);
		
    }

    // TODO AtomXMLProvider needs better support for matching of URIs to resources
    @Test
    public void testUpdate() {
    	// Create note for person 1
		ODataConsumer consumer = ODataJerseyConsumer.newBuilder(Configuration.TEST_ENDPOINT_URI).build();
		OEntity person = consumer.getEntity(PERSON_ENTITYSET_NAME, 1).execute();
		OEntity note = consumer
					.createEntity(NOTE_ENTITYSET_NAME)
					.properties(OProperties.string("body", "test"))
					.link("NotePerson", person)
					.execute();
		// update the note text
		consumer.updateEntity(note)
				.properties(OProperties.string("body", "new text for note"))
				.execute();

		// read the note again, check text
		OEntity afterUpdate = consumer.getEntity(note).execute();
		assertEquals("new text for note", afterUpdate.getProperty("body").getValue());
		
    }
}
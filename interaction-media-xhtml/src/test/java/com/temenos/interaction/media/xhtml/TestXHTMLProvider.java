package com.temenos.interaction.media.xhtml;

/*
 * #%L
 * interaction-media-xhtml
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


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Test;
import org.odata4j.core.OEntities;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OLink;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;

import com.temenos.interaction.core.entity.Entity;
import com.temenos.interaction.core.entity.EntityMetadata;
import com.temenos.interaction.core.entity.EntityProperties;
import com.temenos.interaction.core.entity.EntityProperty;
import com.temenos.interaction.core.entity.GenericError;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.entity.vocabulary.Vocabulary;
import com.temenos.interaction.core.entity.vocabulary.terms.TermComplexGroup;
import com.temenos.interaction.core.entity.vocabulary.terms.TermComplexType;
import com.temenos.interaction.core.entity.vocabulary.terms.TermIdField;
import com.temenos.interaction.core.entity.vocabulary.terms.TermValueType;
import com.temenos.interaction.core.hypermedia.Link;
import com.temenos.interaction.core.resource.CollectionResource;
import com.temenos.interaction.core.resource.EntityResource;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.custommonkey.xmlunit.ComparisonController;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceEngine;
import org.custommonkey.xmlunit.DifferenceListener;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.examples.MultiLevelElementNameAndTextQualifier;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class TestXHTMLProvider {

	@Test
	public void testWriteEntityResourceAcceptHTML() throws Exception {
		EntityResource<Entity> er = new EntityResource<Entity>("Customer", createEntity("123", "Fred"));
		List<Link> links = new ArrayList<Link>();
		links.add(new Link(null, "Fred", "Self", "/Customer(123)", null, null, "GET", null));
		er.setLinks(links);

		//Serialize metadata resource
		XHTMLProvider p = new XHTMLProvider(createMockFlightMetadata());
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		p.writeTo(er, EntityResource.class, Entity.class, null, MediaType.TEXT_HTML_TYPE, null, bos);

		String responseString = new String(bos.toByteArray(), "UTF-8");
		Assert.assertTrue(responseString.contains("<li><a href=\"/Customer(123)\">Fred</a></li>"));
		Assert.assertTrue(responseString.contains("WD8 1LK"));
		Assert.assertTrue(responseString.contains("<td>hobbies</td><td><div contenteditable=\"true\">Tennis,Basketball,Swimming</div></td>"));
	}

	@Test
	public void testWriteOEntityResourceAcceptHTML() throws Exception {
		EntityResource<OEntity> er = createMockEntityResourceOEntity(createMockEdmEntitySet());
		List<Link> links = new ArrayList<Link>();
		links.add(new Link(null, "Fred", "Self", "/Customer(123)", null, null, "GET", null));
		er.setLinks(links);

		//Serialize metadata resource
		XHTMLProvider p = new XHTMLProvider(createMockFlightMetadata());
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		p.writeTo(er, EntityResource.class, OEntity.class, null, MediaType.TEXT_HTML_TYPE, null, bos);

		String responseString = new String(bos.toByteArray(), "UTF-8");
		Assert.assertTrue(responseString.contains("<li><a href=\"/Customer(123)\">Fred</a></li>"));
		Assert.assertTrue(responseString.contains("<td>name</td><td><div contenteditable=\"true\">Fred</div></td>"));
	}
	
	@Test
	public void testWriteEntityResourceAcceptXHTML() throws Exception {
		EntityResource<Entity> er = new EntityResource<Entity>("Customer", createEntity("123", "Fred"));
		List<Link> links = new ArrayList<Link>();
		links.add(new Link(null, "Fred", "self", "/Customer(123)", null, null, "GET", null));
		er.setLinks(links);

		//Serialize metadata resource
		XHTMLProvider p = new XHTMLProvider(createMockFlightMetadata());
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		p.writeTo(er, EntityResource.class, Entity.class, null, MediaType.APPLICATION_XHTML_XML_TYPE, null, bos);

		String responseString = new String(bos.toByteArray(), "UTF-8");
		Assert.assertTrue(responseString.contains("<dd>WD8 1LK</dd>"));
		Assert.assertTrue(responseString.contains("<ul><li><a href=\"/Customer(123)\" rel=\"self\">Fred</a></li></ul>"));
		Assert.assertTrue(responseString.contains("<link rel=\"self\" href=\"/Customer(123)\">"));
	}
	
	@Test
	public void testWriteCollectionResourceAcceptHTML() throws Exception {
		Collection<EntityResource<Entity>> entities = new ArrayList<EntityResource<Entity>>();
		entities.add(createEntityResource(createEntity("123", "Fred"), "123"));
		entities.add(createEntityResource(createEntity("456", "Tom"), "456"));
		entities.add(createEntityResource(createEntity("789", "Bob"), "789"));
		CollectionResource<Entity> cr = new CollectionResource<Entity>("Customer", entities);
		cr.setEntityName("Customer");

		//Serialize metadata resource
		XHTMLProvider p = new XHTMLProvider(createMockFlightMetadata());
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		p.writeTo(cr, CollectionResource.class, Entity.class, null, MediaType.TEXT_HTML_TYPE, null, bos);

		String responseString = new String(bos.toByteArray(), "UTF-8");
		Assert.assertTrue(responseString.contains("Customer"));
		Assert.assertTrue(responseString.contains("Tom"));
		Assert.assertTrue(responseString.contains("navigate('/Customer(456)')"));
	}
	
	@Test
	public void testWriteCollectionResourceAcceptXHTML() throws Exception {
		Collection<EntityResource<Entity>> entities = new ArrayList<EntityResource<Entity>>();
		entities.add(createEntityResource(createEntity("123", "Fred"), "123"));
		entities.add(createEntityResource(createEntity("456", "Tom"), "456"));
		entities.add(createEntityResource(createEntity("789", "Bob"), "789"));
		CollectionResource<Entity> cr = new CollectionResource<Entity>("Customer", entities);

		//Serialize metadata resource
		XHTMLProvider p = new XHTMLProvider(createMockFlightMetadata());
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		p.writeTo(cr, CollectionResource.class, Entity.class, null, MediaType.APPLICATION_XHTML_XML_TYPE, null, bos);

                String responseString = new String(bos.toByteArray(), "UTF-8");
                XpathEngine engine = XMLUnit.newXpathEngine();
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setValidating(false);
                dbf.setNamespaceAware(false);
                dbf.setFeature("http://xml.org/sax/features/namespaces", false);
                dbf.setFeature("http://xml.org/sax/features/validation", false);
                dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
                dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document responseDoc = db.parse(new InputSource(new StringReader(responseString)));

                XPath xpath = XPathFactory.newInstance().newXPath();
               // <li><dl><dt>id</dt><dd>123</dd><dt>address</dt><dl><dt>houseNumber</dt><dd>45</dd><dt>postcode</dt><dd>WD8 1LK</dd></dl><dt>name</dt><dd>Fred</dd><dt>hobbies</dt><dd>Tennis,Basketball,Swimming</dd></dl><ul><li><a href=\"/Customer(123)\" rel=\"self\">123</a></li></ul></li>"
                // the simplest XPath getting us desired <li> element - only this one will have <dd> element equal to 123
                String liNodeOfCustomer123XPath = "//li/dl[./dd=\"123\"]/dd[text()=\"Fred\"]";
                Assert.assertNotNull(xpath.evaluate(liNodeOfCustomer123XPath, responseDoc, XPathConstants.NODE));
        }
	
	@Test
	public void testWriteGenericErrorResourceAcceptHTML() throws Exception {
		EntityResource<GenericError> er = createMockEntityResourceGenericError();

		//Serialize metadata resource
		XHTMLProvider p = new XHTMLProvider(createMockFlightMetadata());
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		p.writeTo(er, EntityResource.class, GenericError.class, null, MediaType.TEXT_HTML_TYPE, null, bos);

		String responseString = new String(bos.toByteArray(), "UTF-8");
		Assert.assertTrue(responseString.contains("<div class=\"error\">[UPSTREAM_SERVER_UNAVAILABLE] Failed to connect to resource manager.</div>"));
	}

	@Test
	public void testWriteGenericErrorResourceAcceptXHTML() throws Exception {
		EntityResource<GenericError> er = createMockEntityResourceGenericError();

		//Serialize metadata resource
		XHTMLProvider p = new XHTMLProvider(createMockFlightMetadata());
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		p.writeTo(er, EntityResource.class, GenericError.class, null, MediaType.APPLICATION_XHTML_XML_TYPE, null, bos);

		String responseString = new String(bos.toByteArray(), "UTF-8");
		Assert.assertTrue(responseString.contains("<error>[UPSTREAM_SERVER_UNAVAILABLE] Failed to connect to resource manager.</error>"));
	}
	
	private Metadata createMockFlightMetadata() {
		//Define vocabulary for this entity
		Metadata metadata = new Metadata("Customers");
		EntityMetadata vocs = new EntityMetadata("Customer");
				
		Vocabulary vocName = new Vocabulary();
		vocName.setTerm(new TermComplexType(false));
		vocName.setTerm(new TermIdField(true));
		vocs.setPropertyVocabulary("name", vocName);
		
		Vocabulary vocAddress = new Vocabulary();
		vocAddress.setTerm(new TermComplexType(true));
		vocs.setPropertyVocabulary("address", vocAddress);
		
		Vocabulary vocNumbert = new Vocabulary();
		vocNumbert.setTerm(new TermComplexGroup("address"));
		vocNumbert.setTerm(new TermValueType(TermValueType.INTEGER_NUMBER));
		vocs.setPropertyVocabulary("number", vocNumbert);
		
		Vocabulary vocStreet = new Vocabulary();
		vocStreet.setTerm(new TermComplexGroup("address"));
		vocStreet.setTerm(new TermValueType(TermValueType.TEXT));
		vocs.setPropertyVocabulary("street", vocStreet);
		
		Vocabulary vocTown = new Vocabulary();
		vocTown.setTerm(new TermComplexGroup("address"));
		vocTown.setTerm(new TermValueType(TermValueType.TEXT));
		vocs.setPropertyVocabulary("town", vocStreet);
		
		Vocabulary vocPostCode = new Vocabulary();
		vocPostCode.setTerm(new TermComplexGroup("address"));
		vocPostCode.setTerm(new TermValueType(TermValueType.TEXT));
		vocs.setPropertyVocabulary("postCode", vocPostCode);
		
		Vocabulary vocDob = new Vocabulary();
		vocDob.setTerm(new TermComplexType(false));
		vocs.setPropertyVocabulary("dateOfBirth", vocDob);
		
		Vocabulary vocSector = new Vocabulary();
		vocSector.setTerm(new TermComplexType(false));
		vocs.setPropertyVocabulary("sector", vocSector);
		
		Vocabulary vocIndustry = new Vocabulary();
		vocIndustry.setTerm(new TermComplexType(false));
		vocs.setPropertyVocabulary("industry", vocIndustry);
		
		Vocabulary vocHobbies = new Vocabulary();
		vocHobbies.setTerm(new TermValueType(TermValueType.ENUMERATION));
		vocs.setPropertyVocabulary("hobbies", vocHobbies);
		
		metadata.setEntityMetadata(vocs);
		
		return metadata;
	}
	
	private Entity createEntity(String id, String name) {
		EntityProperties addressFields = new EntityProperties();
		addressFields.setProperty(new EntityProperty("postcode", "WD8 1LK"));
		addressFields.setProperty(new EntityProperty("houseNumber", "45"));
		
		EntityProperties customerFields = new EntityProperties();
		customerFields.setProperty(new EntityProperty("id", id));
		customerFields.setProperty(new EntityProperty("name", name));
		customerFields.setProperty(new EntityProperty("address", addressFields));
		String[] hobbies = { "Tennis", "Basketball",  "Swimming"};
		customerFields.setProperty(new EntityProperty("hobbies", hobbies));
		return new Entity("Customer", customerFields);
	}
	
	private EntityResource<Entity> createEntityResource(Entity entity, String id) {
		EntityResource<Entity> entityResource = new EntityResource<Entity>(entity.getName(), entity);
		Collection<Link> links = new ArrayList<Link>();
		links.add(new Link(null, id, "self", "/" + entity.getName() + "(" + id + ")", null, null, "GET", null));
		entityResource.setLinks(links);
		return entityResource;
	}
	
	private EdmEntitySet createMockEdmEntitySet() {
		// Create an entity set
		List<EdmProperty.Builder> eprops = new ArrayList<EdmProperty.Builder>();
		EdmProperty.Builder ep = EdmProperty.newBuilder("id").setType(EdmSimpleType.STRING);
		eprops.add(ep);
		EdmEntityType.Builder eet = EdmEntityType.newBuilder().setNamespace("InteractionTest").setName("Customer").addKeys(Arrays.asList("id")).addProperties(eprops);
		EdmEntitySet.Builder eesb = EdmEntitySet.newBuilder().setName("Customer").setEntityType(eet);
		return eesb.build();
	}

	private EntityResource<OEntity> createMockEntityResourceOEntity(EdmEntitySet ees) {
		//Create an OEntity
		OEntityKey entityKey = OEntityKey.create("123");
		List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		properties.add(OProperties.string("id", "123"));
		properties.add(OProperties.string("name", "Fred"));
		OEntity entity = OEntities.create(ees, entityKey, properties, new ArrayList<OLink>());
		EntityResource<OEntity> er = new EntityResource<OEntity>("Customer", entity) {};
		return er;
	}
	
	@SuppressWarnings("unchecked")
	private EntityResource<GenericError> createMockEntityResourceGenericError() {
		EntityResource<GenericError> er = mock(EntityResource.class);
				
		GenericError error = new GenericError("UPSTREAM_SERVER_UNAVAILABLE", "Failed to connect to resource manager.");
		when(er.getGenericEntity()).thenReturn(new GenericEntity<EntityResource<GenericError>>(er, er.getClass().getGenericSuperclass()));
		when(er.getEntity()).thenReturn(error);
		when(er.getEntityName()).thenReturn("Flight");
		return er;
	}	
}

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


import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Test;
import org.odata4j.core.OCollection;
import org.odata4j.core.OCollections;
import org.odata4j.core.OComplexObject;
import org.odata4j.core.OComplexObjects;
import org.odata4j.core.OEntities;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OLink;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmCollectionType;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.temenos.interaction.core.command.CommandHelper;
import com.temenos.interaction.core.entity.EntityMetadata;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.entity.vocabulary.Vocabulary;
import com.temenos.interaction.core.entity.vocabulary.terms.TermComplexType;
import com.temenos.interaction.core.entity.vocabulary.terms.TermValueType;
import com.temenos.interaction.core.resource.EntityResource;

/** Tests for output of complex OEntity structures to HAL JSON
 */
public class TestNesting {

	/* shared type objects */
	EdmEntitySet setType;
	EdmEntityType ridersType;
	EdmCollectionType ridesCollectionType;
	EdmComplexType ridesType;


	@Before
	public void setup() {
		EdmEntitySet dummy = createMockRidersEntitySet();
	}

	public Map parseJson(String json) throws IOException {
		//converting json to Map
		byte[] mapData = json.getBytes();
		Map<String,Object> myMap = new HashMap<String, Object>();
		
		ObjectMapper objectMapper = new ObjectMapper();
		myMap = objectMapper.readValue(mapData, HashMap.class);
		return myMap;
	}		
	
	private Metadata createMockRiderVocabMetadata() {
		EntityMetadata vocs = new EntityMetadata("Riders");
		Vocabulary vocId = new Vocabulary();
		vocId.setTerm(new TermValueType(TermValueType.TEXT));
		vocs.setPropertyVocabulary("name", vocId);
		Vocabulary vocBody = new Vocabulary();
		vocBody.setTerm(new TermValueType(TermValueType.INTEGER_NUMBER));
		vocs.setPropertyVocabulary("age", vocBody);

		Vocabulary vocRides = new Vocabulary();
		vocRides.setTerm(new TermComplexType(true));
		vocs.setPropertyVocabulary("rides", vocRides);
		Vocabulary vocHorseName = new Vocabulary();
		vocHorseName.setTerm(new TermValueType(TermValueType.TEXT));
		vocs.setPropertyVocabulary("HorseName", vocHorseName, Collections.enumeration(Collections.singletonList("rides")));
		Vocabulary vocHorseSize = new Vocabulary();
		vocHorseName.setTerm(new TermValueType(TermValueType.TEXT));
		vocs.setPropertyVocabulary("HorseSize", vocHorseSize, Collections.enumeration(Collections.singletonList("rides")));
		
		Metadata metadata = new Metadata("Family");
		metadata.setEntityMetadata(vocs);
		return metadata;
	}
	

	private String makeSingleLineString(ByteArrayOutputStream bos) throws Exception {
		String responseString = new String(bos.toByteArray(), "UTF-8");
		responseString = responseString.replaceAll(System.getProperty("line.separator"), "");
		responseString = responseString.replaceAll(">\\s+<", "><");
		return responseString;
	}
	

	/* a rider is a child who rides horses */
	private EdmEntitySet createMockRidersEntitySet() {
		List<EdmProperty.Builder> subprops = new ArrayList<EdmProperty.Builder>();
		subprops.add(EdmProperty.newBuilder("HorseName").setType(EdmSimpleType.STRING));
		subprops.add(EdmProperty.newBuilder("HorseSize").setType(EdmSimpleType.STRING));
		ridesType = EdmComplexType.newBuilder().setNamespace("InteractionTest").setName("RidersRides").addProperties(subprops).build();
		
		List<EdmProperty.Builder> eprops = new ArrayList<EdmProperty.Builder>();
		eprops.add(EdmProperty.newBuilder("ID").setType(EdmSimpleType.STRING));
		eprops.add(EdmProperty.newBuilder("name").setType(EdmSimpleType.STRING));
		eprops.add(EdmProperty.newBuilder("age").setType(EdmSimpleType.STRING));
		eprops.add(EdmProperty.newBuilder("rides").setType(new EdmCollectionType(EdmProperty.CollectionKind.Bag, ridesType)));
				   
		EdmEntityType.Builder eet = EdmEntityType.newBuilder().setNamespace("InteractionTest").setName("Riders").addKeys(Arrays.asList("ID")).addProperties(eprops);
		EdmEntitySet.Builder ees = EdmEntitySet.newBuilder().setName("Riders").setEntityType(eet);

		setType = ees.build();
		ridersType = (EdmEntityType)setType.getType();
		ridesCollectionType = (EdmCollectionType)ridersType.findDeclaredProperty("rides").getType();
		ridesType = (EdmComplexType)ridesCollectionType.getItemType();

		return setType;
	}

	private OComplexObject makeHorse(String name, String size) {
		List<OProperty<?>> subproperties = new ArrayList<OProperty<?>>();
		subproperties.add(OProperties.string("HorseName", name));
		subproperties.add(OProperties.string("HorseSize", size));
		return OComplexObjects.create(ridesType, subproperties);
	}

	/** A structure with a collection of a complex type in it
	 */
	@Test
	public void testSerialiseNestedResource() throws Exception {

		// the test key
		OEntityKey entityKey = OEntityKey.create("123");

		// the test properties
		List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		properties.add(OProperties.string("name", "noah"));
		properties.add(OProperties.string("age", "2"));

		OCollection rides = OCollections.newBuilder(ridesCollectionType).
			add(makeHorse("Harley", "12.2")).
			add(makeHorse("Donny", "13.2")).
			build();

		properties.add(OProperties.collection("Riders_rides", ridesCollectionType, rides));


		OEntity entity = OEntities.create(setType, entityKey, properties, new ArrayList<OLink>());
		EntityResource<OEntity> er = CommandHelper.createEntityResource(entity, OEntity.class);
		er.setEntityName("Riders");

		HALProvider hp = new HALProvider(createMockRiderVocabMetadata());
		UriInfo mockUriInfo = mock(UriInfo.class);
		when(mockUriInfo.getBaseUri()).thenReturn(new URI("http://www.temenos.com/rest.svc/"));
		hp.setUriInfo(mockUriInfo);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		hp.writeTo(er, EntityResource.class, OEntity.class, null, MediaType.APPLICATION_HAL_JSON_TYPE, null, bos);

		String expectedJSON = "{'_links':{'self':{'href':'http://www.temenos.com/rest.svc/'}},'age':'2','name':'noah','rides':[{'HorseSize':'12.2','HorseName':'Harley'},{'HorseName':'Donny','HorseSize':'13.2'}]}".replace('\'','\"');

		String responseString = makeSingleLineString(bos);
		System.err.println(responseString);

		Map<String,Object> expectedData = parseJson(expectedJSON);
		Map<String,Object> actualData   = parseJson(responseString);

		assertEquals(expectedData, actualData);
	}

	/** A structure with a collection of a complex type in it
	 *  This collection has only one element; the default behaviour
	 *  as of Halbuilder 3 is to remove the js array when there is only
	 *  one element. This checks that that behaviour is overridden and
	 *  the array is still there.
	 */
	@Test
	public void testSerialiseNestedResource1() throws Exception {

		// the test key
		OEntityKey entityKey = OEntityKey.create("123");

		// the test properties
		List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
		properties.add(OProperties.string("name", "noah"));
		properties.add(OProperties.string("age", "2"));

		OCollection rides = OCollections.newBuilder(ridesCollectionType).
			add(makeHorse("Harley", "12.2")).
			//			add(makeHorse("Donny", "13.2")).
			build();

		properties.add(OProperties.collection("Riders_rides", ridesCollectionType, rides));


		OEntity entity = OEntities.create(setType, entityKey, properties, new ArrayList<OLink>());
		EntityResource<OEntity> er = CommandHelper.createEntityResource(entity, OEntity.class);
		er.setEntityName("Riders");

		HALProvider hp = new HALProvider(createMockRiderVocabMetadata());
		UriInfo mockUriInfo = mock(UriInfo.class);
		when(mockUriInfo.getBaseUri()).thenReturn(new URI("http://www.temenos.com/rest.svc/"));
		hp.setUriInfo(mockUriInfo);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		hp.writeTo(er, EntityResource.class, OEntity.class, null, MediaType.APPLICATION_HAL_JSON_TYPE, null, bos);

		String expectedJSON = "{'_links':{'self':{'href':'http://www.temenos.com/rest.svc/'}},'age':'2','name':'noah','rides':[{'HorseSize':'12.2','HorseName':'Harley'}]}".replace('\'','\"');

		String responseString = makeSingleLineString(bos);
		System.err.println(responseString);

		Map<String,Object> expectedData = parseJson(expectedJSON);
		Map<String,Object> actualData   = parseJson(responseString);

		assertEquals(expectedData, actualData);
	}
}

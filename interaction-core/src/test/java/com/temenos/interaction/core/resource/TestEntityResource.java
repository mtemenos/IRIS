package com.temenos.interaction.core.resource;

/*
 * #%L
 * interaction-core
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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.CoreMatchers.not;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.odata4j.core.OEntity;
import org.odata4j.core.OProperty;

import com.temenos.interaction.core.NestedObject;
import com.temenos.interaction.core.entity.Entity;
import com.temenos.interaction.core.hypermedia.Link;
import com.temenos.interaction.core.hypermedia.Transition;

public class TestEntityResource {

	private @Spy EntityResource<OEntity> oEntityEntityResource;
	private @Spy EntityResource<Entity> entityEntityResource;
	
	private @Mock EntityResource<OEntity> oEntityEntityResourceMock;
	private @Mock EntityResource<Entity> entityEntityResourceMock;
	
	private @Mock OProperty<String> oProperty;
	private @Mock Entity entity;
	private @Mock OEntity oEntity;
	
	private Map<Transition, RESTResource> embeddedDummy = new HashMap<Transition, RESTResource>();
	private Collection<Link> linksDummy = new ArrayList<Link>();
	private String entityTagDummy = "MyResource";
	
	@Before
	public void setUp(){
		MockitoAnnotations.initMocks(this);
		
		when(this.oEntityEntityResource.getEntity()).thenReturn(this.oEntity);
		when(this.entityEntityResource.getEntity()).thenReturn(this.entity);
		
		this.oEntityEntityResource = spy(new EntityResource<OEntity>("company", this.oEntity));
		this.entityEntityResource = spy(new EntityResource<Entity>("user", this.entity));
		
		this.oEntityEntityResource.setEmbedded(this.embeddedDummy);
		this.oEntityEntityResource.setLinks(this.linksDummy);
		this.oEntityEntityResource.setEntityTag(this.entityTagDummy);
		
		this.entityEntityResource.setEmbedded(this.embeddedDummy);
		this.entityEntityResource.setLinks(this.linksDummy);
		this.entityEntityResource.setEntityTag(this.entityTagDummy);
	}
	
	@After
	public void tearDown(){
		
	}
	
	@Test
	public void testCloneWithOEntity() throws CloneNotSupportedException {		
		//given
		doReturn(this.oEntityEntityResourceMock).when(this.oEntityEntityResource).createNewEntityResource(anyString(), any(OEntity.class));
		//when
		EntityResource<OEntity> result = this.oEntityEntityResource.clone();
		assertThat(result, not(sameInstance(this.oEntityEntityResource)));
		//then
		verify(this.oEntityEntityResource).createNewEntityResource(eq("company"), same(this.oEntity));
		verify(this.oEntityEntityResourceMock).setEmbedded(this.embeddedDummy);
		verify(this.oEntityEntityResourceMock).setLinks(this.linksDummy);
		verify(this.oEntityEntityResourceMock).setEntityTag(this.entityTagDummy);
	}
	
	@Test
	public void testCloneWithEntity() throws CloneNotSupportedException {		
		//given
		doReturn(this.entityEntityResourceMock).when(this.entityEntityResource).createNewEntityResource(anyString(), any(Entity.class));
		//when
		EntityResource<Entity> result = this.entityEntityResource.clone();
		assertThat(result, not(sameInstance(this.entityEntityResource)));
		//then
		verify(this.entityEntityResource).createNewEntityResource(eq("user"), same(this.entity));
		verify(this.entityEntityResourceMock).setEmbedded(this.embeddedDummy);
		verify(this.entityEntityResourceMock).setLinks(this.linksDummy);
		verify(this.entityEntityResourceMock).setEntityTag(this.entityTagDummy);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testEntityObject() throws JAXBException {
		String testXMLString = "<resource><Test/></resource>";
		
		JAXBContext jc = JAXBContext.newInstance(EntityResource.class, NestedObject.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        EntityResource<NestedObject> er = (EntityResource<NestedObject>) unmarshaller.unmarshal(new ByteArrayInputStream(testXMLString.getBytes()));
 
        assertNotNull(er);
        assertNotNull(er.getEntity());
		assertTrue(er.getEntity() instanceof NestedObject);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testEntityObjectName() throws JAXBException {
		String testXMLString = "<resource><Test><name>noah</name></Test></resource>";
		
		JAXBContext jc = JAXBContext.newInstance(EntityResource.class, NestedObject.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        EntityResource<NestedObject> er = (EntityResource<NestedObject>) unmarshaller.unmarshal(new ByteArrayInputStream(testXMLString.getBytes()));
 
        assertNotNull(er);
        assertNotNull(er.getEntity());
		assertEquals("noah", ((NestedObject)er.getEntity()).getName());
	}

}

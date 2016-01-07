package com.temenos.interaction.jdbc.producer;

/* 
 * #%L
 * interaction-jdbc-producer
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import javax.naming.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jndi.JndiTemplate;

import com.temenos.interaction.authorization.command.util.ODataParser;
import com.temenos.interaction.core.MultivaluedMapImpl;
import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.entity.Entity;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.hypermedia.ResourceState;
import com.temenos.interaction.core.resource.CollectionResource;
import com.temenos.interaction.core.resource.EntityResource;
import com.temenos.interaction.jdbc.ServerMode;
import com.temenos.interaction.jdbc.exceptions.JdbcException;

/**
 * Test JdbcProducer class.
 */
public class TestJdbcProducer extends AbstractJdbcProducerTest {

    // Somewhere to store Jndi context.
    private JndiTemplate jndiTemplate = null;

    // Jndi name for the data source
    String DATA_SOURCE_JNDI_NAME = "H2Datasource";

    /*
     * Utility to set up Jndi context
     */
    private void jndiSetUp() {
        // Get working directory as a URL.
        URL workingDir = null;
        boolean threw = false;
        try {
            File file = new File(System.getProperty("user.dir"));
            workingDir = file.toURI().toURL();
        } catch (Exception e) {
            threw = true;
        }
        assertFalse("Could not get working directory.", threw);

        // Create a jndiTemplate. This will contain a Jndi context.
        threw = false;
        try {
            Properties env = new Properties();

            // Uses the local file system for Jndi storage,
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.fscontext.RefFSContextFactory");

            // Store Jndi information in a .bindings file in the working
            // directory.
            env.put(Context.PROVIDER_URL, workingDir.toString());

            jndiTemplate = new JndiTemplate(env);

        } catch (Exception e) {
            threw = true;
        }
        assertFalse("Could not open JndiTemplate", threw);
    }

    /*
     * Utility to shut down Jndi context
     */
    private void jndiShutDown() {
        try {
            // Remove object. If .bindings file becomes empty this should also
            // delete it. If not then we have left it as we found it.
            jndiTemplate.unbind(DATA_SOURCE_JNDI_NAME);

            // Don't think we have to close JndiTemplate
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test constructor
     */
    @Test
    public void testConstructor() {
        JdbcProducer producer = null;
        try {
            producer = new JdbcProducer(dataSource);
        } catch (Exception e) {
            fail();
        }

        // Should contain DataSource
        assertEquals(dataSource, producer.getDataSource());
    }

    /**
     * Test basic access to database.
     */
    @Test
    public void testQuery() {

        // Populate the database.
        populateTestTable();

        // Create data source for target

        // Create the producer
        JdbcProducer producer = null;
        try {
            producer = new JdbcProducer(dataSource);
        } catch (Exception e) {
            fail();
        }
        // Run a query
        SqlRowSet rs = producer.query(query);

        // Check the results
        assertFalse(null == rs);

        int rowCount = 0;
        while (rs.next()) {
            assertEquals(TEST_KEY_DATA + rowCount, rs.getString(KEY_FIELD_NAME));
            assertEquals(TEST_VARCHAR_DATA + rowCount, rs.getString(VARCHAR_FIELD_NAME));
            assertEquals(TEST_INTEGER_DATA + rowCount, rs.getInt(INTEGER_FIELD_NAME));
            rowCount++;
        }
        assertEquals(TEST_ROW_COUNT, rowCount);
    }

    /**
     * Check that Jndi itself is working
     */
    @Test
    public void testJndiWorking() {
        // Start up Jndi
        jndiSetUp();

        // Write reference to the data source into Jndi
        boolean threw = false;
        try {
            // Remove any existing objects with same name
            jndiTemplate.unbind(DATA_SOURCE_JNDI_NAME);

            jndiTemplate.bind(DATA_SOURCE_JNDI_NAME, dataSource);
        } catch (Exception e) {
            threw = true;
        }
        assertFalse("Could not bind object to JndiTemplate", threw);

        // Test Jndi by reading the object.
        threw = false;
        JdbcDataSource actualSource = null;
        try {
            actualSource = (JdbcDataSource) jndiTemplate.lookup(DATA_SOURCE_JNDI_NAME);
        } catch (Exception e) {
            threw = true;
        }
        assertFalse("Could not read object from JndiTemplate", threw);

        // Check returned data.
        assertEquals(dataSource.getUrl(), actualSource.getUrl());
        assertEquals(dataSource.getUser(), actualSource.getUser());
        assertEquals(dataSource.getPassword(), actualSource.getPassword());

        // Tidy
        jndiShutDown();
    }

    /**
     * Test access to database with Jndi lookup of datasource
     */
    @Test
    public void testJndiQuery() {
        // Populate the jdbc database.
        populateTestTable();

        // Start up Jndi
        jndiSetUp();

        // Write reference to the data source into Jndi
        try {
            // Remove any existing objects with same name
            jndiTemplate.unbind(DATA_SOURCE_JNDI_NAME);

            jndiTemplate.bind(DATA_SOURCE_JNDI_NAME, dataSource);
        } catch (Exception e) {
            fail("Could not bind object to JndiTemplate");
        }

        // Create the jdbc producer
        JdbcProducer producer = null;
        try {
            producer = new JdbcProducer(jndiTemplate, DATA_SOURCE_JNDI_NAME);
        } catch (Exception e) {
            fail();
        }

        // Run a query
        SqlRowSet rs = producer.query(query);

        // Check the results
        assertFalse(null == rs);
        int rowCount = 0;
        while (rs.next()) {
            assertEquals(TEST_KEY_DATA + rowCount, rs.getString(KEY_FIELD_NAME));
            assertEquals(TEST_VARCHAR_DATA + rowCount, rs.getString(VARCHAR_FIELD_NAME));
            assertEquals(TEST_INTEGER_DATA + rowCount, rs.getInt(INTEGER_FIELD_NAME));
            rowCount++;
        }
        assertEquals(TEST_ROW_COUNT, rowCount);

        // Tidy
        jndiShutDown();
    }

    /**
     * Test access to database using Iris parameter passing.
     */
    @Test
    public void testIrisQuery() {
        // Populate the database.
        populateTestTable();

        // Create the producer
        JdbcProducer producer = null;
        try {
            producer = new JdbcProducer(dataSource);
        } catch (Exception e) {
            fail();
        }

        // Build up an InteractionContext
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl<String>();
        MultivaluedMap<String, String> pathParams = new MultivaluedMapImpl<String>();
        InteractionContext ctx = new InteractionContext(mock(UriInfo.class), mock(HttpHeaders.class), pathParams,
                queryParams, mock(ResourceState.class), mock(Metadata.class));

        // Run a query
        SqlRowSet rs = null;
        try {
            rs = producer.query(TEST_TABLE_NAME, null, ctx);
        } catch (Exception e) {
            fail();
        }

        // Check the results
        assertFalse(null == rs);

        int rowCount = 0;
        while (rs.next()) {
            assertEquals(TEST_KEY_DATA + rowCount, rs.getString(KEY_FIELD_NAME));
            assertEquals(TEST_VARCHAR_DATA + rowCount, rs.getString(VARCHAR_FIELD_NAME));
            assertEquals(TEST_INTEGER_DATA + rowCount, rs.getInt(INTEGER_FIELD_NAME));
            rowCount++;
        }
        assertEquals(TEST_ROW_COUNT, rowCount);
    }

    /**
     * Test access to database using Iris parameter passing and returning a
     * single entity.
     */
    @Test
    public void testIrisQueryEntity() {
        // Populate the database.
        populateTestTable();

        // Create the producer
        JdbcProducer producer = null;
        try {
            producer = new JdbcProducer(dataSource);
        } catch (Exception e) {
            fail();
        }

        // Build up an InteractionContext
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl<String>();
        MultivaluedMap<String, String> pathParams = new MultivaluedMapImpl<String>();
        InteractionContext ctx = new InteractionContext(mock(UriInfo.class), mock(HttpHeaders.class), pathParams,
                queryParams, mock(ResourceState.class), mock(Metadata.class));

        // Run a query
        EntityResource<Entity> entityResource = null;
        String expectedType = "returnEntityType";
        String key = TEST_KEY_DATA + 1;
        try {
            entityResource = producer.queryEntity(TEST_TABLE_NAME, key, ctx, expectedType);
        } catch (Exception e) {
            fail();
        }

        // Check the results
        assertFalse(null == entityResource);

        Entity entity = (Entity) entityResource.getEntity();

        assertEquals(expectedType, entity.getName());
        assertEquals(TEST_KEY_DATA + 1, entity.getProperties().getProperty(KEY_FIELD_NAME).getValue());
        assertEquals(TEST_VARCHAR_DATA + 1, entity.getProperties().getProperty(VARCHAR_FIELD_NAME).getValue());
        assertEquals(TEST_INTEGER_DATA + 1, entity.getProperties().getProperty(INTEGER_FIELD_NAME).getValue());

    }

    /**
     * Test access to database using Iris parameter passing and returning a
     * single entity. When the entry is not present in the database.
     */
    @Test(expected = JdbcException.class)
    public void testIrisQueryEntityMissing() throws Exception {
        // Populate the database.
        populateTestTable();

        // Create the producer
        JdbcProducer producer = null;
        try {
            producer = new JdbcProducer(dataSource);
        } catch (Exception e) {
            fail();
        }

        // Build up an InteractionContext
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl<String>();
        MultivaluedMap<String, String> pathParams = new MultivaluedMapImpl<String>();
        InteractionContext ctx = new InteractionContext(mock(UriInfo.class), mock(HttpHeaders.class), pathParams,
                queryParams, mock(ResourceState.class), mock(Metadata.class));

        // Run a query. Should throw.
        String expectedType = "returnEntityType";
        String key = "badEntityKey";
        producer.queryEntity(TEST_TABLE_NAME, key, ctx, expectedType);
    }

    /**
     * Test access to database using Iris parameter passing and returning a
     * collection of entities.
     */
    @Test
    public void testIrisQueryEntities() {
        // Populate the database.
        populateTestTable();

        // Create the producer
        JdbcProducer producer = null;
        try {
            producer = new JdbcProducer(dataSource);
        } catch (Exception e) {
            fail();
        }

        // Build up an InteractionContext
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl<String>();
        MultivaluedMap<String, String> pathParams = new MultivaluedMapImpl<String>();
        InteractionContext ctx = new InteractionContext(mock(UriInfo.class), mock(HttpHeaders.class), pathParams,
                queryParams, mock(ResourceState.class), mock(Metadata.class));

        // Run a query
        CollectionResource<Entity> entities = null;
        String expectedType = "returnEntityType";
        try {
            entities = producer.queryEntities(TEST_TABLE_NAME, ctx, expectedType);
        } catch (Exception e) {
            fail();
        }

        // Check the results
        assertFalse(null == entities);

        int entityCount = 0;
        for (EntityResource<Entity> entityResource : entities.getEntities()) {
            Entity actualEntity = (Entity) entityResource.getEntity();

            assertEquals(expectedType, actualEntity.getName());
            assertEquals(TEST_KEY_DATA + entityCount, actualEntity.getProperties().getProperty(KEY_FIELD_NAME)
                    .getValue());
            assertEquals(TEST_VARCHAR_DATA + entityCount, actualEntity.getProperties().getProperty(VARCHAR_FIELD_NAME)
                    .getValue());
            assertEquals(TEST_INTEGER_DATA + entityCount, actualEntity.getProperties().getProperty(INTEGER_FIELD_NAME)
                    .getValue());
            entityCount++;
        }
        assertEquals(TEST_ROW_COUNT, entityCount);
    }

    /**
     * Test access to database using Iris parameters with a $select term.
     */
    @Test
    public void testIrisSelectQuery() {
        // Populate the database.
        populateTestTable();

        // Create the producer
        JdbcProducer producer = null;
        try {
            producer = new JdbcProducer(dataSource);
        } catch (Exception e) {
            fail();
        }

        // Build up an InteractionContext
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl<String>();
        queryParams.add(ODataParser.SELECT_KEY, INTEGER_FIELD_NAME);
        MultivaluedMap<String, String> pathParams = new MultivaluedMapImpl<String>();
        InteractionContext ctx = new InteractionContext(mock(UriInfo.class), mock(HttpHeaders.class), pathParams,
                queryParams, mock(ResourceState.class), mock(Metadata.class));

        // Run a query
        SqlRowSet rs = null;
        try {
            rs = producer.query(TEST_TABLE_NAME, null, ctx);
        } catch (Exception e) {
            fail();
        }

        // Check the results
        assertFalse(null == rs);

        int rowCount = 0;
        while (rs.next()) {
            boolean threw = false;
            try {
                rs.getString(KEY_FIELD_NAME);
            } catch (InvalidResultSetAccessException e) {
                threw = true;
            }
            // Not expecting this field so should throw.
            assertTrue(threw);

            threw = false;
            try {
                rs.getString(VARCHAR_FIELD_NAME);
            } catch (InvalidResultSetAccessException e) {
                threw = true;
            }
            // Not expecting this field so should throw.
            assertTrue(threw);

            // We are expecting this field.
            assertEquals(TEST_INTEGER_DATA + rowCount, rs.getInt(INTEGER_FIELD_NAME));
            rowCount++;
        }
        assertEquals(TEST_ROW_COUNT, rowCount);
    }

    /**
     * Test access to database using Iris parameters with a $filter term.
     */
    @Test
    public void testIrisFilterQuery() {
        // Populate the database.
        populateTestTable();

        // Create the producer
        JdbcProducer producer = null;
        try {
            producer = new JdbcProducer(dataSource);
        } catch (Exception e) {
            fail();
        }

        // Build up an InteractionContext
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl<String>();
        queryParams.add(ODataParser.FILTER_KEY, VARCHAR_FIELD_NAME + " eq " + TEST_VARCHAR_DATA + "2");
        MultivaluedMap<String, String> pathParams = new MultivaluedMapImpl<String>();
        InteractionContext ctx = new InteractionContext(mock(UriInfo.class), mock(HttpHeaders.class), pathParams,
                queryParams, mock(ResourceState.class), mock(Metadata.class));

        // Run a query
        SqlRowSet rs = null;
        try {
            rs = producer.query(TEST_TABLE_NAME, null, ctx);
        } catch (Exception e) {
            fail();
        }

        // Check the results. Should get all fields of the single row we
        // filtered for.
        assertFalse(null == rs);

        int rowCount = 0;
        while (rs.next()) {
            assertEquals(TEST_KEY_DATA + 2, rs.getString(KEY_FIELD_NAME));
            assertEquals(TEST_VARCHAR_DATA + 2, rs.getString(VARCHAR_FIELD_NAME));
            assertEquals(TEST_INTEGER_DATA + 2, rs.getInt(INTEGER_FIELD_NAME));
            rowCount++;
        }
        assertEquals(1, rowCount);
    }

    /**
     * Test access to database using Iris parameters with a numeric $filter
     * term.
     */
    @Test
    public void testIrisNumericFilterQuery() {
        // Populate the database.
        populateTestTable();

        // Create the producer
        JdbcProducer producer = null;
        try {
            producer = new JdbcProducer(dataSource);
        } catch (Exception e) {
            fail();
        }

        // Build up an InteractionContext
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl<String>();
        queryParams.add(ODataParser.FILTER_KEY, INTEGER_FIELD_NAME + " eq " + (TEST_INTEGER_DATA + 2));
        MultivaluedMap<String, String> pathParams = new MultivaluedMapImpl<String>();
        InteractionContext ctx = new InteractionContext(mock(UriInfo.class), mock(HttpHeaders.class), pathParams,
                queryParams, mock(ResourceState.class), mock(Metadata.class));

        // Run a query
        SqlRowSet rs = null;
        try {
            rs = producer.query(TEST_TABLE_NAME, null, ctx);
        } catch (Exception e) {
            fail();
        }

        // Check the results. Should get all fields of the single row we
        // filtered for.
        assertFalse(null == rs);

        int rowCount = 0;
        while (rs.next()) {
            assertEquals(TEST_KEY_DATA + 2, rs.getString(KEY_FIELD_NAME));
            assertEquals(TEST_VARCHAR_DATA + 2, rs.getString(VARCHAR_FIELD_NAME));
            assertEquals(TEST_INTEGER_DATA + 2, rs.getInt(INTEGER_FIELD_NAME));
            rowCount++;
        }
        assertEquals(1, rowCount);
    }

    /**
     * Test access to database using Iris parameters with a non numeric value
     * for a numeric $filter term.
     */
    @Test(expected = SecurityException.class)
    public void testIrisBadNumericFilterQuery() throws Exception {
        // Populate the database.
        populateTestTable();

        // Create the producer
        JdbcProducer producer = null;
        try {
            producer = new JdbcProducer(dataSource);
        } catch (Exception e) {
            fail();
        }

        // Build up an InteractionContext
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl<String>();
        queryParams.add(ODataParser.FILTER_KEY, INTEGER_FIELD_NAME + " eq " + "bad");
        MultivaluedMap<String, String> pathParams = new MultivaluedMapImpl<String>();
        InteractionContext ctx = new InteractionContext(mock(UriInfo.class), mock(HttpHeaders.class), pathParams,
                queryParams, mock(ResourceState.class), mock(Metadata.class));

        // Run a query. Should fail and throw.
        producer.query(TEST_TABLE_NAME, null, ctx);
    }

    /**
     * Test access to database using Iris with null tablename.
     */
    @Test(expected = JdbcException.class)
    public void testIrisQueryNullTable() throws Exception {

        // Create the producer
        JdbcProducer producer = null;
        try {
            producer = new JdbcProducer(mock(JdbcDataSource.class), ServerMode.MSSQL);
        } catch (Exception e) {
            fail();
        }

        // Build up an InteractionContext
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl<String>();
        MultivaluedMap<String, String> pathParams = new MultivaluedMapImpl<String>();
        InteractionContext ctx = new InteractionContext(mock(UriInfo.class), mock(HttpHeaders.class), pathParams,
                queryParams, mock(ResourceState.class), mock(Metadata.class));

        // Run a query. Should throw.
        producer.query(null, null, ctx);
    }
}

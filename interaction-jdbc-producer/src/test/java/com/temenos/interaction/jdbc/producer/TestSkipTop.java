package com.temenos.interaction.jdbc.producer;

/*
 * Test paging operations ($top and $skip). These differ between servers so test each under both Oracle and MSSQL 
 * compatibility mode.
 */

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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.junit.Test;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.temenos.interaction.authorization.command.util.ODataParser;
import com.temenos.interaction.core.MultivaluedMapImpl;
import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.entity.Metadata;
import com.temenos.interaction.core.hypermedia.ResourceState;
import com.temenos.interaction.jdbc.ServerMode;

/**
 * Test $skip and $top options withJdbcProducer class.
 */
public class TestSkipTop extends AbstractJdbcProducerTest {

    /**
     * Test access to database using Iris parameters with a $top term.
     */
    @Test
    public void testTopQueryMSSQL() {
        setMSSQLMode();
        testTopQuery(ServerMode.H2_MSSQL);
    }

    @Test
    public void testTopQueryOracle() {
        setOracleMode();
        testTopQuery(ServerMode.H2_ORACLE);
    }

    private void testTopQuery(ServerMode serverMode) {
        // Populate the database.
        populateTestTable();

        // Create the producer
        JdbcProducer producer = null;
        try {
            producer = new JdbcProducer(dataSource, serverMode);
        } catch (Exception e) {
            fail();
        }

        // Build up an InteractionContext
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl<String>();

        // Return all but the last row.
        queryParams.add(ODataParser.TOP_KEY, Integer.toString(TEST_ROW_COUNT - 1));

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
            assertEquals(TEST_KEY_DATA + rowCount, rs.getString(KEY_FIELD_NAME));
            assertEquals(TEST_VARCHAR_DATA + rowCount, rs.getString(VARCHAR_FIELD_NAME));
            assertEquals(TEST_INTEGER_DATA + rowCount, rs.getInt(INTEGER_FIELD_NAME));
            rowCount++;
        }
        assertEquals(TEST_ROW_COUNT - 1, rowCount);
    }

    /**
     * Test access to database using Iris parameters with a $skip term.
     */
    @Test
    public void testSkipQueryMSSQL() {
        setMSSQLMode();
        testSkipQuery(ServerMode.H2_MSSQL);
    }

    @Test
    public void testSkipQueryOracle() {
        setOracleMode();
        testSkipQuery(ServerMode.H2_ORACLE);
    }

    private void testSkipQuery(ServerMode serverMode) {
        // Populate the database.
        populateTestTable();

        // Create the producer
        JdbcProducer producer = null;
        try {
            producer = new JdbcProducer(dataSource, serverMode);
        } catch (Exception e) {
            fail();
        }

        // Build up an InteractionContext
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl<String>();

        // Skip first then return all.
        queryParams.add(ODataParser.SKIP_KEY, "1");

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
            assertEquals(TEST_KEY_DATA + (rowCount + 1), rs.getString(KEY_FIELD_NAME));
            assertEquals(TEST_VARCHAR_DATA + (rowCount + 1), rs.getString(VARCHAR_FIELD_NAME));
            assertEquals(TEST_INTEGER_DATA + (rowCount + 1), rs.getInt(INTEGER_FIELD_NAME));
            rowCount++;
        }
        assertEquals(TEST_ROW_COUNT - 1, rowCount);
    }

    /**
     * Test access to database using Iris parameters with a $top and $skip term.
     */
    @Test
    public void testTopSkipQueryMSSQL() {
        setMSSQLMode();
        testTopSkipQuery(ServerMode.H2_MSSQL);
    }

    @Test
    public void testTopSkipQueryOracle() {
        setOracleMode();
        testTopSkipQuery(ServerMode.H2_ORACLE);
    }

    private void testTopSkipQuery(ServerMode serverMode) {
        // Populate the database.
        populateTestTable();

        // Create the producer
        JdbcProducer producer = null;
        try {
            producer = new JdbcProducer(dataSource, serverMode);
        } catch (Exception e) {
            fail();
        }

        // Build up an InteractionContext
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl<String>();

        // Skip first then return one.
        queryParams.add(ODataParser.SKIP_KEY, "1");
        queryParams.add(ODataParser.TOP_KEY, "1");

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
            assertEquals(TEST_KEY_DATA + 1, rs.getString(KEY_FIELD_NAME));
            assertEquals(TEST_VARCHAR_DATA + 1, rs.getString(VARCHAR_FIELD_NAME));
            assertEquals(TEST_INTEGER_DATA + 1, rs.getInt(INTEGER_FIELD_NAME));
            rowCount++;
        }
        assertEquals(1, rowCount);
    }
}

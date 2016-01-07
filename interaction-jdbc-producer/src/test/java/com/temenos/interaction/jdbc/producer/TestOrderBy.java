package com.temenos.interaction.jdbc.producer;

/*
 * Test ordering operation ($orderby). These differ between servers so test each under both Oracle and MSSQL 
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
 * Test $orderby options withJdbcProducer class.
 */
public class TestOrderBy extends AbstractJdbcProducerTest {

    /**
     * Test access to database using Iris parameters with a $orderby term.
     */
    @Test
    public void testAscQueryMSSQL() {
        setMSSQLMode();
        testOrderedQuery(ServerMode.MSSQL, "asc");
    }

    @Test
    public void testAscQueryOracle() {
        setOracleMode();
        testOrderedQuery(ServerMode.ORACLE, "asc");
    }

    @Test
    public void testDescQueryMSSQL() {
        setMSSQLMode();
        testOrderedQuery(ServerMode.MSSQL, "desc");
    }

    @Test
    public void testDescQueryOracle() {
        setOracleMode();
        testOrderedQuery(ServerMode.ORACLE, "desc");
    }

    private void testOrderedQuery(ServerMode serverMode, String direction) {
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

        // Order by integer.
        queryParams.add(ODataParser.ORDERBY_KEY, INTEGER_FIELD_NAME + " " + direction);

        MultivaluedMap<String, String> pathParams = new MultivaluedMapImpl<String>();
        InteractionContext ctx = new InteractionContext(mock(UriInfo.class), mock(HttpHeaders.class), pathParams,
                queryParams, mock(ResourceState.class), mock(Metadata.class));

        // Run a query
        SqlRowSet rs = null;
        try {
            rs = producer.query(TEST_TABLE_NAME, null, ctx);
        } catch (Exception e) {
            fail("Query failed: " + e);
        }

        // Check the results. Should get all fields of the single row we
        // filtered for.
        assertFalse(null == rs);

        int rowCount = 0;
        while (rs.next()) {
            int data;
            if (direction.equals("asc")) {
                data = rowCount;
            } else {
                data = TEST_ROW_COUNT - 1 - rowCount;
            }
            assertEquals(TEST_KEY_DATA + data, rs.getString(KEY_FIELD_NAME));
            assertEquals(TEST_VARCHAR_DATA + data, rs.getString(VARCHAR_FIELD_NAME));
            assertEquals(TEST_INTEGER_DATA + data, rs.getInt(INTEGER_FIELD_NAME));
            rowCount++;
        }
        assertEquals(TEST_ROW_COUNT, rowCount);
    }
}

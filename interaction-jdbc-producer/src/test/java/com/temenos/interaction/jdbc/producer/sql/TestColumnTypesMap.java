package com.temenos.interaction.jdbc.producer.sql;

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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.temenos.interaction.jdbc.producer.AbstractJdbcProducerTest;
import com.temenos.interaction.jdbc.producer.JdbcProducer;
import com.temenos.interaction.jdbc.producer.sql.ColumnTypesMap;

/**
 * Test ColumnTypesMap class.
 */
public class TestColumnTypesMap extends AbstractJdbcProducerTest {

	/**
	 * Test constructor
	 */
	@Test
	public void testConstructor() {
		// Set up a queryable environment.
		populateTestTable();

		// Create the producer
		JdbcProducer producer = null;
		try {
			producer = new JdbcProducer(dataSource);
		} catch (Exception e) {
			fail();
		}

		// Create the object under test
		try {
			new ColumnTypesMap(producer, TEST_TABLE_NAME, true);
		} catch (Exception e) {
			fail();
		}
	}
	
	/**
	 * Test getting the primary key name
	 */
	@Test
	public void testGetPrimaryKeyName() {
		// Set up a queryable environment.
		populateTestTable();

		// Create the producer
		JdbcProducer producer = null;
		try {
			producer = new JdbcProducer(dataSource);
		} catch (Exception e) {
			fail();
		}

		// Create the object under test
		ColumnTypesMap map = null;
		try {
			map = new ColumnTypesMap(producer, TEST_TABLE_NAME, true);
		} catch (Exception e) {
			fail();
		}

		assertEquals(KEY_FIELD_NAME, map.getPrimaryKeyName());
	}
	
	/**
	 * Test getting the default "RECID" primary key.
	 */
	@Test
	public void testGetPrimaryKeyNameDefaultKey() {
		// Set up a queryable environment.
		populateTestTable(TEST_ROW_COUNT, false);

		// Create the producer
		JdbcProducer producer = null;
		try {
			producer = new JdbcProducer(dataSource);
		} catch (Exception e) {
			fail();
		}

		// Create the object under test
		ColumnTypesMap map = null;
		try {
			map = new ColumnTypesMap(producer, TEST_TABLE_NAME, true);
		} catch (Exception e) {
			fail();
		}
		
		// Since the RECID column is present should get the default key.
		assertEquals("RECID", map.getPrimaryKeyName());
	}
	
	@Test
	public void testGetType() {

		// Build up some column metadata matching the above columns. The correct
		// fields must be numeric of textual.
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("col1", java.sql.Types.VARCHAR);
		map.put("col2", java.sql.Types.INTEGER);
		ColumnTypesMap columnTypesMap = new ColumnTypesMap(map, null);

		for (String colName : map.keySet()) {
			assertEquals(map.get(colName), columnTypesMap.getType(colName));
		}
	}

	@Test
	public void testIsNueric() {

		// Build up some column metadata matching the above columns. The correct
		// fields must be numeric of textual.
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("col1", java.sql.Types.VARCHAR);
		map.put("col2", java.sql.Types.INTEGER);
		ColumnTypesMap columnTypesMap = new ColumnTypesMap(map, null);

		assertFalse(columnTypesMap.isNumeric("col1"));
		assertTrue(columnTypesMap.isNumeric("col2"));
	}

}

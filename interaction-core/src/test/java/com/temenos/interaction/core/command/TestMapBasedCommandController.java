package com.temenos.interaction.core.command;

/*
 * #%L
 * interaction-core
 * %%
 * Copyright (C) 2012 - 2015 Temenos Holdings N.V.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Test class for the MapBasedCommandController {@link MapBasedCommandController}
 *
 * @author trojanbug
 */

@RunWith(MockitoJUnitRunner.class)
public class TestMapBasedCommandController {

	private MapBasedCommandController controller;
	
	@Before
	public void setUp() throws Exception {
		this.controller = new MapBasedCommandController(new HashMap<String, InteractionCommand>() {
			private static final long serialVersionUID = 1L;
			{
				this.put("testCommand1", new TestCommand());
				this.put("testCommand2", new TestCommand());
			}
		});
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testFetchCommand() {
		assertNotNull(this.controller.fetchCommand("testCommand1"));
		assertNull(this.controller.fetchCommand("testCommand123214321"));
	}

	@Test
	public void testIsValidCommand() {
		assertTrue(this.controller.isValidCommand("testCommand1"));
		assertFalse(this.controller.isValidCommand("testCommand12325415"));
	}
	
	@Test
	public void testMapIsNotUsedAfterSetting() {
		Map<String,InteractionCommand> commandMap = new HashMap<String, InteractionCommand>() {
			private static final long serialVersionUID = 1L;
			{
				this.put("testCommand1", new TestCommand());
				this.put("testCommand2", new TestCommand());
			}
		};
		MapBasedCommandController contrller = new MapBasedCommandController(commandMap);
		commandMap.clear();
		assertNotNull(contrller.fetchCommand("testCommand1"));
	}
}

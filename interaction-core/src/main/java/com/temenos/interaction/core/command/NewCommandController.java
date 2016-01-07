package com.temenos.interaction.core.command;

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


import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Legacy implementation of command registry, refactored to implement {@link CommandController}
 * @author trojanbug
 * @deprecated as of introduction of {@link MapBasedCommandController}, for compatibility with configuration generator referring to NewCommandController explicitly.
 */
@Deprecated
public class NewCommandController implements CommandController {
	private final Logger logger = LoggerFactory.getLogger(NewCommandController.class);

	protected Map<String, InteractionCommand> commands = new HashMap<String, InteractionCommand>();

	/**
	 * Create an empty command controller.
	 */
	public NewCommandController() {
	}

	/**
	 * Create a command controller and add the supplied commands to the resource path
	 * @precondition commands not null
	 * @postcondition all supplied commands can be retrieved with {@link fetchCommand}
	 * @param resourcePath
	 * @param Map<String, InteractionCommand> commands
	 */
	public NewCommandController(Map<String, InteractionCommand> commands) {
		assert(commands != null);
		for(String name : commands.keySet()) {
			addCommand(name, commands.get(name));
		}
	}

	/**
	 * Add a command to transition a resources state.
	 * @precondition name not null
	 * @precondition {@link InteractionCommand} not null
	 */
	public void addCommand(String name, InteractionCommand c) {
		assert(name != null);
		assert(c != null);
		commands.put(name, c);
	}

	/*
	 * Returns the {@link InteractionCommand} bound to this name.
	 *
	 * @precondition String name is non null
	 * @postcondition InteractionCommand class previously registered by #addCommand
	 * @invariant commands is not null and number of commands
	 */
	public InteractionCommand fetchCommand(String name) {
		logger.debug("Looking up interaction command by name [" + name + "]");
		InteractionCommand command = commands.get(name);
		if (command == null) {
			logger.error("No command bound to [" + name + "]");
		}
		return command;
	}

	public boolean isValidCommand(String name) {
		return (commands.get(name) != null);
	}

    public void removeCommand(String name) {
        commands.remove(name);
    }
}

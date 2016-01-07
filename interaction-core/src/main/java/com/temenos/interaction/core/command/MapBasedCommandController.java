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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of CommandController interface, bootstrapped by proving a map of InteractionCommands 
 * (keyed with names of the commands to be assigned). Replaces NewCommandController.
 * The class instances are thread safe, and individual commands can be added to the map returned by getCommandMap().
 * 
 * @author trojanbug
 */
public class MapBasedCommandController implements CommandController {

    private static final Logger logger = LoggerFactory.getLogger(MapBasedCommandController.class);

    private Map<String, InteractionCommand> commandMap = new ConcurrentHashMap<String, InteractionCommand>();

    public MapBasedCommandController() {
        logger.trace("Empty MapBasedCommandController created: " + this);
    }

    public MapBasedCommandController(Map<String, InteractionCommand> commandMap) {
        logger.trace("MapBasedCommandController created: " + this);
        setCommandMap(commandMap);
    }

    @Override
    public InteractionCommand fetchCommand(String name) {
        logger.trace("Retrieving command for name {} from MapBasedCommandController {}", name, this);
        return commandMap.get(name);
    }

    @Override
    public boolean isValidCommand(String name) {
        return commandMap.containsKey(name);
    }

    /**
     * @return the commandMap
     */
    public Map<String, InteractionCommand> getCommandMap() {
        return commandMap;
    }

    /**
     * @param commandMap the commandMap to set
     */
    public final void setCommandMap(Map<String, InteractionCommand> commandMap) {
        logger.trace("New command map set on MapBasedCommandController {}",this);
        this.commandMap = new ConcurrentHashMap(commandMap);
    }

}

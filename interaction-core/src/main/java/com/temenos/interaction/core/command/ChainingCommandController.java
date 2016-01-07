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
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link CommandController} delegating the command resolution
 * to a chain of wrapped CommandController implementations. The first
 * CommandController to return non-null from fetchCommand "wins". If not found
 * on any, return null
 *
 * @author trojanbug
 */
public class ChainingCommandController implements CommandController {

    private static final Logger logger = LoggerFactory.getLogger(ChainingCommandController.class);

    private List<? extends CommandController> commandControllers = new ArrayList<CommandController>();

    @Override
    public InteractionCommand fetchCommand(String name) {
        logger.trace("Chaining CommandController requested InteractionCommand for name {}", name);

        for (CommandController commandController : commandControllers) {
            logger.trace("ChainingCommandController delegating to {} to retrieve command for name {}", commandController, name);
            InteractionCommand command = commandController.fetchCommand(name);
            if (command != null) {
                logger.trace("ChainingCommandController returning command for name {} found by delegating to {}", name, commandController);
                return command;
            }
        }

        logger.trace("ChainingCommandController could not find command for name {} by delegation to any configured CommandControllers", name);
        return null;
    }

    @Override
    public boolean isValidCommand(String name) {
        logger.trace("ChainingCommandController requested to check existence of command for name {}", name);
        for (CommandController commandController : commandControllers) {
            if (commandController.isValidCommand(name)) {
                logger.trace("ChainingCommandController found command for name {} found by delegating to {}", name, commandController);
                return true;
            }
        }
        logger.trace("ChainingCommandController could not find command for name {} by delegation to any configured CommandControllers", name);
 
        return false;
    }

    public List<? extends CommandController> getCommandControllers() {
        return commandControllers;
    }

    public void setCommandControllers(List<? extends CommandController> commandControllers) {
        this.commandControllers = new ArrayList<CommandController>(commandControllers);
    }

}

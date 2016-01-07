package com.temenos.interaction.command;

/*
 * #%L
 * interaction-example-odata-airline
 * %%
 * Copyright (C) 2012 - 2016 Temenos Holdings N.V.
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


import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.temenos.interaction.core.command.InteractionCommand;
import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.command.InteractionException;

public class HelloWorldCommand implements InteractionCommand {
    private static final Logger logger = LoggerFactory.getLogger(HelloWorldCommand.class);

    private InteractionCommand wrappedCommand = new InteractionCommand() {

        public Result execute(InteractionContext ctx) throws InteractionException {

     logger.warn("" + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%===\n"
                    + "=                                                                 =\n"
                    + "=    HELLO  WORLD COMMAND UNCONFIGURED FOR WRAPPING, BUT USED      =\n"
                    + "=                                                                 =\n"
                    + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%===" + "");
            return Result.SUCCESS;
        }
    };

    public Result execute(InteractionContext ctx) throws InteractionException {

        logger.warn("\n"
                + StringUtils.repeat("=", 102)+"\n"
                + StringUtils.repeat(" ", 102)+"\n"
                + "|"+StringUtils.center("DEMO COMMAND WORKING AGAIN", 100)+"|\n"
                + StringUtils.repeat(" ", 102)+"\n"
                + StringUtils.repeat("=", 102)+"\n"
                );


        return wrappedCommand.execute(ctx);
    }

    public InteractionCommand getWrappedCommand() {
        return wrappedCommand;
    }

    public void setWrappedCommand(InteractionCommand wrappedCommand) {
        this.wrappedCommand = wrappedCommand;
    }

}

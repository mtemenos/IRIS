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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

/**
 * Implementation of {@link CommandController} delegating the command resolution
 * to underlying Spring ApplicationContext. The bean resolution is based on id
 * or name attributes of the beans in the context matching name passed
 * literally.
 *
 * @author trojanbug
 */
public class SpringContextBasedInteractionCommandController
        implements CommandController {

    private static final Logger logger = LoggerFactory.getLogger(SpringContextBasedInteractionCommandController.class);

    private ApplicationContext applicationContext = null;

    /**
     * @param name
     * @return The object returned by calling getBean(name,
     * InteractionCommand.class) method on the underlying application context,
     * or null if no such bean found, or application context is not set. If the
     * bean name matches, but it is not an implementation of
     * {@link InteractionCommand} will be silently ignored.
     */
    @Override
    public InteractionCommand fetchCommand(String name) {
        if (applicationContext == null) {
            logger.warn("ApplicationContext not initialised in fetchCommand of {}", this.getClass().getName());
            return null;
        }
        try {
            logger.trace("{} requesting bean implementing InteractionCommand under name {} from underlying ApplicationContext", this.getClass().getName(), name);
            return applicationContext.getBean(name, InteractionCommand.class);
        } catch (BeansException ex) {
            logger.trace("Could not find bean implementing interaction command under name {}", name);
            return null;
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return this.applicationContext;
    }

    @Override
    public boolean isValidCommand(String name) {
        if (applicationContext == null) {
            logger.warn("applicationContext not initialised in isValidCommand of " + this.getClass());
            return false;
        }
        try {
            logger.trace("{} requesting bean implementing InteractionCommand under name {} from underlying ApplicationContext", this.getClass().getName(), name);
            return (applicationContext.getBean(name, InteractionCommand.class) != null);
        } catch (BeansException ex) {
            logger.trace("Could not find bean implementing interaction command under name {}", name);
            return false;
        }
    }

}

package com.temenos.annotatedtestclasses;

/*
 * #%L
 * AnnotatedTestClasses
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


import com.temenos.interaction.core.command.InteractionCommand;
import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.command.InteractionException;
import com.temenos.interaction.core.command.annotation.InteractionCommandImpl;

/**
 * Class to support testing of AnnotationBasedCommandCOntroller
 * This class is annotated with InteractionCommandImpl so this shouldn't be found using reflection
 * because it is not implementing InteractionCommand
 * 
 * @author hmanchala
 */

@InteractionCommandImpl(name = "testName3")
public class AnnotatedInteractionCmdStubImpl3{

    public InteractionCommand.Result execute(InteractionContext ctx) throws InteractionException {
        return InteractionCommand.Result.SUCCESS;
    }
    
}

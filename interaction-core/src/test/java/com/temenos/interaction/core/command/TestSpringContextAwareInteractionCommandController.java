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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/spring-test-contexts/interaction-command-test-context.xml"})
public class TestSpringContextAwareInteractionCommandController {

    @Autowired
    SpringContextAwareInteractionCommandController commandController;

    @Test
    public void testFetchCommandWorksForConfiguredCommands() throws BeansException {
        Assert.assertNotNull(commandController.fetchCommand("testCommand1"));
        Assert.assertNotNull(commandController.fetchCommand("testCommand2"));
        Assert.assertTrue(commandController.isValidCommand("testCommand1"));
        Assert.assertTrue(commandController.isValidCommand("testCommand2"));
        Assert.assertEquals(TestCommand.class, commandController.fetchCommand("testCommand1").getClass());
    }

    @Test
    public void testFetchCommandDoesNotReturnAnythingElse() throws BeansException {
        // we don't expect any exceptions, just null returned
        Assert.assertNull(commandController.fetchCommand("testCommandNameNotConfiguredAnywhere"));
        Assert.assertFalse(commandController.isValidCommand("testCommandNameNotConfiguredAnywhere"));
    }
    
        @Test
    public void testScopesAreWorking() throws BeansException {
            InteractionCommand ic1_1 = commandController.fetchCommand("testCommand1");
            InteractionCommand ic1_2 = commandController.fetchCommand("testCommand1");

            Assert.assertTrue(ic1_1 == ic1_2);

            InteractionCommand ic3_1 = commandController.fetchCommand("testCommand3");
            InteractionCommand ic3_2 = commandController.fetchCommand("testCommand3");

            Assert.assertTrue(ic3_1 != ic3_2);
    }
}

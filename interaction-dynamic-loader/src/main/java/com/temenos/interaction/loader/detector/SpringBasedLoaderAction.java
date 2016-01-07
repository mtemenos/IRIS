package com.temenos.interaction.loader.detector;

/*
 * #%L
 * interaction-dynamic-loader
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
import java.io.Closeable;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.reflections.Reflections;
import org.reflections.scanners.AbstractScanner;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.vfs.Vfs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.temenos.interaction.core.command.ChainingCommandController;
import com.temenos.interaction.core.command.CommandController;
import com.temenos.interaction.core.command.InteractionCommand;
import com.temenos.interaction.core.command.SpringContextBasedInteractionCommandController;
import com.temenos.interaction.core.loader.Action;
import com.temenos.interaction.core.loader.FileEvent;
import com.temenos.interaction.loader.classloader.CachingParentLastURLClassloaderFactory;
import com.temenos.interaction.loader.objectcreation.ParameterizedFactory;

/**
 * Loads a CommandController with a set of InteractionCommands from Spring
 * configuration files.
 *
 * The execute method would be typically called after a directory change (for
 * instance, when a user copies a jar file with new InteractionCommands). All
 * jars in the directory are scanned for Spring configuration files matching the
 * pattern "/spring/*-interaction-context.xml". By default, the
 * CommandController with id "commandController", together with the defined
 * InteractionCommand, would be loaded to the top of a provided
 * ChainingCommandController.
 *
 * @author andres
 * @author trojan
 * @author cmclopes
 */
public class SpringBasedLoaderAction implements Action<FileEvent<File>>, ApplicationContextAware, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(SpringBasedLoaderAction.class);

    public final static String DEFAULT_COMMAND_CONTROLLER_BEAN_NAME = "commandController";

    List<String> configPatterns = new ArrayList();
    private ApplicationContext currentContext = null;
    private ApplicationContext parentContext = null;
    private boolean useCurrentContextAsParent = false;
    private List<String> configLocationsPatterns = new ArrayList(Arrays.asList(new String[]{"classpath:/spring*/*-interaction-context.xml"}));
    private Collection<? extends Action<ApplicationContext>> listeners = new ArrayList();
    ParameterizedFactory<FileEvent<File>, ClassLoader> classloaderFactory = new CachingParentLastURLClassloaderFactory();
    private String commandControllerBeanName = DEFAULT_COMMAND_CONTROLLER_BEAN_NAME;
    private ChainingCommandController parentChainingCommandController = null;
    private CommandController previouslyAddedCommandController = null;
    private ApplicationContext previousAppCtx = null;

    @Override
    public void execute(FileEvent<File> dirEvent) {
        logger.debug("Creation of new Spring ApplicationContext based CommandController triggerred by change in", dirEvent.getResource().getAbsolutePath());

        Collection<File> jars = FileUtils.listFiles(dirEvent.getResource(), new String[]{"jar"}, true);
        Set<URL> urls = new HashSet();
        for (File f : jars) {
            try {
                logger.trace("Adding {} to list of URLs to create ApplicationContext from", f.toURI().toURL());
                urls.add(f.toURI().toURL());
            } catch (MalformedURLException ex) {
                // kindly ignore and log
            }
        }
        Reflections reflectionHelper = new Reflections(
                new ConfigurationBuilder()
                .addClassLoader(classloaderFactory.getForObject(dirEvent)).addScanners(new ResourcesScanner())
                .addUrls(urls)
        );

        Set<String> resources = new HashSet();

        for (String locationPattern : configLocationsPatterns) {
            String regex = convertWildcardToRegex(locationPattern);
            resources.addAll(reflectionHelper.getResources(Pattern.compile(regex)));
        }

        if (!resources.isEmpty()) {
            // if resources are empty just clean up the previous ApplicationContext and leave!
            logger.debug("Detected potential Spring config files to load");
            ClassPathXmlApplicationContext context;
            if (parentContext != null) {
                context = new ClassPathXmlApplicationContext(parentContext);
            } else {
                context = new ClassPathXmlApplicationContext();
            }

            context.setConfigLocations(configLocationsPatterns.toArray(new String[]{}));

            ClassLoader childClassLoader = classloaderFactory.getForObject(dirEvent);
            context.setClassLoader(childClassLoader);
            context.refresh();

            CommandController cc = null;

            try {
                cc = context.getBean(commandControllerBeanName, CommandController.class);
                logger.debug("Detected pre-configured CommandController in added config files");
            } catch (BeansException ex) {
                Map<String, InteractionCommand> commands = context.getBeansOfType(InteractionCommand.class);
                if (!commands.isEmpty()) {
                    logger.debug("Adding new commands");
                    SpringContextBasedInteractionCommandController scbcc = new SpringContextBasedInteractionCommandController();
                    scbcc.setApplicationContext(context);
                    cc = scbcc;
                } else {
                    logger.debug("No commands detected to be added");
                }
            }

            if (parentChainingCommandController != null) {
                List<CommandController> newCommandControllers = new ArrayList<CommandController>(parentChainingCommandController.getCommandControllers());

                // "unload" the previously loaded CommandController
                if (previouslyAddedCommandController != null) {
                    logger.debug("Removing previously added instance of CommandController");
                    newCommandControllers.remove(previouslyAddedCommandController);
                }

                // if there is a new CommandController on the Spring file, add it on top of the chain
                if (cc != null) {
                    logger.debug("Adding newly created CommandController to ChainingCommandController");
                    newCommandControllers.add(0, cc);
                    parentChainingCommandController.setCommandControllers(newCommandControllers);
                    previouslyAddedCommandController = cc;
                } else {
                    previouslyAddedCommandController = null;
                }
            } else {
                logger.debug("No ChainingCommandController set to add newly created CommandController to - skipping action");
            }

            if (previousAppCtx != null) {
                if (previousAppCtx instanceof Closeable) {
                    try {
                        ((Closeable) previousAppCtx).close();
                    } catch (Exception ex) {
                        // log
                    }
                }
                previousAppCtx = context;
            }
        } else {
            logger.debug("No Spring config files detected in the JARs scanned");
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        currentContext = ac;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (parentContext == null && currentContext != null && useCurrentContextAsParent) {
            parentContext = currentContext;
        }
    }

    /**
     * @return the listeners
     */
    public Collection<? extends Action<ApplicationContext>> getListeners() {
        return listeners;
    }

    /**
     * @param listeners the listeners to set
     */
    public void setListeners(Collection<? extends Action<ApplicationContext>> listeners) {
        this.listeners = new ArrayList(listeners);
    }

    /**
     * @return the classloaderFactory
     */
    public ParameterizedFactory<FileEvent<File>, ClassLoader> getClassloaderFactory() {
        return classloaderFactory;
    }

    /**
     * @param classloaderFactory the classloaderFactory to set
     */
    public void setClassloaderFactory(ParameterizedFactory<FileEvent<File>, ClassLoader> classloaderFactory) {
        this.classloaderFactory = classloaderFactory;
    }

    /**
     * @return the parentContext
     */
    public ApplicationContext getParentContext() {
        return parentContext;
    }

    /**
     * @param parentContext the parentContext to set
     */
    public void setParentContext(ApplicationContext parentContext) {
        this.parentContext = parentContext;
    }

    /**
     * @return the useCurrentContextAsParent
     */
    public boolean isUseCurrentContextAsParent() {
        return useCurrentContextAsParent;
    }

    /**
     * @param useCurrentContextAsParent the useCurrentContextAsParent to set
     */
    public void setUseCurrentContextAsParent(boolean useCurrentContextAsParent) {
        this.useCurrentContextAsParent = useCurrentContextAsParent;
    }

    /**
     * @return the configLocationsPatterns
     */
    public List<String> getConfigLocationsPatterns() {
        return configLocationsPatterns;
    }

    /**
     * @param configLocationsPatterns the configLocationsPatterns to set
     */
    public void setConfigLocationsPatterns(List<String> configLocationsPatterns) {
        this.configLocationsPatterns = configLocationsPatterns;
    }

    /**
     * @return the commandControllerBeanName
     */
    public String getCommandControllerBeanName() {
        return commandControllerBeanName;
    }

    /**
     * @param commandControllerBeanName the commandControllerBeanName to set
     */
    public void setCommandControllerBeanName(String commandControllerBeanName) {
        this.commandControllerBeanName = commandControllerBeanName;
    }

    /**
     * @return the parentChainingCommandController
     */
    public ChainingCommandController getParentChainingCommandController() {
        return parentChainingCommandController;
    }

    /**
     * @param parentChainingCommandController the
     * parentChainingCommandController to set
     */
    public void setParentChainingCommandController(ChainingCommandController parentChainingCommandController) {
        this.parentChainingCommandController = parentChainingCommandController;
    }

    public static class CurrentThreadClassLoaderFactory implements ParameterizedFactory<FileEvent<File>, ClassLoader> {

        public CurrentThreadClassLoaderFactory() {
        }

        @Override
        public ClassLoader getForObject(FileEvent<File> param) {

            return Thread.currentThread().getContextClassLoader();

        }
    }

    private String convertWildcardToRegex(String wildcardPattern) {
        wildcardPattern = wildcardPattern.substring(wildcardPattern.indexOf(':') + 1);
        wildcardPattern = wildcardPattern.substring(wildcardPattern.lastIndexOf("/") + 1);
        return wildcardPattern.replaceAll("\\*", "[^/]*");
    }

    public static class ResourcesScanner extends AbstractScanner {

        public boolean acceptsInput(String file) {
            return !file.endsWith(".class"); //not a class
        }

        @Override
        public Object scan(Vfs.File file, Object classObject) {
            getStore().put(file.getName(), file.getRelativePath());
            return classObject;
        }

        public void scan(Object cls) {
            throw new UnsupportedOperationException(); //shouldn't get here
        }
    }

}

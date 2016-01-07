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

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import com.temenos.interaction.core.command.annotation.InteractionCommandImpl;

/**
 * Implementation of {@link CommandController} based on Annotation. Using
 * Reflection will scan the classes in JAR's & Packages for required annotation.
 * Resolution is based on the name attribute of the annotation.
 *
 * @author hmanchala
 * @author trojanbug
 */
public class AnnotationBasedCommandController implements CommandController {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationBasedCommandController.class);

    protected Map<String, InteractionCommand> cache = new HashMap();
    private ClassLoader classloader = null;
    private Collection<String> packagesToScan = null;
    private Collection<URL> jarsToScan = null;
    private Reflections reflectionsHelper = null;
    private boolean initialized = false;

    public AnnotationBasedCommandController() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public AnnotationBasedCommandController(ClassLoader classloader) {
        if (logger.isTraceEnabled()) {
            logger.trace("AnnotationBasedCommandController created with classloader {}", classloader != null ? classloader.getClass().getCanonicalName() : "NULL");
        }
        setClassloader(classloader);
    }

    /**
     * @param name
     * @return If the command is already in the cache it is returned else using
     * reflection scan the JARs/Packages and get the required annotated classes
     * which have implemented the interface, and return the object by calling
     * BeanUtils.instantiate() method
     */
    @Override
    public InteractionCommand fetchCommand(String name) {
        logger.trace("AnnotationBasedCommandController requested finding command for name: {}", name);
        if (!initialized) {
            logger.debug("AnnotationBasedCommandController store not yet initialized (or reintiialized) - requesting initialisation");
            reinitialize();
        }
        return cache.get(name);
    }

    protected synchronized void reinitialize() {
        logger.debug("AnnotationBasedCommandController initializing.");
        ConfigurationBuilder config = new ConfigurationBuilder();
        if (getJarsToScan() != null && (!jarsToScan.isEmpty())) {
            config.setUrls(getJarsToScan());
        }
        if (getClassloader() != null) {
            config.addClassLoader(getClassloader());
        }
        if (getPackagesToScan() != null && (!packagesToScan.isEmpty())) {
            config.forPackages(getPackagesToScan().toArray(new String[]{}));
        }
        reflectionsHelper = new Reflections(config);

        Set<Class<?>> annotatedClasses = reflectionsHelper.getTypesAnnotatedWith(InteractionCommandImpl.class);

        for (Class<?> annotatedClass : annotatedClasses) {
            String nameFound = annotatedClass.getAnnotation(InteractionCommandImpl.class).name();
            Object newCommandAsObject = BeanUtils.instantiate(annotatedClass);
            if (newCommandAsObject instanceof InteractionCommand) {
                InteractionCommand newCommand = (InteractionCommand) newCommandAsObject;
                logger.debug("AnnotationBasedCommandController adding {} class to cache uinder the name {}.", newCommand.getClass().getCanonicalName(), nameFound);
                cache.put(nameFound, newCommand);
            } else {
                logger.warn("A class annotated with @InteractionCommandImpl is not an InteractionCommand - ignoring!");
            }
        }
        initialized = true;
    }

    @Override
    public boolean isValidCommand(String name) {
        return (fetchCommand(name) != null);
    }

    public ClassLoader getClassloader() {
        return classloader;
    }

    public final void setClassloader(ClassLoader classloader) {
        logger.trace("AnnotationBasedCommandController {} setting classloader: {}", this, classloader);
        this.classloader = classloader;
        initialized = false;
    }

    public Collection<String> getPackagesToScan() {
        return packagesToScan;
    }

    public void setPackagesToScan(Collection<String> packagesToScan) {
        if (logger.isTraceEnabled()) {
            logger.trace("AnnotationBasedCommandController {} setting packages to scan: {}",
                    this,
                    Arrays.toString(packagesToScan.toArray(new String[]{})));
        }
        this.packagesToScan = packagesToScan;
        initialized = false;
    }

    public Collection<URL> getJarsToScan() {
        return jarsToScan;
    }

    public void setJarsToScan(Collection<URL> jarsToScan) {
        if (logger.isTraceEnabled()) {
            logger.trace("AnnotationBasedCommandController {} setting JARs' URLs to scan: {}",
                    this,
                    Arrays.toString(jarsToScan.toArray()));
        }
        this.jarsToScan = jarsToScan;
        initialized = false;
    }
}

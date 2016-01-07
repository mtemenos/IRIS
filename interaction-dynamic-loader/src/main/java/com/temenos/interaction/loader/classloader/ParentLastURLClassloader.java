package com.temenos.interaction.loader.classloader;

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


import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the loading of jar files, which would contain new InteractionCommands.
 * The jar files are supplied as an array of URLs.
 * It extends the URLClassLoader and basically forces the supplied jar files to the front of the search
 * down the classpath.
 * It performs the following search:
 *   1) Tries to find the required class loaded in the local classpath.
 *   2) Tries to find the required class in the supplied jar files.
 *   3) Delegates the search to the parent class loader, which will typically be the context class loader from the current thread.
 *
 * If the class is not found a ClassNotFoundException is thrown.
 *
 * @author trojanbug
 */

public class ParentLastURLClassloader extends URLClassLoader {
	
	private static final Logger logger = LoggerFactory.getLogger(ParentLastURLClassloader.class);

    public ParentLastURLClassloader(URL[] urls, ClassLoader cl) {
        super(urls, cl);
    }

    public ParentLastURLClassloader(URL[] urls) {
        super(urls);
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // check if loaded locally
        Class<?> result = findLoadedClass(name);
        if (result == null) {
            try {
                // search locally 
                result = findClass(name);
            } catch (ClassNotFoundException ex) {
                // Next, delegate to the parent, if not found locally.
                try {
                result = getParent().loadClass(name);
                } catch (Throwable t) {
					logger.warn("Classloader failed to find class " + name, t);
                }
            }
        }

        if (resolve) {
            resolveClass(result);
        }
        return result;
    }
}

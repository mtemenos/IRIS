package com.temenos.interaction.loader.classloader;

/*
 * #%L
 * * interaction-dynamic-loader
 * *
 * %%
 * Copyright (C) 2012 - 2015 Temenos Holdings N.V.
 * *
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
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Longs;
import com.temenos.interaction.core.loader.FileEvent;
import com.temenos.interaction.loader.objectcreation.ParameterizedFactory;
import java.io.IOException;
import java.net.URLClassLoader;

/**
 * This class manages the creation of URLClassLoaders, which are responsible for making new
 * InteractionCommands available.
 * It maintains a hashed state of the last jar file dynamically loaded based on the modified time.
 * If an event arrives with information about new jars being available, the hash is calculated for the
 * new jars and compared to the stored hash. If the hash values are equal, the cached URLClassLoader is
 * simply returned.
 * If the hash values are not equal a temporary directory is created and the jar files are copied to this directory.
 * This circumvents the problem of the URLClassLoader holding the jar file open, making it impossible to replace it
 * with a new version. A ParentLastURLClassloader is created to load the jars and the hash value is stored for future
 * comparisons.
 *
 * @author trojanbug
 */
public class CachingParentLastURLClassloaderFactory implements ParameterizedFactory<FileEvent<File>, ClassLoader> {

    private static final Logger logger = LoggerFactory.getLogger(CachingParentLastURLClassloaderFactory.class);

    private URLClassLoader cache = null;
    private Object lastState = null;
    private File lastClassloaderTempDir = null;

    @Override
    public synchronized ClassLoader getForObject(FileEvent<File> param) {

        Object state = calculateCurrentState(param);
        if (lastState == null || (!lastState.equals(state))) {
            logger.debug("Detected state change, creating new classloader");
            Object previousState = lastState;
            URLClassLoader previousCL = cache;
            File previousTempDir = lastClassloaderTempDir;
            
            cleanupClassloaderResources(previousCL, previousTempDir);
            //TODO add listeners to inform about classloader creation and "destruction"
            lastState = state;
            cache = createClassLoader(state, param);
        }

        return cache;
    }

    protected synchronized URLClassLoader createClassLoader(Object currentState, FileEvent<File> param) {
        try {
                logger.debug("Classloader requested from CachingParentLastURLClassloaderFactory, based on FileEvent reflecting change in {}", param.getResource().getAbsolutePath());
            Set<URL> urls = new HashSet<URL>();
            File newTempDir = new File(FileUtils.getTempDirectory(),currentState.toString());
            FileUtils.forceMkdir(newTempDir);
            Collection<File> files = FileUtils.listFiles(param.getResource(), new String[]{"jar"}, true);
            for (File f : files) {
                try {
                    logger.trace("Adding {} to list of URLs to create classloader from", f.toURI().toURL());
                    FileUtils.copyFileToDirectory(f, newTempDir);
                    urls.add(new File(newTempDir, f.getName()).toURI().toURL());
                } catch (MalformedURLException ex) {
                    // should not happen, we do have the file there
                    // but if, what can we do - just log it
                    logger.warn("Trying to intilialize classloader based on URL failed!", ex);
                }
            }
            lastClassloaderTempDir = newTempDir;
            URLClassLoader classloader = new ParentLastURLClassloader(urls.toArray(new URL[]{}), Thread.currentThread().getContextClassLoader());
            
            return classloader;
        } catch (IOException ex) {
            throw new RuntimeException("Unexpected error trying to create new classloader.", ex);
        }
    }

    protected Object calculateCurrentState(FileEvent<File> param) {
        Collection<File> files = FileUtils.listFiles(param.getResource(), new String[]{"jar"}, true);

        MessageDigest md = DigestUtils.getMd5Digest();
        for (File f : files) {
            md.update(Longs.toByteArray(f.lastModified()));
        }
        
        Object state = Hex.encodeHexString(md.digest());
            logger.trace("Calculated representation /hash/ of state of collection of URLs for classloader creation to: {}", state);
        return state;
    }

    private void cleanupClassloaderResources(URLClassLoader previousCL, File previousTempDir) {
    	try {
    		if(previousCL != null){
    			previousCL.close();
    		}
        } catch (IOException ex) {
            logger.error("Failed to close classloader - potential resource and memory leak!");
        }
        try {
            if(previousTempDir != null){
            	FileUtils.forceDelete(previousTempDir);
            }
        } catch (IOException ex) {
            logger.error("Failed to delete temporary directory, possible resource leak!");
        }
    }
}

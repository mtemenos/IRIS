package com.temenos.interaction.loader.properties;

/*
 * #%L
 * interaction-springdsl
 * %%
 * Copyright (C) 2012 - 2014 Temenos Holdings N.V.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.PropertiesPersister;

import com.temenos.interaction.loader.xml.XmlChangedEventImpl;
import com.temenos.interaction.loader.xml.resource.notification.XmlModificationNotifier;
import com.temenos.interaction.springdsl.DynamicProperties;

/**
 * A properties factory bean that creates a reconfigurable Properties object.
 * When the Properties' reloadConfiguration method is called, and the file has
 * changed, the properties are read again from the file. Credit to:
 * http://www.wuenschenswert.net/wunschdenken/archives/127
 */
public class ReloadablePropertiesFactoryBean extends PropertiesFactoryBean implements DynamicProperties,
		DisposableBean, ApplicationContextAware {
	private ApplicationContext ctx;

	private List<ReloadablePropertiesListener> preListeners;
	private PropertiesPersister propertiesPersister = new DefaultPropertiesPersister();
	private ReloadablePropertiesBase reloadableProperties;
	private Properties properties;
	private long lastFileTimeStamp = 0;
	private List<Resource> resourcesPath = null;
	private File lastChangeFile = null;
	private XmlModificationNotifier xmlNotifier = null;

	public void setListeners(List<ReloadablePropertiesListener> listeners) {
		// early type check, and avoid aliasing
		this.preListeners = new ArrayList<ReloadablePropertiesListener>();
		for (ReloadablePropertiesListener l : listeners) {
			preListeners.add(l);
		}
	}

	/**
	 * @param properties
	 *            the properties to set
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	protected Object createInstance() throws IOException {
		// would like to uninherit from AbstractFactoryBean (but it's final!)
		if (!isSingleton())
			throw new RuntimeException("ReloadablePropertiesFactoryBean only works as singleton");
		reloadableProperties = new ReloadablePropertiesImpl();
		reloadableProperties.setProperties(properties);

		if (preListeners != null)
			reloadableProperties.setListeners(preListeners);
		reload(true);
		return reloadableProperties;
	}

	public void setXmlNotifier(XmlModificationNotifier xmlNotifier) {
		this.xmlNotifier = xmlNotifier;
	}

	public void destroy() throws Exception {
		reloadableProperties = null;
	}

	protected void getMoreRecentThan(File root, final long timestamp, final List<Resource> resources,
			final List<simplePattern> patterns) {
		File file = root;// new File(root.getURL().getFile());

		file.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if (pathname.isDirectory()) {
					getMoreRecentThan(pathname, timestamp, resources, patterns);
				} else {
					if (pathname.lastModified() > timestamp) {
						for (simplePattern pattern : patterns) {
							if (pattern.matches(pathname.getName())) {
								resources.add(new FileSystemResource(pathname));
							}
						}

					}
				}
				return false;
			}
		});

	}

	protected void reload(boolean forceReload) throws IOException {
		long l = System.currentTimeMillis();
		
		reload_new(forceReload);
		
		l = System.currentTimeMillis() - l;
		if (l > 2000) {
			logger.warn("Reload time " + l + " ms.");
		}
	}

	private List<Resource> initializeResourcesPath() throws IOException {
		List<Resource> ret = new ArrayList<Resource>();
		List<Resource> tmp = Arrays.asList(ctx.getResources("classpath*:"));
		for (Resource oneResource : tmp) {
			String sPath = oneResource.getURI().getPath().replace('\\', '/');
			int pos = sPath.indexOf("models-gen/src/generated/iris");
			if (pos > 0) {
				ret.add(oneResource);
				/*
				 * now let's look at the lastChange file
				 */
				String sRoot = sPath.substring(0, pos + "models-gen".length());
				File f = new File(sRoot, "lastChange");
				if (f.exists()) {
					lastChangeFile = f;
				}
			}
		}
		
		String irisCacheIndexFileStr = System.getProperty("iris.cache.index.file");				
		
		if(irisCacheIndexFileStr != null) {		    
    		File irisCacheIndexFile = new File(irisCacheIndexFileStr).getAbsoluteFile();
    		
    		if(irisCacheIndexFile.exists()) {
    		    lastChangeFile = irisCacheIndexFile;
    		    logger.info("The following index file will be used for refreshing resources: " + irisCacheIndexFile.getAbsolutePath());
    		}
    		
		}

		return ret;

	}

	private List<Resource> getLastChangeAndClear(File f) {
		File LastChangeFileLock = new File(f.getParent(), ".lastChangeLock");
		List<Resource> ret = new ArrayList<Resource>();
		FileLock lock = null;
		BufferedReader bufR = null;
		FileChannel fc = null;
		FileChannel fcLock = null;
		try {
			/*
			 * Maintain a specific lock to avoid partial file locking.
			 */
			fcLock = new RandomAccessFile(LastChangeFileLock, "rw").getChannel();
			lock = fcLock.lock();
			
			fc = new RandomAccessFile(f, "rws").getChannel();

			bufR = new BufferedReader(new FileReader(f));
			String sLine = null;
			boolean bFirst = true;
			while ((sLine = bufR.readLine()) != null) {
				if (bFirst) {
					if (sLine.startsWith("RefreshAll")) {
						ret = null;
						break;
					}
					bFirst = false;
				}
				Resource toAdd = new FileSystemResource(new File(sLine));
				if (!ret.contains(toAdd)) {
					ret.add(toAdd);
				}
			}
			/*
			 * Empty the file
			 */
			fc.truncate(0);
		} catch (Exception e) {
			logger.error("Failed to get the lastChanges contents.", e);
		} finally {
			try {
				lock.release();
			} catch (IOException e) {
				logger.error("Failed close bufferedReader on lastChange file.", e);
			}
			try {
				fcLock.close();
			} catch (IOException e) {
				logger.error("Failed to release lock on .lastChangeLock file.", e);
			}
			try {
				bufR.close();
			} catch (IOException e) {
				logger.error("Failed close bufferedReader on lastChange file.", e);
			}
			try {
				fc.close();
			} catch (IOException e) {
				logger.error("Failed close filechannel on lastChange file.", e);
			}

			/*
			 * No need to release the lock as it is done when closing the
			 * FileChannel
			 */

		}
		return ret;
	}

	protected void reload_new(boolean forceReload) throws IOException {

		if (resourcesPath == null) {
			/*
			 * initiate it once for all.
			 */
			resourcesPath = initializeResourcesPath();
		}

		/*
		 * Let's do it as we could miss a file being modified during the scan.
		 */
		long tmpLastCheck = lastFileTimeStamp;

		List<Resource> changedPaths = new ArrayList<Resource>();

		if (lastChangeFile != null && lastChangeFile.exists()) {
			long lastChange = lastChangeFile.lastModified();
			if (lastChange <= lastFileTimeStamp) {
				return;
			}
			if (lastChangeFile.length() > 0) {
				/*
				 * Mhh, there is something in it ! So we get the lock, read the
				 * contents, and update only the resources in this file. If the
				 * contents starts with "RefreshAll" (without the quotes), then
				 * just look at the timestamp of all resources.
				 * 
				 * @see com.odcgroup.workbench.generation.cartridge.ng.
				 * SimplerEclipseResourceFileSystemNotifier
				 */
				List<Resource> lastChangeFileContents = getLastChangeAndClear(lastChangeFile);
				if (lastChangeFileContents != null) {
					/*
					 * If null, this means the file was starting with
					 * "RefreshAll" (see previous comment)
					 */
					changedPaths = lastChangeFileContents;
				}
			}
			lastFileTimeStamp = lastChangeFile.lastModified();
		} else {
			/*
			 * We do not have the file (yet) (old EDS ?), so use the old
			 * strategy
			 * 
			 * Some file systems (FAT, NTFS) do have a write time resolution of
			 * 2 seconds see
			 * http://msdn.microsoft.com/en-us/library/ms724290%28VS.85%29.aspx
			 * So better give a 2 seconds latency.
			 */
			lastFileTimeStamp = System.currentTimeMillis() - 2000;
		}

		if (forceReload) {
			if (tmpLastCheck == 0) {
				return; // First time, no need to do anything.
			} else {
				tmpLastCheck = 0;
				changedPaths.clear();
			}
		}

		if (changedPaths.size() == 0) { // Only if nothing interesting was in
										// the lastChange file.
			List<simplePattern> lstPatterns = new ArrayList<simplePattern>();
			for (ReloadablePropertiesListener listener : preListeners) {
				String[] sPatterns = listener.getResourcePatterns();
				for (String pattern : sPatterns) {
					String orPatterns[] = pattern.split("\\|");
					for (String orPattern : orPatterns) {
						lstPatterns.add(new simplePattern(orPattern));
					}
				}
			}
			for (Resource res : resourcesPath) {
				getMoreRecentThan(new File(res.getURL().getFile()), tmpLastCheck, changedPaths, lstPatterns);
			}
		}
		long l = System.currentTimeMillis();

		for (Resource location : changedPaths) {
			try {
				String sFileName = location.getFilename();

				if (sFileName.endsWith(".xml")) {
					if (sFileName.startsWith("IRIS-T24-")) {
						logger.info("Refreshing : " + location.getFilename());
						xmlNotifier.execute(new XmlChangedEventImpl(location));
					}
				} else {
					Properties newProperties = new Properties();
					/*
					 * Ensure this property has been loaded.
					 */
					propertiesPersister.load(newProperties, location.getInputStream());
					if (reloadableProperties.updateProperties(newProperties)) {
						logger.info("Loading new : " + location.getFilename());
						reloadableProperties.notifyPropertiesLoaded(location, newProperties);
					} else {
						logger.info("Refreshing : " + location.getFilename());
						/*
						 * Notify subscribers that properties have been modified
						 */
						reloadableProperties.notifyPropertiesChanged(location, newProperties);
					}
				}
			} catch (Exception e) {
				logger.error("Unexpected error when dynamicly loading resources ", e);
			}
		}
		if (changedPaths.size() > 0) {
			logger.info(changedPaths.size() + " resources reloaded in " + (System.currentTimeMillis() - l) + " ms.");
		}

	}

	class ReloadablePropertiesImpl extends ReloadablePropertiesBase implements ReconfigurableBean {
		private static final long serialVersionUID = -3401718333944329073L;

		public void reloadConfiguration() throws Exception {
			ReloadablePropertiesFactoryBean.this.reload(false);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext ctx) throws BeansException {
		this.ctx = ctx;
	}

	class simplePattern {
		private final String startsWith;
		private final String endsWith;

		private simplePattern(String pattern) {
			pattern = pattern.replace("classpath*:", "");
			int idx = pattern.indexOf("*");
			startsWith = pattern.substring(0, idx);
			endsWith = pattern.substring(idx + 1);
		}

		private boolean matches(String s) {
			return s.startsWith(startsWith) && s.endsWith(endsWith);
		}
	}
}

package com.temenos.interaction.sdk.plugin;

/*
 * #%L
 * interaction-sdk-plugin Maven Mojo
 * %%
 * Copyright (C) 2012 - 2013 Temenos Holdings N.V.
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.temenos.interaction.sdk.JPAResponderGen;
import com.temenos.interaction.sdk.adapter.edmx.EDMXAdapter;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * A Maven plugin that generates a responder from a given EDMX file.
 */
@Mojo(name = "gen",
        requiresDependencyResolution=ResolutionScope.COMPILE)
public class ResponderGenMojo extends AbstractMojo {

    @Parameter(property = "edmxFile", alias="edmxFile")
    private String edmxFilePath;

    /**
     * Enable/disable strict odata compliance.
     */
    @Parameter
    private boolean strictOdata = true;
    
    @Parameter(property = "srcTargetDirectory")
    private String srcTargetDirectory;

    @Parameter(property = "configTargetDirectory")
    private String configTargetDirectory;

	public void setEdmxFile(String edmxFilePath) {
		this.edmxFilePath = edmxFilePath;
	}

	public void setSrcTargetDirectory(String targetDirectory) {
		this.srcTargetDirectory = targetDirectory;
	}

	public void setConfigTargetDirectory(String targetDirectory) {
		this.configTargetDirectory = targetDirectory;
	}

    public void setStrictOdata(String strictOdata) {
		this.strictOdata = (strictOdata != null && strictOdata.equalsIgnoreCase("true"));
	}
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		// check our configuration
		if (edmxFilePath == null)
			throw new MojoExecutionException("[edmxFilePath] not specified in plugin configuration");
		if (srcTargetDirectory == null)
			throw new MojoExecutionException("[srcTargetDirectory] not specified in plugin configuration");
		if (configTargetDirectory == null) {
			getLog().warn("[configTargetDirectory] not set, using [srcTargetDirectory]");
			configTargetDirectory = srcTargetDirectory;
		}
		File edmxFile = new File(edmxFilePath);
		File srcTargetDir = new File(srcTargetDirectory);
		File configTargetDir = new File(configTargetDirectory);
		execute(edmxFile, srcTargetDir, configTargetDir);
	}
	
	protected void execute(File edmxFileHandle, File srcTargetDir, File configTargetDir) throws MojoExecutionException, MojoFailureException {
		if (!srcTargetDir.exists()) {
			getLog().info("Source target directory does not exist, creating it [" + srcTargetDirectory + "]");
			srcTargetDir.mkdirs();
		}
		if (!srcTargetDir.isDirectory()) {
			getLog().error("Source target directory is invalid [" + srcTargetDirectory + "]");
			throw new MojoExecutionException("Source target directory is invalid");
		}
		if (!configTargetDir.exists()) {
			getLog().info("Configuration target directory does not exist, creating it [" + configTargetDir + "]");
			configTargetDir.mkdirs();
		}
		if (!configTargetDir.isDirectory()) {
			getLog().error("Configuration target directory is invalid [" + configTargetDir + "]");
			throw new MojoExecutionException("Configuration target directory is invalid");
		}

		boolean ok = false;
		JPAResponderGen rg = new JPAResponderGen(strictOdata);
		if (edmxFileHandle.exists()) {
			getLog().info("Generating artifacts (strict odata compliance: " + (strictOdata ? "true" : "false") + ")");
			ok = rg.generateArtifacts(new EDMXAdapter(edmxFileHandle.getAbsolutePath()), srcTargetDir, configTargetDir);
		}
		else {
			getLog().error("EDMX file does not exist.");
		}
		
		if (!ok)
			throw new MojoFailureException("An unexpected error occurred while generating artifacts");
	}

}

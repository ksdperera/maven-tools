/*
*  Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.maven.plugin.publisher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.wso2.developerstudio.eclipse.utils.data.ITemporaryFileTag;
import org.wso2.maven.capp.model.Artifact;
import org.wso2.maven.capp.mojo.AbstractPOMGenMojo;
import org.wso2.maven.capp.utils.CAppMavenUtils;
import org.wso2.maven.capp.utils.WSO2MavenPluginConstantants;
import org.wso2.maven.analytics.AnalyticsArtifact;
import org.wso2.maven.analytics.utils.AnalyticsMavenUtils;

/**
 * This is the Maven Mojo used for generating a pom for a publisher artifact 
 * from the old CApp project structure
 * 
 * @goal pom-gen
 * 
 */
public class PublisherPOMGenMojo extends AbstractPOMGenMojo {

	/**
	 * @parameter default-value="${project}"
	 */
	public MavenProject project;

	/**
	 * Maven ProjectHelper.
	 * 
	 * @component
	 */
	public MavenProjectHelper projectHelper;

	/**
	 * The path of the location to output the pom
	 * 
	 * @parameter expression="${project.build.directory}/artifacts"
	 */
	public File outputLocation;

	/**
	 * The resulting extension of the file
	 * 
	 * @parameter
	 */
	public File artifactLocation;
	
	/**
	 * POM location for the module project
	 * 
	 * @parameter expression="${project.build.directory}/pom.xml"
	 */
	public File moduleProject;
	
	/**
	 * Group id to use for the generated pom
	 * 
	 * @parameter
	 */
	public String groupId;

	/**
	 * Comma separated list of "artifact_type=extension" to be used when creating dependencies for other capp artifacts
	 * 
	 * @parameter
	 */
	public String typeList;


	private static final String ARTIFACT_TYPE="event/publisher";
	
	private List<AnalyticsArtifact> retrieveArtifacts() {
		return AnalyticsMavenUtils.retrieveArtifacts(getArtifactLocation());
	}
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		//Retrieving all the existing Analytics Artifacts for the given Maven project 
		List<AnalyticsArtifact> artifacts = retrieveArtifacts();
		
		//Artifact list
		List<Artifact> mappedArtifacts=new ArrayList<Artifact>();
		
		//Mapping Analytics Artifacts to C-App artifacts so that we can reuse the analytics-publisher-maven-plugin
		for (AnalyticsArtifact analyticsArtifact : artifacts) {
	        Artifact artifact=new Artifact();
	        artifact.setName(analyticsArtifact.getName());
	        artifact.setVersion(analyticsArtifact.getVersion());
	        artifact.setType(analyticsArtifact.getType());
	        artifact.setServerRole(analyticsArtifact.getServerRole());
	        artifact.setFile(analyticsArtifact.getFile());
	        artifact.setSource(new File(getArtifactLocation(),"artifact.xml"));
	        mappedArtifacts.add(artifact);
        }
		//Calling the process artifacts method of super type to continue the sequence.
		super.processArtifacts(mappedArtifacts);

	}
	
	
	protected void copyResources(MavenProject project, File projectLocation, Artifact artifact)throws IOException {
		ITemporaryFileTag newTag = org.wso2.developerstudio.eclipse.utils.file.FileUtils.createNewTempTag();
		File publisherArtifact = processTokenReplacement(artifact);
		if (publisherArtifact == null) {
			publisherArtifact = artifact.getFile();
		}
		FileUtils.copyFile(publisherArtifact, new File(projectLocation, artifact.getFile().getName()));
		newTag.clearAndEnd();
	}
	protected void addPlugins(MavenProject artifactMavenProject, Artifact artifact) {
		Plugin plugin = CAppMavenUtils.createPluginEntry(artifactMavenProject,"org.wso2.maven","analytics-publisher-maven-plugin",WSO2MavenPluginConstantants.MAVEN_ANALYTICS_PLUGIN_VERSION,true);
		Xpp3Dom configuration = (Xpp3Dom)plugin.getConfiguration();
		//add configuration
		Xpp3Dom aritfact = CAppMavenUtils.createConfigurationNode(configuration,"artifact");
		aritfact.setValue(artifact.getFile().getName());
	}

	protected String getArtifactType() {
		return ARTIFACT_TYPE;
	}
}

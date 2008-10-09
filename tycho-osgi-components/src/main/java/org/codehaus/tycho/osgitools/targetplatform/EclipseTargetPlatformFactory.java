package org.codehaus.tycho.osgitools.targetplatform;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.PluginRef;
import org.codehaus.tycho.osgitools.OsgiState;

public class EclipseTargetPlatformFactory extends AbstractLogEnabled {

	public static final String PACKAGING_ECLIPSE_INSTALLATION = "eclipse-installation";
	public static final String PACKAGING_ECLIPSE_PLUGIN = "eclipse-plugin";
	public static final String PACKAGING_ECLIPSE_FEATURE = "eclipse-feature";

	private ArtifactResolver artifactResolver;
	private ArtifactFactory artifactFactory;

	private ArtifactRepository localRepository;
	
	public EclipseTargetPlatformFactory(Logger logger, ArtifactResolver artifactResolver, ArtifactFactory artifactFactory, ArtifactRepository localRepository) {
		this.artifactResolver = artifactResolver;
		this.artifactFactory = artifactFactory;
		this.localRepository = localRepository;
		enableLogging(logger);
	}

	public void createTargetPlatform(List<MavenProject> projects, OsgiState state) {

		File installation = getEclipseInstallation(projects);
		if (installation != null) {
			createTargetPlatform(state, installation);
		}

		Set<File> features = new LinkedHashSet<File>();
		Set<File> bundles = new LinkedHashSet<File>();

		Map<Artifact, Exception> exceptions = new HashMap<Artifact, Exception>();

		// no P2 support for now, will add later
		for (MavenProject project : projects) {
			Map<String, Artifact> versionMap = project.getManagedVersionMap();
			if (versionMap != null) {
				for (Artifact artifact : versionMap.values()) {
					try {
						if (PACKAGING_ECLIPSE_FEATURE.equals(artifact.getType())) {
							resolveFeature(artifact, features, bundles, project.getRemoteArtifactRepositories());
						} else if (PACKAGING_ECLIPSE_PLUGIN.equals(artifact.getType())) {
							resolvePlugin(artifact, bundles, project.getRemoteArtifactRepositories());
						}
					} catch (Exception e) {
						exceptions.put(artifact, e);
					}
				}
			}
		}
		
		if (!exceptions.isEmpty()) {
			throw new TargetPlatformException("Cannot resolve target platform", exceptions);
		}
		
		state.addSite(new File(localRepository.getBasedir()), features, bundles);

	}

	public void createTargetPlatform(OsgiState state, File installation) {
		state.setTargetPlatform(installation);

		EclipseInstallationLayout finder = new EclipseInstallationLayout(getLogger(), installation);
		Set<File> sites = finder.getSites();
		for (File site : sites) {
			Set<File> features = finder.getFeatures(site);
			Set<File> bundles = finder.getPlugins(site);
			state.addSite(site, features, bundles);
		}
	}

	private void resolveFeature(Artifact artifact, Set<File> features, Set<File> bundles, List<ArtifactRepository> remoteRepositories) throws AbstractArtifactResolutionException, IOException, XmlPullParserException {
		resolveArtifact(artifact, remoteRepositories);
		features.add(artifact.getFile());
		// XXX unpack feature
		Feature feature = Feature.read(artifact.getFile());
		for (PluginRef ref : feature.getPlugins()) {
			Artifact includedArtifact = artifactFactory.createArtifact(ref.getId(), ref.getId(), ref.getVersion(), null, PACKAGING_ECLIPSE_PLUGIN);
			resolvePlugin(includedArtifact, bundles, remoteRepositories);
		}
		for (Feature.FeatureRef ref : feature.getIncludedFeatures()) {
			Artifact includedArtifact = artifactFactory.createArtifact(ref.getId(), ref.getId(), ref.getVersion(), null, PACKAGING_ECLIPSE_FEATURE);
			resolveFeature(includedArtifact, features, bundles, remoteRepositories);
		}
	}

	private void assertResolved(Artifact artifact) throws ArtifactNotFoundException {
		if (!artifact.isResolved() || artifact.getFile() == null || !artifact.getFile().canRead()) {
			throw new ArtifactNotFoundException("Artifact is not resolved", artifact);
		}
	}

	private void resolvePlugin(Artifact artifact, Set<File> bundles, List<ArtifactRepository> remoteRepositories) throws AbstractArtifactResolutionException {
		resolveArtifact(artifact, remoteRepositories);
		bundles.add(artifact.getFile());
	}

	private void resolveArtifact(Artifact artifact, List<ArtifactRepository> remoteRepositories) throws AbstractArtifactResolutionException	{
		artifactResolver.resolve(artifact, remoteRepositories, localRepository);
		assertResolved(artifact);
	}

	private File getEclipseInstallation(List<MavenProject> projects) {
		File installation = null;
		for (MavenProject project : projects) {
			Map<String, Artifact> versionMap = project.getManagedVersionMap();
			if (versionMap != null) {
				for (Artifact artifact : versionMap.values()) {
					if (PACKAGING_ECLIPSE_INSTALLATION.equals(artifact.getType())) {
						if (installation == null) {
							installation = artifact.getFile();
						} else {
							if (!installation.equals(artifact.getFile())) {
								throw new TargetPlatformException("No more than one eclipse-installation and/or eclipse-distriction");
							}
						}
					}
				}
			}
		}
		return installation;
	}
}

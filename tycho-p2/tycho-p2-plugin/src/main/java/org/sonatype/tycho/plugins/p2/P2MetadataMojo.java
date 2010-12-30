package org.sonatype.tycho.plugins.p2;

import static org.sonatype.tycho.p2.repository.RepositoryLayoutHelper.CLASSIFIER_P2_ARTIFACTS;
import static org.sonatype.tycho.p2.repository.RepositoryLayoutHelper.CLASSIFIER_P2_METADATA;
import static org.sonatype.tycho.p2.repository.RepositoryLayoutHelper.EXTENSION_P2_ARTIFACTS;
import static org.sonatype.tycho.p2.repository.RepositoryLayoutHelper.EXTENSION_P2_METADATA;
import static org.sonatype.tycho.p2.repository.RepositoryLayoutHelper.FILE_NAME_LOCAL_ARTIFACTS;
import static org.sonatype.tycho.p2.repository.RepositoryLayoutHelper.FILE_NAME_P2_ARTIFACTS;
import static org.sonatype.tycho.p2.repository.RepositoryLayoutHelper.FILE_NAME_P2_METADATA;
import static org.sonatype.tycho.p2.repository.RepositoryLayoutHelper.KEY_ARTIFACT_ATTACHED;
import static org.sonatype.tycho.p2.repository.RepositoryLayoutHelper.KEY_ARTIFACT_MAIN;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.FileUtils;
import org.sonatype.tycho.equinox.EquinoxServiceFactory;
import org.sonatype.tycho.p2.IArtifactFacade;
import org.sonatype.tycho.p2.P2Generator;
import org.sonatype.tycho.p2.facade.internal.ArtifactFacade;

/**
 * @goal p2-metadata
 */
public class P2MetadataMojo
    extends AbstractMojo
{
    /** @parameter expression="${project}" */
    protected MavenProject project;

    /** @parameter default-value="true" */
    protected boolean attachP2Metadata;

    /** @component */
    protected MavenProjectHelper projectHelper;

    /** @component */
    private EquinoxServiceFactory equinox;

    private P2Generator p2;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        attachP2Metadata();
    }

    protected P2Generator getP2Generator()
    {
        if ( p2 == null )
        {
            p2 = equinox.getService( P2Generator.class );

            if ( p2 == null )
            {
                throw new IllegalStateException( "Could not acquire P2 metadata service" );
            }
        }
        return p2;
    }

    protected void attachP2Metadata()
        throws MojoExecutionException
    {
        if ( !attachP2Metadata )
        {
            return;
        }

        File file = project.getArtifact().getFile();

        if ( file == null || !file.canRead() )
        {
            throw new IllegalStateException();
        }

        File targetDir = new File( project.getBuild().getDirectory() );

        Map<String, IArtifactFacade> attachedArtifacts = new HashMap<String, IArtifactFacade>();

        ArtifactFacade projectDefaultArtifact = new ArtifactFacade( project.getArtifact() );

        Artifact p2contentArtifact =
            createP2Artifact( projectDefaultArtifact, EXTENSION_P2_METADATA, CLASSIFIER_P2_METADATA,
                              FILE_NAME_P2_METADATA, targetDir );
        attachedArtifacts.put( CLASSIFIER_P2_METADATA, new ArtifactFacade( p2contentArtifact ) );

        Artifact p2artifactsArtifact =
            createP2Artifact( projectDefaultArtifact, EXTENSION_P2_ARTIFACTS, CLASSIFIER_P2_ARTIFACTS,
                              FILE_NAME_P2_ARTIFACTS, targetDir );
        attachedArtifacts.put( CLASSIFIER_P2_ARTIFACTS, new ArtifactFacade( p2artifactsArtifact ) );

        try
        {
            List<IArtifactFacade> artifacts = new ArrayList<IArtifactFacade>();

            artifacts.add( projectDefaultArtifact );

            for ( Artifact artifact : project.getArtifactMap().values() )
            {
                artifacts.add( new ArtifactFacade( artifact ) );
            }

            getP2Generator().generateMetadata( artifacts, attachedArtifacts );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not generate P2 metadata", e );
        }

        for ( Entry<String, IArtifactFacade> entry : attachedArtifacts.entrySet() )
        {
            IArtifactFacade artifactFacade = entry.getValue();

            // copy artifact if not already exists target dir
            File sourceArtifact = artifactFacade.getLocation();
            File targetArtifact = new File( targetDir, sourceArtifact.getName() );

            if ( !targetArtifact.exists() )
            {
                try
                {
                    FileUtils.copyFile( sourceArtifact, targetArtifact );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException(
                                                      "Could not copy source artifact file to output directory. Source: "
                                                          + sourceArtifact.getAbsolutePath() + "; output directory: "
                                                          + targetDir.getAbsolutePath(), e );
                }
                // clean up actions
                sourceArtifact.delete();
                File parentFile = sourceArtifact.getParentFile();
                if ( parentFile.list().length == 0 )
                {
                    parentFile.delete();
                }

            }

            projectHelper.attachArtifact( project, artifactFacade.getPackagingType(), artifactFacade.getClassidier(),
                                          targetArtifact );
        }

        File localArtifactsFile =
            new File( project.getBuild().getDirectory(), FILE_NAME_LOCAL_ARTIFACTS );
        writeArtifactLocations( localArtifactsFile, getAllProjectArtifacts( project ) );
    }

    private static DefaultArtifact createP2Artifact( ArtifactFacade projectDefaultArtifact, String extension,
                                                     String classifier, String p2ArtifactFileName, File targetDir )
    {
        DefaultArtifact p2Artifact =
            new DefaultArtifact( projectDefaultArtifact.getGroupId(), projectDefaultArtifact.getArtifactId(),
                                 projectDefaultArtifact.getVersion(), null, extension, classifier, null );
        p2Artifact.setFile( new File( targetDir, p2ArtifactFileName ) );
        return p2Artifact;
    }

    /**
     * Returns a map from classifiers to artifact files of the given project. The classifier
     * <code>null</code> is mapped to the project's main artifact.
     */
    private static Map<String, File> getAllProjectArtifacts( MavenProject project )
    {
        Map<String, File> artifacts = new HashMap<String, File>();
        Artifact mainArtifact = project.getArtifact();
        if ( mainArtifact != null )
        {
            artifacts.put( null, mainArtifact.getFile() );
        }
        for ( Artifact attachedArtifact : project.getAttachedArtifacts() )
        {
            artifacts.put( attachedArtifact.getClassifier(), attachedArtifact.getFile() );
        }
        return artifacts;
    }

    static void writeArtifactLocations( File outputFile, Map<String, File> artifactLocations )
        throws MojoExecutionException
    {
        Properties outputProperties = new Properties();

        for ( Entry<String, File> entry : artifactLocations.entrySet() )
        {
            if ( entry.getKey() == null )
            {
                outputProperties.put( KEY_ARTIFACT_MAIN, entry.getValue().getAbsolutePath() );
            }
            else
            {
                outputProperties.put( KEY_ARTIFACT_ATTACHED + entry.getKey(), entry.getValue().getAbsolutePath() );
            }
        }

        writeProperties( outputProperties, outputFile );
    }

    private static void writeProperties( Properties properties, File outputFile )
        throws MojoExecutionException
    {
        FileOutputStream outputStream;
        try
        {
            outputStream = new FileOutputStream( outputFile );

            try
            {
                properties.store( outputStream, null );
            }
            finally
            {
                outputStream.close();
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "I/O exception while writing " + outputFile, e );
        }
    }
}

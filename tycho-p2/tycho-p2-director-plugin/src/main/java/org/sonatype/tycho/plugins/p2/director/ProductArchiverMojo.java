package org.sonatype.tycho.plugins.p2.director;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.tycho.TargetEnvironment;

/**
 * @goal archive-products
 * @phase package
 */
public final class ProductArchiverMojo
    extends AbstractProductMojo
{
    /**
     * @component role="org.codehaus.plexus.archiver.Archiver" role-hint="zip"
     */
    private Archiver inflater;

    /**
     * @component
     */
    private MavenProjectHelper helper;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        ProductConfig config = getProductConfig();
        if ( !config.uniqueAttachIds() )
        {
            throw new MojoFailureException( "Artifact file names for the archived products are not unique. "
                + "Configure the attachId or select a subset of products. Current configuration: "
                + config.getProducts() );
        }
        for ( Product product : config.getProducts() )
        {
            for ( TargetEnvironment env : getEnvironments() )
            {
                File productArchive =
                    new File( getProductsBuildDirectory(), product.getId() + "-" + getOsWsArch( env, '.' ) + ".zip" );

                try
                {
                    inflater.setDestFile( productArchive );
                    inflater.addDirectory( getProductMaterializeDirectory( product, env ) );
                    inflater.createArchive();
                }
                catch ( ArchiverException e )
                {
                    throw new MojoExecutionException( "Error packing product", e );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( "Error packing product", e );
                }

                final String artifactClassifier = getArtifactClassifier( product, env );
                helper.attachArtifact( getProject(), productArchive, artifactClassifier );
            }
        }
    }

    static String getArtifactClassifier( Product product, TargetEnvironment environment )
    {
        // classifier (and hence artifact file name) ends with os.ws.arch (similar to Eclipse
        // download packages)
        final String artifactClassifier;
        if ( product.getAttachId() == null )
        {
            artifactClassifier = getOsWsArch( environment, '.' );
        }
        else
        {
            artifactClassifier = product.getAttachId() + "-" + getOsWsArch( environment, '.' );
        }
        return artifactClassifier;
    }
}

package org.sonatype.tycho.p2.impl.publisher.repo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactDescriptor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.publisher.IPublisherAdvice;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.actions.IPropertyAdvice;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.spi.p2.publisher.PublisherHelper;
import org.sonatype.tycho.p2.IArtifactFacade;
import org.sonatype.tycho.p2.impl.publisher.FeatureRootAdvice;
import org.sonatype.tycho.p2.impl.publisher.MavenPropertiesAdvice;
import org.sonatype.tycho.p2.repository.RepositoryLayoutHelper;

@SuppressWarnings( "restriction" )
public class FeatureRootfileArtifactRepository
    extends TransientArtifactRepository
{

    /**
     * This class is used to transport the required information for installing the artifact into the local repository.
     * 
     * @see org.sonatype.tycho.p2.impl.publisher.P2GeneratorImpl
     */
    private static class RootfileArtifact
        implements IArtifactFacade
    {
        static final String ROOTFILE_CLASSIFIER = "root";

        private static final String ROOTFILE_EXTENSION = "zip";

        private final File location;

        private String classifier;

        public RootfileArtifact( File location, String classifier )
        {
            this.location = location;
            this.classifier = classifier;
        }

        public String getPackagingType()
        {
            return ROOTFILE_EXTENSION;
        }

        public String getClassidier()
        {
            return this.classifier;
        }

        public File getLocation()
        {
            return location;
        }

        public String getGroupId()
        {
            throw new UnsupportedOperationException();
        }

        public String getArtifactId()
        {
            throw new UnsupportedOperationException();
        }

        public String getVersion()
        {
            throw new UnsupportedOperationException();
        }
    }

    private File rootfilesArtifactTempDir;

    private final PublisherInfo publisherInfo;

    private Map<String, IArtifactFacade> attachedArtifacts = new HashMap<String, IArtifactFacade>();

    public FeatureRootfileArtifactRepository( PublisherInfo publisherInfo )
    {
        this.publisherInfo = publisherInfo;
    }

    @Override
    public OutputStream getOutputStream( IArtifactDescriptor descriptor )
        throws ProvisionException
    {
        IArtifactKey artifactKey = descriptor.getArtifactKey();
        if ( artifactKey != null && PublisherHelper.BINARY_ARTIFACT_CLASSIFIER.equals( artifactKey.getClassifier() ) )
        {
            try
            {
                return createRootfileOutputStream( artifactKey );
            }
            catch ( IOException e )
            {
                throw new ProvisionException( e.getMessage(), e );
            }
        }

        return super.getOutputStream( descriptor );
    }

    private OutputStream createRootfileOutputStream( IArtifactKey artifactKey )
        throws ProvisionException, IOException
    {
        if ( this.rootfilesArtifactTempDir == null )
        {
            this.rootfilesArtifactTempDir = createRootfilesArtifactTempDir();
        }

        File outputFile =
            new File( this.rootfilesArtifactTempDir, artifactKey.getId() + "-" + artifactKey.getVersion() + "-"
                + RootfileArtifact.ROOTFILE_CLASSIFIER + "." + RootfileArtifact.ROOTFILE_EXTENSION );

        OutputStream target = null;
        try
        {
            SimpleArtifactDescriptor simpleArtifactDescriptor =
                (SimpleArtifactDescriptor) createArtifactDescriptor( artifactKey );

            Collection<IPropertyAdvice> advices =
                publisherInfo.getAdvice( null, false, simpleArtifactDescriptor.getArtifactKey().getId(),
                                         simpleArtifactDescriptor.getArtifactKey().getVersion(), IPropertyAdvice.class );

            boolean mavenPropAdviseExists = false;
            for ( IPropertyAdvice entry : advices )
            {
                if ( entry instanceof MavenPropertiesAdvice )
                {
                    mavenPropAdviseExists = true;
                    entry.getArtifactProperties( null, simpleArtifactDescriptor );
                }
            }

            if ( !mavenPropAdviseExists )
            {
                throw new ProvisionException( "MavenPropertiesAdvice does not exist for artifact: "
                    + simpleArtifactDescriptor );
            }

            String mavenArtifactClassifier =
                getRootFileArtifactClassifier( simpleArtifactDescriptor.getArtifactKey().getId() );
            simpleArtifactDescriptor.setProperty( RepositoryLayoutHelper.PROP_CLASSIFIER, mavenArtifactClassifier );
            simpleArtifactDescriptor.setProperty( RepositoryLayoutHelper.PROP_EXTENSION, RootfileArtifact.ROOTFILE_EXTENSION );

            target = new BufferedOutputStream( new FileOutputStream( outputFile ) );

            RootfileArtifact rootfileArtifact = new RootfileArtifact( outputFile, mavenArtifactClassifier );
            this.attachedArtifacts.put( rootfileArtifact.getClassidier(), rootfileArtifact );

            descriptors.add( simpleArtifactDescriptor );
        }
        catch ( FileNotFoundException e )
        {
            throw new ProvisionException( e.getMessage(), e );
        }

        return target;
    }

    String getRootFileArtifactClassifier( String artifactId )
    {
        List<IPublisherAdvice> adviceList = this.publisherInfo.getAdvice();

        for ( IPublisherAdvice publisherAdvice : adviceList )
        {
            if ( publisherAdvice instanceof FeatureRootAdvice )
            {
                String[] configurations = ( (FeatureRootAdvice) publisherAdvice ).getConfigurations();

                for ( String config : configurations )
                {
                    if ( !"".equals( config ) && artifactId.endsWith( config ) )
                    {
                        return RootfileArtifact.ROOTFILE_CLASSIFIER + "." + config;
                    }
                }
            }
        }

        return RootfileArtifact.ROOTFILE_CLASSIFIER;
    }

    public Map<String, IArtifactFacade> getAttachedArtifacts()
    {
        return attachedArtifacts;
    }

    private File createRootfilesArtifactTempDir()
        throws IOException
    {
        this.rootfilesArtifactTempDir = File.createTempFile( "rootfilesArtifactTempDir", "" );
        this.rootfilesArtifactTempDir.delete();
        this.rootfilesArtifactTempDir.mkdirs();

        return ( this.rootfilesArtifactTempDir );
    }

}

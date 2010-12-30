package org.sonatype.tycho.p2.impl.publisher.repo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactDescriptor;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRequest;
import org.eclipse.equinox.p2.repository.artifact.spi.AbstractArtifactRepository;

public class TransientArtifactRepository
    extends AbstractArtifactRepository
{

    protected Set<IArtifactDescriptor> descriptors = new LinkedHashSet<IArtifactDescriptor>();

    private Set<IArtifactKey> keys = new LinkedHashSet<IArtifactKey>();

    public TransientArtifactRepository()
    {
        super( null, "TransientArtifactRepository", TransientArtifactRepository.class.getName(), "1.0.0", null, null,
               null, null );
    }

    @Override
    public boolean contains( IArtifactDescriptor descriptor )
    {
        return descriptors.contains( descriptor );
    }

    @Override
    public boolean contains( IArtifactKey key )
    {
        return keys.contains( key );
    }

    @Override
    public IStatus getArtifact( IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public IArtifactDescriptor[] getArtifactDescriptors( IArtifactKey key )
    {
        throw new UnsupportedOperationException();
    }

    public IQueryResult<IArtifactKey> query( IQuery<IArtifactKey> query, IProgressMonitor monitor )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public IQueryable<IArtifactDescriptor> descriptorQueryable()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IStatus getArtifacts( IArtifactRequest[] requests, IProgressMonitor monitor )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream getOutputStream( IArtifactDescriptor descriptor )
        throws ProvisionException
    {
        descriptors.add( descriptor );
        return new OutputStream()
        {
            @Override
            public void write( int b )
                throws IOException
            {
            }
        };
    }

    @Override
    public void addDescriptor( IArtifactDescriptor descriptor )
    {
        descriptors.add( descriptor );
    }

    @Override
    public void addDescriptors( IArtifactDescriptor[] descriptors )
    {
        this.descriptors.addAll( Arrays.asList( descriptors ) );
    }

    public IStatus getRawArtifact( IArtifactDescriptor descriptor, OutputStream destination, IProgressMonitor monitor )
    {
        throw new UnsupportedOperationException();
    }

    public Set<IArtifactDescriptor> getArtifactDescriptors()
    {
        return descriptors;
    }

    @Override
    public boolean isModifiable()
    {
        return true;
    }

    @Override
    public IArtifactDescriptor createArtifactDescriptor( IArtifactKey key )
    {
        // this is necessary for MavenPropertiesAdvice to work
        return new SimpleArtifactDescriptor(key);
    }
}

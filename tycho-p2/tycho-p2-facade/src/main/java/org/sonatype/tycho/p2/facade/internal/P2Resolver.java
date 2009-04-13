package org.sonatype.tycho.p2.facade.internal;

import java.io.File;
import java.net.URI;
import java.util.Properties;

import org.codehaus.tycho.ProjectType;

public interface P2Resolver
{
    public static final String TYPE_OSGI_BUNDLE = ProjectType.OSGI_BUNDLE;
    
    public static final String TYPE_ECLIPSE_FEATURE = ProjectType.ECLIPSE_FEATURE;
    
    public static final String TYPE_ECLIPSE_TEST_PLUGIN = ProjectType.ECLIPSE_TEST_PLUGIN;

    /**
     * Pseudo artifact type used to denote P2 installable unit dependencies 
     */
    public static final String TYPE_INSTALLABLE_UNIT = "p2-installable-unit";
    
    public void addMavenProject( File location, String type, String groupId, String artifactId, String version );

    public void addRepository( URI location );

    public void setLocalRepositoryLocation( File location );

    public void setProperties( Properties properties );

    public P2ResolutionResult resolve( File location );

    public void addDependency( String type, String id, String version );
}

package org.codehaus.tycho.osgitools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.maven.project.MavenProject;
import org.codehaus.tycho.ArtifactDependencyVisitor;
import org.codehaus.tycho.ArtifactDependencyWalker;
import org.codehaus.tycho.ArtifactDescription;
import org.codehaus.tycho.ArtifactKey;
import org.codehaus.tycho.PluginDescription;
import org.codehaus.tycho.TargetEnvironment;
import org.codehaus.tycho.TargetPlatform;
import org.codehaus.tycho.TychoProject;
import org.codehaus.tycho.model.Feature;
import org.codehaus.tycho.model.FeatureRef;
import org.codehaus.tycho.model.PluginRef;
import org.codehaus.tycho.model.ProductConfiguration;
import org.codehaus.tycho.model.UpdateSite;
import org.codehaus.tycho.utils.PlatformPropertiesUtils;

public abstract class AbstractArtifactDependencyWalker
    implements ArtifactDependencyWalker
{
    public static final String EQUINOX_LAUNCHER = "org.eclipse.equinox.launcher";

    private final TargetPlatform platform;

    private final TargetEnvironment[] environments;

    public AbstractArtifactDependencyWalker( TargetPlatform platform )
    {
        this( platform, null );
    }

    public AbstractArtifactDependencyWalker( TargetPlatform platform, TargetEnvironment[] environments )
    {
        this.platform = platform;
        this.environments = environments;
    }

    public void traverseUpdateSite( UpdateSite site, ArtifactDependencyVisitor visitor )
    {
        WalkbackPath visited = new WalkbackPath();

        for ( FeatureRef ref : site.getFeatures() )
        {
            traverseFeature( ref, visitor, visited );
        }
    }

    public void traverseFeature( File location, Feature feature, ArtifactDependencyVisitor visitor )
    {
        traverseFeature( location, feature, null, visitor, new WalkbackPath() );
    }

    protected void traverseFeature( File location, Feature feature, FeatureRef featureRef,
                                    ArtifactDependencyVisitor visitor, WalkbackPath visited )
    {
        ArtifactDescription artifact = platform.getArtifact( location );

        if ( artifact == null )
        {
            // ah?
            throw new IllegalStateException( "Feature " + location
                + " is not part of the project build target platform" );
        }

        ArtifactKey key = artifact.getKey();
        MavenProject project = artifact.getMavenProject();

        DefaultFeatureDescription description =
            new DefaultFeatureDescription( key, location, project, feature, featureRef );

        if ( visitor.visitFeature( description ) )
        {
            for ( PluginRef ref : feature.getPlugins() )
            {
                traversePlugin( ref, visitor, visited );
            }

            for ( FeatureRef ref : feature.getIncludedFeatures() )
            {
                traverseFeature( ref, visitor, visited );
            }
        }
    }

    public void traverseProduct( ProductConfiguration product, ArtifactDependencyVisitor visitor )
    {
        WalkbackPath visited = new WalkbackPath();

        if ( product.useFeatures() )
        {
            for ( FeatureRef ref : product.getFeatures() )
            {
                traverseFeature( ref, visitor, visited );
            }
        }
        else
        {
            for ( PluginRef ref : product.getPlugins() )
            {
                traversePlugin( ref, visitor, visited );
            }
        }

        Set<String> bundles = new HashSet<String>();
        for ( ArtifactDescription artifact : visited.getVisited() )
        {
            ArtifactKey key = artifact.getKey();
            if ( TychoProject.ECLIPSE_PLUGIN.equals( key.getType() ) )
            {
                bundles.add( key.getId() );
            }
        }

        // RCP apparently implicitly includes equinox.launcher and corresponding native fragments
        // See also org.sonatype.tycho.p2.ProductDependenciesAction.perform

        if ( !bundles.contains( EQUINOX_LAUNCHER ) )
        {
            PluginRef ref = new PluginRef( "plugin" );
            ref.setId( EQUINOX_LAUNCHER );
            traversePlugin( ref, visitor, visited );
        }

        if ( environments != null )
        {
            for ( TargetEnvironment environment : environments )
            {
                String os = environment.getOs();
                String ws = environment.getWs();
                String arch = environment.getArch();

                String id;

                // for Mac OS X there is no org.eclipse.equinox.launcher.carbon.macosx.x86 folder,
                // only a org.eclipse.equinox.launcher.carbon.macosx folder.
                // see http://jira.codehaus.org/browse/MNGECLIPSE-1075
                if ( PlatformPropertiesUtils.OS_MACOSX.equals( os ) && PlatformPropertiesUtils.ARCH_X86.equals( arch ) )
                {
                    id = "org.eclipse.equinox.launcher." + ws + "." + os;
                }
                else
                {
                    id = "org.eclipse.equinox.launcher." + ws + "." + os + "." + arch;
                }

                if ( !bundles.contains( id ) )
                {
                    PluginRef ref = new PluginRef( "plugin" );
                    ref.setId( id );
                    ref.setOs( os );
                    ref.setWs( ws );
                    ref.setArch( arch );
                    ref.setUnpack( true );
                    traversePlugin( ref, visitor, visited );
                }
            }
        }
    }

    protected void traverseFeature( FeatureRef ref, ArtifactDependencyVisitor visitor, WalkbackPath visited )
    {
        ArtifactDescription artifact =
            platform.getArtifact( TychoProject.ECLIPSE_FEATURE, ref.getId(), ref.getVersion() );

        if ( artifact != null )
        {
            if ( visited.visited( artifact.getKey() ) )
            {
                return;
            }

            visited.enter( artifact );
            try
            {
                File location = artifact.getLocation();

                Feature feature = Feature.loadFeature( location );
                traverseFeature( location, feature, ref, visitor, visited );
            }
            finally
            {
                visited.leave( artifact );
            }
        }
        else
        {
            visitor.missingFeature( ref, visited.getWalkback() );
        }
    }

    private void traversePlugin( PluginRef ref, ArtifactDependencyVisitor visitor, WalkbackPath visited )
    {
        if ( !matchTargetEnvironment( ref ) )
        {
            return;
        }

        ArtifactDescription artifact =
            platform.getArtifact( TychoProject.ECLIPSE_PLUGIN, ref.getId(), ref.getVersion() );

        if ( artifact != null )
        {
            ArtifactKey key = artifact.getKey();
            if ( visited.visited( key ) )
            {
                return;
            }

            File location = artifact.getLocation();

            MavenProject project = platform.getMavenProject( location );
            PluginDescription description = new DefaultPluginDescription( key, location, project, ref );
            visited.enter( description );
            try
            {
                visitor.visitPlugin( description );
            }
            finally
            {
                visited.leave( description );
            }
        }
        else
        {
            visitor.missingPlugin( ref, visited.getWalkback() );
        }
    }

    private boolean matchTargetEnvironment( PluginRef pluginRef )
    {
        String pluginOs = pluginRef.getOs();
        String pluginWs = pluginRef.getWs();
        String pluginArch = pluginRef.getArch();

        if ( environments == null )
        {
            // match all environments be default
            return true;

            // no target environments, only include environment independent plugins
            // return pluginOs == null && pluginWs == null && pluginArch == null;
        }

        for ( TargetEnvironment environment : environments )
        {
            if ( environment.match( pluginOs, pluginWs, pluginArch ) )
            {
                return true;
            }
        }

        return false;
    }

    private static class WalkbackPath
    {
        private Map<ArtifactKey, ArtifactDescription> visited = new HashMap<ArtifactKey, ArtifactDescription>();

        private Stack<ArtifactDescription> walkback = new Stack<ArtifactDescription>();

        boolean visited( ArtifactKey key )
        {
            return visited.containsKey( key );
        }

        public List<ArtifactDescription> getWalkback()
        {
            return new ArrayList<ArtifactDescription>( walkback );
        }

        void enter( ArtifactDescription artifact )
        {
            visited.put( artifact.getKey(), artifact );
            walkback.push( artifact );
        }

        void leave( ArtifactDescription artifact )
        {
            walkback.pop();
        }

        Collection<ArtifactDescription> getVisited()
        {
            return visited.values();
        }
    }
}

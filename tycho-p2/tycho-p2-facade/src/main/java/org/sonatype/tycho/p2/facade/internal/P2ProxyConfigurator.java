package org.sonatype.tycho.p2.facade.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.settings.Proxy;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.tycho.equinox.embedder.EquinoxEmbedder;
import org.sonatype.tycho.equinox.embedder.EquinoxLifecycleListener;
import org.sonatype.tycho.p2.ProxyServiceFacade;

@Component( role = EquinoxLifecycleListener.class, hint = "P2ProxyConfigurator" )
public class P2ProxyConfigurator
    extends EquinoxLifecycleListener
{
    @Requirement
    private Logger logger;

    @Requirement
    private LegacySupport context;

    @Override
    public void afterFrameworkStarted( EquinoxEmbedder framework )
    {
        MavenSession session = context.getSession();

        final List<Proxy> activeProxies = new ArrayList<Proxy>();
        for ( Proxy proxy : session.getSettings().getProxies() )
        {
            if ( proxy.isActive() )
            {
                activeProxies.add( proxy );
            }
        }

        ProxyServiceFacade proxyService;
        proxyService = framework.getService( ProxyServiceFacade.class );
        // make sure there is no old state from previous aborted builds
        logger.debug( "clear OSGi proxy settings" );
        proxyService.clearPersistentProxySettings();
        for ( Proxy proxy : activeProxies )
        {
            logger.debug( "Configure OSGi proxy for protocol " + proxy.getProtocol() + ", host: " + proxy.getHost()
                + ", port: " + proxy.getPort() + ", nonProxyHosts: " + proxy.getNonProxyHosts() );
            proxyService.configureProxy( proxy.getProtocol(), proxy.getHost(), proxy.getPort(), proxy.getUsername(),
                                         proxy.getPassword(), proxy.getNonProxyHosts() );
        }
    }

}

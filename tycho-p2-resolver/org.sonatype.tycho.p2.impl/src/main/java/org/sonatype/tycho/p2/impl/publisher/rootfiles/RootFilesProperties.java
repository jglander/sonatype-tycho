package org.sonatype.tycho.p2.impl.publisher.rootfiles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RootFilesProperties
{

    public class Permission
    {
        private final String path;

        private final String chmodPermissionPattern;

        public Permission( String path, String chmodPermissionPattern )
        {
            this.path = path.trim();
            this.chmodPermissionPattern = chmodPermissionPattern;
        }

        public String[] toP2Format()
        {
            return new String[] { chmodPermissionPattern, path };
        }

    }

    private List<Permission> permissions = new ArrayList<Permission>();

    private String links = "";

    public Collection<Permission> getPermissions()
    {
        return permissions;
    }

    public void addPermission( String chmodPermissionPattern, String[] pathsInInstallation )
    {
        for ( String path : pathsInInstallation )
        {
            permissions.add( new Permission( path, chmodPermissionPattern ) );
        }
    }

    public String getLinks()
    {
        return links;
    }

    public void addLinks( String[] linkValueSegments )
    {
        verifySpecifiedInPairs( linkValueSegments );
        for ( String segment : linkValueSegments )
        {
            addTrimmedLinkSegment( segment );
        }
    }

    private static void verifySpecifiedInPairs( String[] linkValueSegments )
    {
        if ( linkValueSegments.length % 2 != 0 )
        {
            String message = "Links must be specified as a sequence of \"link target,link name\" pairs; the actual value \""
                + TextHelper.segmentsToString( linkValueSegments, ',' ) + "\" contains an odd number of segments";
            throw new IllegalArgumentException( message );
        }
    }

    private void addTrimmedLinkSegment( String segment )
    {
        if ( !links.isEmpty() )
        {
            links += ",";
        }
        links += segment.trim();
    }
}

package org.sonatype.tycho.plugins.p2.director;

/**
 * Value object for the configuration of this Maven plug-in. Used to select products to be
 * materialized and to specify the classifier under which the product archives artifacts are
 * attached.
 */
public final class Product
{
    /**
     * Installable unit ID of the product. In the .product file, this corresponds to the 'uid'
     * attribute in 'product' tag.
     */
    private String id;

    /**
     * The classifier for materialized products is this string followed by the platform (OS, WS
     * Arch). May be omitted.
     */
    private String attachId;

    public Product()
    {
    }

    Product( String id )
    {
        this.id = id;
    }

    Product( String id, String attachId )
    {
        this.id = id;
        this.attachId = attachId;
    }

    public String getId()
    {
        return id;
    }

    public String getAttachId()
    {
        return attachId;
    }

    @Override
    public String toString()
    {
        return "Product [id=" + id + ", attachId=" + attachId + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( attachId == null ) ? 0 : attachId.hashCode() );
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;
        if ( obj instanceof Product )
        {
            Product other = (Product) obj;
            return equals( this.id, other.id ) && equals( this.attachId, other.attachId );
        }
        return false;
    }

    private <T> boolean equals( T left, T right )
    {
        if ( left == right )
            return true;
        else if ( left == null )
            return false;
        else
            return left.equals( right );
    }
}
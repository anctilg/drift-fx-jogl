package dev.anctil.fx.drift.jogl;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.scene.NodeHelper;
import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.util.Utils;

import javafx.scene.Node;

public class DriftFXSurfaceHelper extends NodeHelper
{
    private static final DriftFXSurfaceHelper theInstance;
    private static DriftFXSurfaceAcessor accessor;

    static
    {
        theInstance = new DriftFXSurfaceHelper();
        Utils.forceInit(DriftFXSurface.class);
    }

    private static DriftFXSurfaceHelper getInstance()
    {
        return theInstance;
    }

    public static void initHelper(DriftFXSurface surface)
    {
        setHelper(surface, getInstance());
    }

    @Override
    protected NGNode createPeerImpl(Node node)
    {
        return accessor.doCreatePeer(node);
    }

    @Override
    protected void updatePeerImpl(Node node)
    {
        super.updatePeerImpl(node);
        accessor.doUpdatePeer(node);
    }

    @Override
    protected BaseBounds computeGeomBoundsImpl(Node node, BaseBounds bounds, BaseTransform tx)
    {
        return accessor.doComputeGeomBounds(node, bounds, tx);
    }

    @Override
    protected boolean computeContainsImpl(Node node, double localX, double localY)
    {
        return accessor.doComputeContains(node, localX, localY);
    }

    public static void setDriftFXSurfaceAccessor(final DriftFXSurfaceAcessor newAccessor)
    {
        if (accessor != null)
        {
            throw new IllegalStateException();
        }

        accessor = newAccessor;
    }

    public interface DriftFXSurfaceAcessor
    {
        NGNode doCreatePeer(Node node);

        void doUpdatePeer(Node node);

        BaseBounds doComputeGeomBounds(Node node, BaseBounds bounds, BaseTransform tx);

        boolean doComputeContains(Node node, double localX, double localY);
    }
}

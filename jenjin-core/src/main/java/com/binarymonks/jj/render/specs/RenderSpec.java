package com.binarymonks.jj.render.specs;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.binarymonks.jj.assets.AssetReference;
import com.binarymonks.jj.backend.Global;
import com.binarymonks.jj.physics.specs.PhysicsNodeSpec;
import com.binarymonks.jj.render.nodes.RenderNode;
import com.binarymonks.jj.specs.SpecPropField;
import com.binarymonks.jj.utils.Empty;

public abstract class RenderSpec<CONCRETE extends RenderSpec> {
    public int id = Global.renderWorld.nextRenderID();
    public int layer;
    public int thingPriority;
    public GraphSpec<CONCRETE> renderGraph = new GraphSpec<CONCRETE>((CONCRETE) this);
    public SpecPropField<Color, CONCRETE> color = new SpecPropField<>((CONCRETE) this, Color.WHITE);
    CONCRETE self;

    public RenderSpec() {
        self = (CONCRETE) this;
    }

    public CONCRETE setLayer(int layer) {
        this.layer = layer;
        return self;
    }

    public CONCRETE setPriority(int priority) {
        this.thingPriority = priority;
        return self;
    }

    public abstract RenderNode<?> makeNode(PhysicsNodeSpec physicsNodeSpec);

    public Array<AssetReference> getAssets() {
        return Empty.Array();
    }

    public static class Null extends RenderSpec<Null> {


        @Override
        public RenderNode<?> makeNode(PhysicsNodeSpec physicsNodeSpec) {
            return RenderNode.NULL;
        }

    }

}

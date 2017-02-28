package com.binarymonks.jj.demo.d05;

import com.binarymonks.jj.Game;
import com.binarymonks.jj.JJ;
import com.binarymonks.jj.JJConfig;
import com.binarymonks.jj.layers.GameRenderingLayer;
import com.binarymonks.jj.specs.SceneSpec;
import com.binarymonks.jj.specs.render.AnimationSequence;
import com.binarymonks.jj.specs.render.BackingTexture;
import com.binarymonks.jj.specs.render.RenderBuilder;
import com.binarymonks.jj.things.InstanceParams;
import com.binarymonks.jj.specs.ThingSpec;

public class D05_animations extends Game {
    float WORLD_WIDTH = 100;
    float WORLD_HEIGHT = 100;

    public D05_animations(JJConfig jjconfig) {
        super(jjconfig);
    }

    GameRenderingLayer gameRenderingLayer;

    @Override
    protected void gameOn() {
        JJ.specs.set("twinkle_animated", twinkleAnimated());

        SceneSpec scene = new SceneSpec();
        scene.addInstance("twinkle_animated", InstanceParams.New().setPosition(WORLD_WIDTH * 0.5f, WORLD_HEIGHT * 0.5f));


        JJ.things.load(scene, this::gameLoaded);
    }

    private void gameLoaded() {

    }

    private ThingSpec twinkleAnimated() {
        ThingSpec spec = new ThingSpec();
        spec.newNode()
                .setRender(
                        RenderBuilder.animated(new BackingTexture.Simple("textures/count.png"),
                                3, 4,
                                50, 50)
                                .addAnimation(new AnimationSequence()
                                        .setDuration(6f)
                                        .setName("default")
                                        .setStartEnd(4, 8)
                                ).build());
        return spec;
    }
}

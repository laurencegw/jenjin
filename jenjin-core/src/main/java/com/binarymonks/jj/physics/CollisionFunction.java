package com.binarymonks.jj.physics;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.Array;
import com.binarymonks.jj.specs.PropField;
import com.binarymonks.jj.things.Thing;
import com.binarymonks.jj.things.ThingNode;
import com.binarymonks.jj.utils.Reflection;

import java.lang.reflect.Field;

public abstract class CollisionFunction {

    private CollisionResolver resolver;
    protected Array<String> ignoreProperties = new Array<>();
    protected Array<String> matchProperties = new Array<>();
    private boolean enabled = true;

    public void performCollision(Thing me, Fixture myFixture,
                                 Thing other, Fixture otherFixture, Contact contact) {
        if (enabled) {
            ThingNode gameData = (ThingNode) otherFixture.getUserData();
            for (String ignore : ignoreProperties) {
                if (gameData.hasProperty(ignore)) {
                    return;
                }
            }
            if (matchProperties.size > 0) {
                for (String matchProp : matchProperties) {
                    if (gameData.hasProperty(matchProp)) {
                        collision(me, myFixture, other, otherFixture, contact);
                        break;
                    }
                }
            } else {
                collision(me, myFixture, other, otherFixture, contact);
            }
        }
    }

    public abstract void collision(Thing me, Fixture myFixture, Thing other, Fixture otherFixture, Contact contact);

    public abstract CollisionFunction clone();

    public void copyProperties(CollisionFunction copyFrom) {
        this.matchProperties.addAll(copyFrom.matchProperties);
        this.ignoreProperties.addAll(copyFrom.ignoreProperties);
    }

    public void setResolver(CollisionResolver resolver) {
        this.resolver = resolver;
        for (Field field : this.getClass().getFields()) {
            if (PropField.class.isAssignableFrom(field.getType())) {
                PropField pf = Reflection.getFieldFromInstance(field, this);
                pf.setParent(resolver.getSelf());
            }
        }
    }

    public void disable() {
        enabled = false;
    }

    public void enable() {
        enabled = true;
    }
}
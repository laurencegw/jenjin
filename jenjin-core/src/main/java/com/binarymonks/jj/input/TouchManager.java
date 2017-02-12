package com.binarymonks.jj.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.binarymonks.jj.backend.Global;
import com.binarymonks.jj.pools.N;
import com.binarymonks.jj.pools.Poolable;
import com.binarymonks.jj.pools.Re;
import com.binarymonks.jj.things.Thing;
import com.binarymonks.jj.things.ThingNode;

public class TouchManager implements InputProcessor {

    OrthographicCamera camera;
    ObjectMap<Integer, Thing> dragableThings = new ObjectMap<>();
    ObjectMap<Integer, Touch> touchTracker = new ObjectMap<>(10);
    Array<Integer> touchRemovals = new Array<>(10);


    Vector3 testPoint = N.ew(Vector3.class);
    Vector2 testPoint2 = N.ew(Vector2.class);
    Vector2 touchOffset = N.ew(Vector2.class);
    ObjectSet<Fixture> possibleBodies = new ObjectSet<>();
    Thing touchedThing = null;

    public TouchManager(OrthographicCamera camera) {
        this.camera=camera;
    }

    public void addMouseDrag(Thing thing) {
        dragableThings.put(thing.id, thing);
    }


    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        unproject(x, y);
        checkForDragableThings();
        if (touchedThing != null) {
            touchTracker.put(pointer, N.ew(Touch.class).set(touchedThing, touchOffset));
            touchedThing = null;
        }
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (touchTracker.containsKey(pointer)) {
            Touch t = touchTracker.remove(pointer);
            Re.cycle(t);
        }
        return true;
    }

    public void update() {
        for (ObjectMap.Entry<Integer, Touch> touch : touchTracker) {
            if (touch.value.touchedThing.isMarkedForDestruction()) {
                touchRemovals.add(touch.key);
                continue;
            }
            unproject(Gdx.input.getX(touch.key), Gdx.input.getY(touch.key));
            Touch trackedTouch = touchTracker.get(touch.key);
            if (!testPoint2.equals(trackedTouch.testPointCache)) {
                touch.value.move(testPoint2);
            }
        }
        for (Integer touch : touchRemovals) {
            Touch t = touchTracker.remove(touch);
            Re.cycle(t);
        }
        touchRemovals.clear();
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }

    protected void unproject(int x, int y) {
        camera.unproject(testPoint.set(x, y, 0));
        testPoint2.set(testPoint.x, testPoint.y);
    }

    private void checkForDragableThings() {
        Global.physics.world.QueryAABB(this::reportFixture, testPoint.x - 0.3f, testPoint.y - 0.3f, testPoint.x + 0.3f, testPoint.y + 0.3f);
        selectDragable();
    }

    private void selectDragable() {
        for (Fixture fixture : possibleBodies) {
            ThingNode node = (ThingNode) fixture.getUserData();
            if (node != null) {
                Thing parent = node.parent;
                if (!parent.isMarkedForDestruction()) {
                    if (dragableThings.containsKey(parent.id)) {
                        touchedThing = parent;
                        Body hitBody = fixture.getBody();
                        Vector2 bodyPosition = N.ew(Vector2.class).set(hitBody.getPosition());
                        touchOffset.set(bodyPosition.sub(testPoint.x, testPoint.y));
                        Re.cycle(bodyPosition);
                        break;
                    }
                }
            }
        }
        possibleBodies.clear();
    }

    public boolean reportFixture(Fixture fixture) {
        possibleBodies.add(fixture);
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    public static class Touch implements Poolable {
        Thing touchedThing;
        Vector2 testPointCache = N.ew(Vector2.class);
        Vector2 touchOffset = N.ew(Vector2.class);

        public Touch set(Thing touchedThing, Vector2 offset) {
            this.touchedThing = touchedThing;
            this.touchOffset.set(offset);
            return this;
        }


        @Override
        public void reset() {
            testPointCache.set(0, 0);
            touchOffset.set(0, 0);
            touchedThing = null;
        }

        public void move(Vector2 newTouchLocation) {
            testPointCache.set(newTouchLocation);
            Vector2 newPosition = N.ew(Vector2.class).set(newTouchLocation).add(touchOffset);
            touchedThing.physicsroot.setPosition(newPosition.x, newPosition.y);
            Re.cycle(newPosition);
        }
    }


}

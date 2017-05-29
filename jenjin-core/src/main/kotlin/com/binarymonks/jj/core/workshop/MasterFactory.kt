package com.binarymonks.jj.core.workshop

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.badlogic.gdx.utils.Array
import com.binarymonks.jj.core.JJ
import com.binarymonks.jj.core.audio.SoundEffects
import com.binarymonks.jj.core.audio.SoundParams
import com.binarymonks.jj.core.extensions.copy
import com.binarymonks.jj.core.physics.PhysicsNode
import com.binarymonks.jj.core.physics.PhysicsRoot
import com.binarymonks.jj.core.pools.new
import com.binarymonks.jj.core.render.RenderRoot
import com.binarymonks.jj.core.render.nodes.RenderNode
import com.binarymonks.jj.core.specs.*
import com.binarymonks.jj.core.specs.physics.*
import com.binarymonks.jj.core.specs.render.RenderSpec
import com.binarymonks.jj.core.things.Thing

class MasterFactory {

    private var paramsStackCache: Array<ParamStack> = Array()

    fun createScene(scene: SceneSpec, params: InstanceParams): Thing? {
        var paramsStack = paramsStack()
        paramsStack.add(params)
        var myThing: Thing? = createSceneHelper(scene, paramsStack)
        returnParamsStack(paramsStack)
        return myThing
    }

    private fun createSceneHelper(
            scene: SceneSpec,
            paramsStack: ParamStack): Thing? {

        val myThing = createThing(scene.thingSpec, paramsStack)

        for (entry in scene.nodes) {
            val nodeScene = entry.value.resolve()
            val nodeParams = entry.value.params
            paramsStack.add(nodeParams)
            createSceneHelper(nodeScene, paramsStack)
            paramsStack.pop()
        }
        return myThing
    }

    private fun createThing(thingSpec: ThingSpec?, paramsStack: ParamStack): Thing? {
        if (thingSpec == null) return null
        val thing = Thing(
                paramsStack.peek().uniqueInstanceName,
                physicsRoot = buildPhysicsRoot(thingSpec.physics, paramsStack),
                renderRoot = buildRenderRoot(thingSpec.render, paramsStack),
                soundEffects = buildSoundEffects(thingSpec.sounds),
                properties = paramsStack.peek().properties.copy()
        )
        addBehaviour(thing, thingSpec)

        JJ.B.renderWorld.addThing(thing)
        JJ.B.thingWorld.add(thing)

        return thing
    }

    private fun addBehaviour(thing: Thing, thingSpec: ThingSpec) {
        for(component in thingSpec.components){
            thing.addComponent(component.clone())
        }
    }

    private fun buildSoundEffects(sounds: Array<SoundParams>): SoundEffects {
        val soundEffects = SoundEffects()
        soundEffects.addSoundEffects(sounds)
        return soundEffects
    }

    private fun buildRenderRoot(renderSpec: RenderSpec, paramsStack: ParamStack): RenderRoot {
        val renderRoot: RenderRoot = RenderRoot(renderSpec.id)
        for (nodeSpec in renderSpec.renderNodes) {
            val node: RenderNode = nodeSpec.makeNode(paramsStack)
            renderRoot.addNode(nodeSpec.layer, node)
        }
        return renderRoot
    }

    private fun buildPhysicsRoot(physicsSpec: PhysicsSpec, paramsStack: ParamStack): PhysicsRoot {
        val def = BodyDef()

        var worldPosition = new(Vector2::class).mul(paramsStack.transformMatrix)

        def.position.set(worldPosition.x, worldPosition.y)
        def.angle = paramsStack.rotationD * MathUtils.degreesToRadians
        def.type = physicsSpec.bodyType
        def.fixedRotation = physicsSpec.fixedRotation
        def.linearDamping = physicsSpec.linearDamping
        def.angularDamping = physicsSpec.angularDamping
        def.bullet = physicsSpec.bullet
        def.allowSleep = physicsSpec.allowSleep
        def.gravityScale = physicsSpec.gravityScale
        val body = JJ.B.physicsWorld.b2dworld.createBody(def)

        val physicsRoot = PhysicsRoot(body)

        for (ibegin in physicsSpec.beginCollisions) {
            physicsRoot.collisionResolver.addInitialBegin(ibegin.clone())
        }
        for (fbegin in physicsSpec.finalBeginCollisions) {
            physicsRoot.collisionResolver.addFinalBegin(fbegin.clone())
        }
        for (end in physicsSpec.endCollisions) {
            physicsRoot.collisionResolver.addInitialBegin(end.clone())
        }

        for (fixtureSpec in physicsSpec.fixtures) {
            buildFixture(physicsRoot, fixtureSpec, body, paramsStack)
        }

        return physicsRoot

    }

    private fun buildFixture(physicsRoot: PhysicsRoot, fixtureSpec: FixtureSpec, body: Body, params: ParamStack) {
        val shape = buildShape(fixtureSpec, params.scaleX, params.scaleY)
        val fDef = FixtureDef()
        fDef.shape = shape
        fDef.density = fixtureSpec.density
        fDef.friction = fixtureSpec.friction
        fDef.restitution = fixtureSpec.restitution
        fDef.isSensor = fixtureSpec.isSensor
        val cd = fixtureSpec.collisionGroup.toCollisionData(checkNotNull(params.peek()).properties)
        fDef.filter.categoryBits = cd.category
        fDef.filter.maskBits = cd.mask
        val fixture = body.createFixture(fDef)
        val physicsNode = PhysicsNode(fixture, physicsRoot)
        fixture.userData = physicsNode
        shape!!.dispose()
    }

    private fun buildShape(nodeSpec: FixtureSpec, scaleX: Float, scaleY: Float): Shape? {
        if (nodeSpec.shape is Rectangle) {
            val polygonRectangle = nodeSpec.shape as Rectangle
            val boxshape = PolygonShape()
            boxshape.setAsBox(polygonRectangle.width * scaleX / 2.0f, polygonRectangle.height * scaleY / 2.0f, new(Vector2::class.java).set(nodeSpec.offsetX * scaleX, nodeSpec.offsetY * scaleY), nodeSpec.rotationD * MathUtils.degreesToRadians)
            return boxshape
        } else if (nodeSpec.shape is Circle) {
            val circle = nodeSpec.shape as Circle
            val circleShape = CircleShape()
            circleShape.radius = circle.radius * scaleX
            circleShape.position = new(Vector2::class).set(nodeSpec.offsetX, nodeSpec.offsetY)
            return circleShape
        } else if (nodeSpec.shape is Polygon) {
            val polygonSpec = nodeSpec.shape as Polygon
            val polygonShape = PolygonShape()
            val vertices = arrayOfNulls<Vector2>(polygonSpec.vertices.size)
            for (i in 0..polygonSpec.vertices.size - 1) {
                vertices[i] = polygonSpec.vertices.get(i).copy().scl(scaleX, scaleY)
            }
            polygonShape.set(vertices)
            return polygonShape
        } else if (nodeSpec.shape is Chain) {
            val chainSpec = nodeSpec.shape as Chain
            val chainShape = ChainShape()
            val vertices = arrayOfNulls<Vector2>(chainSpec.vertices.size)
            for (i in 0..chainSpec.vertices.size - 1) {
                vertices[i] = chainSpec.vertices.get(i).add(nodeSpec.offsetX, nodeSpec.offsetY)
            }
            chainShape.createChain(vertices)
            return chainShape
        }
        return null
    }

    private fun paramsStack(): ParamStack {
        if (paramsStackCache.size > 0) {
            return paramsStackCache.pop()
        }
        return ParamStack()
    }

    private fun returnParamsStack(stack: ParamStack) {
        stack.clear()
        stack.rotationD = 0f
        stack.x = 0f
        stack.y = 0f
        stack.scaleX = 1f
        stack.scaleY = 1f
        stack.transformMatrix.idt()
        paramsStackCache.add(stack)
    }


}


class ParamStack : Array<InstanceParams>() {

    var rotationD = 0f
    var x = 0f
    var y = 0f
    var scaleX = 1f
    var scaleY = 1f
    var transformMatrix: Matrix3 = new(Matrix3::class)

    override fun add(params: InstanceParams) {
        super.add(params)
        rotationD += params.rotationD
        x += params.x
        y += params.y
        scaleX *= params.scaleX
        scaleY *= params.scaleY
        transformMatrix.mul(params.getTransformMatrix())
    }

    override fun pop(): InstanceParams? {
        var removing = super.pop()
        if (removing != null) {
            rotationD -= removing.rotationD
            x -= removing.x
            y -= removing.y
            scaleX /= removing.scaleX
            scaleY /= removing.scaleY
            transformMatrix.mul(removing.getTransformMatrix().inv())
        }
        return removing
    }
}
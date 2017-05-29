package com.binarymonks.jj.core.render.nodes

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.PolygonRegion
import com.badlogic.gdx.graphics.g2d.PolygonSprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.EarClippingTriangulator
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.binarymonks.jj.core.JJ
import com.binarymonks.jj.core.pools.new
import com.binarymonks.jj.core.pools.recycle
import com.binarymonks.jj.core.properties.PropOverride
import com.binarymonks.jj.core.specs.InstanceParams
import com.binarymonks.jj.core.specs.render.RenderNodeSpec

class PolygonRenderNode constructor(
        priority: Int,
        color: PropOverride<Color>,
        internal var poly: PolygonSprite,
        internal var scaleX: Float = 1f,
        internal var scaleY: Float = 1f
) : RenderNode(priority, color) {

    override fun render(camera: OrthographicCamera) {
        JJ.B.renderWorld.switchToBatch()
        val parentPos = myParent().physicsRoot.position()
        poly.color = color.get()
        poly.setOrigin(0f, 0f)
        poly.setScale(scaleX, scaleY)
        poly.rotation = parent!!.physicsRoot.rotationR() * MathUtils.radDeg
        poly.setPosition(parentPos.x, parentPos.y)
        poly.draw(JJ.B.renderWorld.polyBatch)
    }

    override fun dispose() {}

    companion object {

        private val spriteByNodeIDCache: ObjectMap<Int, PolygonSprite> = ObjectMap()
        internal var triangulator = EarClippingTriangulator()

        fun haveBuilt(renderNodeSpec: RenderNodeSpec): Boolean {
            return spriteByNodeIDCache.containsKey(renderNodeSpec.id)
        }

        fun rebuild(renderNodeSpec: RenderNodeSpec, instanceParams: InstanceParams): PolygonRenderNode {
            val polygonSprite: PolygonSprite = spriteByNodeIDCache.get(renderNodeSpec.id)
            return PolygonRenderNode(renderNodeSpec.priority, renderNodeSpec.color.copy(), polygonSprite, instanceParams.scaleX, instanceParams.scaleY)
        }

        fun buildNew(renderNodeSpec: RenderNodeSpec, vertices: Array<Vector2>, offset: Vector2, rotationD: Float, scaleX: Float, scaleY: Float): PolygonRenderNode {
            val trMatrix = new(Matrix3::class.java)
            trMatrix.translate(offset.x, offset.y)
            trMatrix.rotate(rotationD)
            for (vertex in vertices) {
                vertex.mul(trMatrix)
            }
            val polygonSprite: PolygonSprite = polygonSprite(vertices)
            spriteByNodeIDCache.put(renderNodeSpec.id, polygonSprite)
            recycle(trMatrix)
            return PolygonRenderNode(renderNodeSpec.priority, renderNodeSpec.color.copy(), polygonSprite, scaleX, scaleY)
        }

        fun polygonSprite(points: Array<Vector2>): PolygonSprite {
            val pix = Pixmap(1, 1, Pixmap.Format.RGBA8888)
            pix.setColor(0xFFFFFFFF.toInt())
            pix.fill()
            val textureSolid = Texture(pix)
            val vertices = FloatArray(points.size * 2)
            val triangleIndices = triangulator.computeTriangles(vertices)
            for (i in 0..points.size - 1) {
                val point = points.get(i)
                val offset = i * 2
                vertices[offset] = point.x
                vertices[offset + 1] = point.y
            }
            val polyReg = PolygonRegion(TextureRegion(textureSolid),
                    vertices, triangleIndices.toArray())
            val poly = PolygonSprite(polyReg)
            return poly
        }
    }

}

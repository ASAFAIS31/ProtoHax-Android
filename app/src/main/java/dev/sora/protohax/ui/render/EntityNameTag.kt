package dev.sora.protohax.ui.render


import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import dev.sora.protohax.relay.modules.ModuleNameTags
import dev.sora.protohax.util.AnimationUtils
import dev.sora.relay.game.entity.EntityPlayer
import dev.sora.relay.utils.timing.MillisecondTimer
import org.cloudburstmc.math.matrix.Matrix4f
import java.util.regex.Pattern


class EntityNameTag {


    private val minXTimer = MillisecondTimer()

    private var minXFloat = 0.0f

    private val minYTimer = MillisecondTimer()

    private var minYFloat = 0.0f

    private val maxXTimer = MillisecondTimer()

    private var maxXFloat = 0.0f

    private val maxYTimer = MillisecondTimer()

    private var maxYFloat = 0.0f

    fun draw(

		entity: EntityPlayer,

		viewProjMatrix: Matrix4f,

		screenWidth: Int,

		screenHeight: Int,

		canvas: Canvas,

		m: ModuleNameTags

    ) {

        if (entity.username.isEmpty()) return

        var minX = (entity.posX - 0.3).toDouble()

        val minZ = (entity.posZ - 0.3).toDouble()

        var maxX = (entity.posX + 0.3).toDouble()

        val maxZ = (entity.posZ + 0.3).toDouble()

        var minY = (entity.posY).toDouble()

        var maxY = (entity.posY + 1).toDouble()

        val boxVertices = if (entity is EntityPlayer) {

            minY -= 1.62

            maxY -= 0.82

            arrayOf(

                doubleArrayOf(minX, minY, minZ),

                doubleArrayOf(minX, maxY, minZ),

                doubleArrayOf(maxX, maxY, minZ),

                doubleArrayOf(maxX, minY, minZ),

                doubleArrayOf(minX, minY, maxZ),

                doubleArrayOf(minX, maxY, maxZ),

                doubleArrayOf(maxX, maxY, maxZ),

                doubleArrayOf(maxX, minY, maxZ)

            )

        } else {

            arrayOf(

                doubleArrayOf(minX, minY, minZ),

                doubleArrayOf(minX, maxY, minZ),

                doubleArrayOf(maxX, maxY, minZ),

                doubleArrayOf(maxX, minY, minZ),

                doubleArrayOf(minX, minY, maxZ),

                doubleArrayOf(minX, maxY, maxZ),

                doubleArrayOf(maxX, maxY, maxZ),

                doubleArrayOf(maxX, minY, maxZ)

            )

        }




        minX = screenWidth.toDouble()

        minY = screenHeight.toDouble()

        maxX = .0

        maxY = .0

        for (boxVertex in boxVertices) {

            val screenPos = m.worldToScreen(

                boxVertex[0],

                boxVertex[1],

                boxVertex[2],

                viewProjMatrix,

                screenWidth,

                screenHeight

            ) ?: continue

            minX = screenPos.x.coerceAtMost(minX)

            minY = screenPos.y.coerceAtMost(minY)

            maxX = screenPos.x.coerceAtLeast(maxX)

            maxY = screenPos.y.coerceAtLeast(maxY)

        }

        if (this.minXTimer.hasTimePassed(15L)) {

            this.minXFloat = AnimationUtils.animate(

                minX,

                this.minXFloat.toDouble(), 0.2

            ).toFloat()

            this.minXTimer.reset();

        }

        if (this.minYTimer.hasTimePassed(15L)) {

            this.minYFloat = AnimationUtils.animate(

                minY,

                this.minYFloat.toDouble(), 0.2

            ).toFloat()

            this.minXTimer.reset();

        }

        if (this.maxXTimer.hasTimePassed(15L)) {

            this.maxXFloat = AnimationUtils.animate(

                maxX,

                this.maxXFloat.toDouble(), 0.2

            ).toFloat()

            this.maxXTimer.reset();

        }

        if (this.maxYTimer.hasTimePassed(15L)) {

            this.maxYFloat = AnimationUtils.animate(

                maxY,

                this.maxYFloat.toDouble(), 0.2

            ).toFloat()

            this.maxYTimer.reset();

        }




        if (!(minX >= screenWidth || minY >= screenHeight || maxX <= 0 || maxY <= 0)) {

            val width = this.maxXFloat - this.minXFloat

            val posX = this.minXFloat + (width / 2)

            font.textSize = 18f

            font.textAlign = Paint.Align.CENTER

            font.color = Color.argb(255, 255, 255, 255)

            buttonBackground.color = Color.argb(180, 0, 0, 0)

            buttonBackground.style = Paint.Style.FILL

            buttonBackground.maskFilter = BlurMaskFilter(25f, BlurMaskFilter.Blur.NORMAL)

            canvas.drawRoundRect(

                posX - (font.measureText(stripControlCodes(entity.username)) / 2) - 10,

                this.minYFloat - 45,

                posX + (font.measureText(stripControlCodes(entity.username)) / 2) + 10,

                this.minYFloat - 10, 8f, 8f,

                buttonBackground

            )

            buttonBackground.maskFilter = null

            canvas.drawRoundRect(

                posX - (font.measureText(stripControlCodes(entity.username)) / 2) - 10,

                this.minYFloat - 45,

                posX + (font.measureText(stripControlCodes(entity.username)) / 2) + 10,

                this.minYFloat - 10, 8f, 8f,

                buttonBackground

            )

            canvas.drawText(

                stripControlCodes(entity.username)!!,

                posX - 0, this.minYFloat - 20f, font

            )

        }


    }

    private val patternControlCode: Pattern = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]")


    private fun stripControlCodes(p_76338_0_: String?): String? {

        return p_76338_0_?.let { patternControlCode.matcher(it).replaceAll("") }

    }

    private val font = Paint()

    private val buttonBackground = Paint()

}

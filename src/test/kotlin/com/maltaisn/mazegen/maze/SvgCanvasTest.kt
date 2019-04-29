package com.maltaisn.mazegen.maze

import com.maltaisn.mazegen.render.SvgCanvas
import org.junit.jupiter.api.Test
import java.io.File

internal class SvgCanvasTest {

    @Test
    fun arc() {
        val canvas = SvgCanvas()
        canvas.init(100.0, 100.0)
        canvas.optimization = SvgCanvas.OPTIMIZATION_PATHS

        canvas.drawLine(0.0, 0.0, 10.0, 0.0)
        canvas.drawLine(10.0, 0.0, 20.0, 0.0)
        canvas.drawLine(100.0, 0.0, 100.0, 0.0)
        canvas.drawLine(200.0, 0.0, 210.0, 0.0)

        canvas.exportTo(File("testMazes/testSvg.svg"))
    }

}

package com.grsds.personal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 应用内统一图标集。
 *
 * 官方 APP 底部导航用 24dp 实心图标（选中蓝、未选中深炭色，细节用白色镂空/白描），
 * 页面内部的次级图标才是线性风格。这里按同一套规格自绘，
 * 避免用「⌂ ✓ ▤」这类字符或单个汉字充当图标——字符的字形、粗细、基线都不可控，
 * 一放大就和系统字体绑死，观感和原生图标差得很远。
 */
enum class Glyph {
    Home, HomeFilled, Todo, TodoFilled, Services, ServicesFilled,
    Message, MessageFilled, Profile, Smiley, SmileyFilled,
    Bell, BellFilled, Flame, Chart, Receipt, Printer,
    TileForm, TileWallet, TileSeal,
    Card, DocCheck, Stamp, IdCard, People,
    Briefcase, Pencil, Headset, Grid, Gear,
    Check, ChevronRight, ChevronDown, Search, Scan,
    Calendar, Close, Clock, Shield
}

@Composable
fun GlyphIcon(
    kind: Glyph,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp,
    strokeWidth: Dp = 1.8.dp
) {
    Canvas(modifier.size(size)) {
        drawGlyph(kind, tint, strokeWidth.toPx())
    }
}

fun DrawScope.drawGlyph(kind: Glyph, color: Color, strokeWidth: Float) {
    val u = size.minDimension / 24f
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    fun ln(x1: Float, y1: Float, x2: Float, y2: Float) =
        drawLine(color, p(x1, y1), p(x2, y2), strokeWidth, StrokeCap.Round)
    fun linePath(vararg pts: Float) {
        val path = Path()
        var i = 0
        while (i + 3 < pts.size) {
            path.moveTo(pts[i] * u, pts[i + 1] * u)
            path.lineTo(pts[i + 2] * u, pts[i + 3] * u)
            i += 4
        }
        drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    fun outline(block: Path.() -> Unit) {
        drawPath(Path().apply(block), color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    fun solid(block: Path.() -> Unit) {
        drawPath(Path().apply(block), color, style = Fill)
    }
    fun rect(x: Float, y: Float, w: Float, h: Float, r: Float, solidShape: Boolean = false) {
        drawRoundRect(
            color = color,
            topLeft = p(x, y),
            size = Size(w * u, h * u),
            cornerRadius = CornerRadius(r * u, r * u),
            style = if (solidShape) Fill else Stroke(strokeWidth)
        )
    }
    fun dot(x: Float, y: Float, r: Float) = drawCircle(color, r * u, p(x, y))

    /**
     * 实心图标内部的负形细节统一用白色描：底栏恒为白底，
     * 视觉上等价于把细节从实心形状里镂空出来，比拼 EvenOdd 路径更好维护。
     */
    fun wln(x1: Float, y1: Float, x2: Float, y2: Float) =
        drawLine(Color.White, p(x1, y1), p(x2, y2), strokeWidth, StrokeCap.Round)

    fun wdot(x: Float, y: Float, r: Float) = drawCircle(Color.White, r * u, p(x, y))

    fun whiteStroke(block: Path.() -> Unit) {
        drawPath(
            Path().apply(block),
            Color.White,
            style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    when (kind) {
        /** 官方底栏「首页」：圆角五边形房子 + 拱形门，不是尖顶三角屋。 */
        Glyph.Home -> {
            outline {
                moveTo(3.2f * u, 20.2f * u); lineTo(3.2f * u, 10.8f * u)
                cubicTo(3.2f * u, 10f * u, 3.5f * u, 9.4f * u, 4.1f * u, 8.9f * u)
                lineTo(10.7f * u, 3.5f * u)
                cubicTo(11.5f * u, 2.9f * u, 12.5f * u, 2.9f * u, 13.3f * u, 3.5f * u)
                lineTo(19.9f * u, 8.9f * u)
                cubicTo(20.5f * u, 9.4f * u, 20.8f * u, 10f * u, 20.8f * u, 10.8f * u)
                lineTo(20.8f * u, 20.2f * u); close()
            }
            outline {
                moveTo(9.5f * u, 20.2f * u); lineTo(9.5f * u, 16.8f * u)
                cubicTo(9.5f * u, 15.4f * u, 10.6f * u, 14.6f * u, 12f * u, 14.6f * u)
                cubicTo(13.4f * u, 14.6f * u, 14.5f * u, 15.4f * u, 14.5f * u, 16.8f * u)
                lineTo(14.5f * u, 20.2f * u)
            }
        }

        /** 选中态：实心房子，拱形门洞用奇偶填充抠掉，透出底栏的白底。 */
        Glyph.HomeFilled -> solid {
            fillType = PathFillType.EvenOdd
            moveTo(3.2f * u, 20.4f * u); lineTo(3.2f * u, 10.8f * u)
            cubicTo(3.2f * u, 10f * u, 3.5f * u, 9.4f * u, 4.1f * u, 8.9f * u)
            lineTo(10.7f * u, 3.5f * u)
            cubicTo(11.5f * u, 2.9f * u, 12.5f * u, 2.9f * u, 13.3f * u, 3.5f * u)
            lineTo(19.9f * u, 8.9f * u)
            cubicTo(20.5f * u, 9.4f * u, 20.8f * u, 10f * u, 20.8f * u, 10.8f * u)
            lineTo(20.8f * u, 20.4f * u); close()
            moveTo(9.5f * u, 20.4f * u); lineTo(9.5f * u, 16.8f * u)
            cubicTo(9.5f * u, 15.4f * u, 10.6f * u, 14.6f * u, 12f * u, 14.6f * u)
            cubicTo(13.4f * u, 14.6f * u, 14.5f * u, 15.4f * u, 14.5f * u, 16.8f * u)
            lineTo(14.5f * u, 20.4f * u); close()
        }

        /** 官方「待办」是带卡齿的日历 + 对勾，不是写字板。 */
        Glyph.Todo -> {
            rect(3.8f, 5.2f, 16.4f, 15.2f, 4f)
            ln(7.6f, 3.4f, 7.6f, 6.6f)
            ln(12f, 3.4f, 12f, 6.6f)
            ln(16.4f, 3.4f, 16.4f, 6.6f)
            outline {
                moveTo(8.6f * u, 13.6f * u); lineTo(11f * u, 16f * u); lineTo(15.4f * u, 11.2f * u)
            }
        }

        /** 底栏选中态：实心日历 + 白色卡齿与白对勾（卡齿超出部分在白底上不可见，形成缺口）。 */
        Glyph.TodoFilled -> {
            rect(3.8f, 5.2f, 16.4f, 15.2f, 4f, solidShape = true)
            wln(7.6f, 3.8f, 7.6f, 6.8f)
            wln(12f, 3.8f, 12f, 6.8f)
            wln(16.4f, 3.8f, 16.4f, 6.8f)
            whiteStroke {
                moveTo(8.6f * u, 13.6f * u); lineTo(11f * u, 16f * u); lineTo(15.4f * u, 11.2f * u)
            }
        }

        /** 官方「办&查」：两条横线 + 空心斜笔头，不是放大镜。 */
        Glyph.Services -> {
            rect(3.4f, 3.6f, 17.2f, 16.8f, 4.4f)
            linePath(6.4f, 8.8f, 12.2f, 8.8f, 6.4f, 11.2f, 9.8f, 11.2f)
            rotate(degrees = 35f, pivot = p(15.2f, 14.4f)) {
                drawOval(
                    color,
                    topLeft = p(13.6f, 11.4f),
                    size = Size(3.2f * u, 6f * u),
                    style = Stroke(strokeWidth)
                )
            }
        }

        /** 底栏选中态：实心圆角方 + 白色横线与白色空心笔头。 */
        Glyph.ServicesFilled -> {
            rect(3.4f, 3.6f, 17.2f, 16.8f, 4.4f, solidShape = true)
            wln(6.4f, 8.8f, 12.2f, 8.8f)
            wln(6.4f, 11.2f, 9.8f, 11.2f)
            rotate(degrees = 35f, pivot = p(15.2f, 14.4f)) {
                drawOval(
                    Color.White,
                    topLeft = p(13.6f, 11.4f),
                    size = Size(3.2f * u, 6f * u),
                    style = Stroke(strokeWidth)
                )
            }
        }

        /** 官方「消息」：圆润气泡 + 左下尾巴 + 长短两条线。 */
        Glyph.Message -> {
            rect(3.8f, 4.6f, 16.4f, 13.4f, 5.4f)
            solid {
                moveTo(7.8f * u, 16.8f * u); lineTo(6.4f * u, 21.2f * u)
                lineTo(12.2f * u, 17f * u); close()
            }
            linePath(7.8f, 9.8f, 16.2f, 9.8f, 7.8f, 12.8f, 12.6f, 12.8f)
        }

        /** 底栏选中态：实心气泡 + 两条白色文字线。 */
        Glyph.MessageFilled -> {
            rect(3.8f, 4.6f, 16.4f, 13.4f, 5.4f, solidShape = true)
            solid {
                moveTo(7.8f * u, 16.8f * u); lineTo(6.4f * u, 21.2f * u)
                lineTo(12.2f * u, 17f * u); close()
            }
            wln(7.8f, 9.8f, 16.2f, 9.8f)
            wln(7.8f, 12.8f, 12.6f, 12.8f)
        }

        Glyph.Profile -> {
            drawCircle(color, 3.5f * u, p(12f, 9.2f), style = Stroke(strokeWidth))
            outline {
                moveTo(4.8f * u, 20.6f * u)
                cubicTo(4.8f * u, 16.2f * u, 7.9f * u, 13.6f * u, 12f * u, 13.6f * u)
                cubicTo(16.1f * u, 13.6f * u, 19.2f * u, 16.2f * u, 19.2f * u, 20.6f * u)
            }
        }

        /** 官方底栏「我的」是一个只有笑口的圆脸，没有眼睛。 */
        Glyph.Smiley -> {
            drawCircle(color, 8.4f * u, p(12f, 12f), style = Stroke(strokeWidth))
            outline {
                moveTo(8.6f * u, 13.6f * u)
                cubicTo(9.4f * u, 15.9f * u, 10.6f * u, 16.7f * u, 12f * u, 16.7f * u)
                cubicTo(13.4f * u, 16.7f * u, 14.6f * u, 15.9f * u, 15.4f * u, 13.6f * u)
            }
        }

        /** 底栏选中态：实心圆脸 + 白色笑口。 */
        Glyph.SmileyFilled -> {
            drawCircle(color, 8.6f * u, p(12f, 12f))
            whiteStroke {
                moveTo(8.6f * u, 13.6f * u)
                cubicTo(9.4f * u, 15.9f * u, 10.6f * u, 16.7f * u, 12f * u, 16.7f * u)
                cubicTo(13.4f * u, 16.7f * u, 14.6f * u, 15.9f * u, 15.4f * u, 13.6f * u)
            }
        }

        Glyph.Bell -> {
            outline {
                moveTo(5.5f * u, 17.4f * u); lineTo(5.5f * u, 11.8f * u)
                cubicTo(5.5f * u, 8.2f * u, 8.4f * u, 5.4f * u, 12f * u, 5.4f * u)
                cubicTo(15.6f * u, 5.4f * u, 18.5f * u, 8.2f * u, 18.5f * u, 11.8f * u)
                lineTo(18.5f * u, 17.4f * u); lineTo(20.2f * u, 19f * u)
                lineTo(3.8f * u, 19f * u); close()
            }
            ln(10.2f, 21f, 13.8f, 21f)
        }

        /** 提醒条用的实心铃铛，与官方通知条一致（官方是实心橙色铃，不是描边）。 */
        Glyph.BellFilled -> {
            solid {
                moveTo(5.5f * u, 17.4f * u); lineTo(5.5f * u, 11.8f * u)
                cubicTo(5.5f * u, 8.2f * u, 8.4f * u, 5.4f * u, 12f * u, 5.4f * u)
                cubicTo(15.6f * u, 5.4f * u, 18.5f * u, 8.2f * u, 18.5f * u, 11.8f * u)
                lineTo(18.5f * u, 17.4f * u); lineTo(20.2f * u, 19f * u)
                lineTo(3.8f * u, 19f * u); close()
            }
            ln(10.2f, 21f, 13.8f, 21f)
        }

        Glyph.Flame -> {
            solid {
                moveTo(12f * u, 2.6f * u)
                cubicTo(12f * u, 2.6f * u, 6.8f * u, 8.2f * u, 6.8f * u, 13.2f * u)
                cubicTo(6.8f * u, 16.9f * u, 9.2f * u, 19.4f * u, 12f * u, 19.4f * u)
                cubicTo(14.8f * u, 19.4f * u, 17.2f * u, 16.9f * u, 17.2f * u, 13.2f * u)
                cubicTo(17.2f * u, 10.4f * u, 15.6f * u, 8.1f * u, 14.2f * u, 6.5f * u)
                cubicTo(13.9f * u, 8.6f * u, 13f * u, 9.8f * u, 12.2f * u, 10.3f * u)
                cubicTo(12.7f * u, 7.5f * u, 12.6f * u, 4.7f * u, 12f * u, 2.6f * u)
                close()
            }
        }

        Glyph.Chart -> {
            rect(3.4f, 3.4f, 17.2f, 17.2f, 3.4f)
            ln(8.2f, 17f, 8.2f, 13f)
            ln(12f, 17f, 12f, 9.2f)
            ln(15.8f, 17f, 15.8f, 11.4f)
        }

        Glyph.Receipt -> {
            outline {
                moveTo(6f * u, 3.4f * u); lineTo(18f * u, 3.4f * u); lineTo(18f * u, 20.6f * u)
                lineTo(15.6f * u, 19f * u); lineTo(13.2f * u, 20.6f * u); lineTo(10.8f * u, 19f * u)
                lineTo(8.4f * u, 20.6f * u); lineTo(6f * u, 19f * u); close()
            }
            linePath(9f, 8f, 15f, 8f, 9f, 11.6f, 15f, 11.6f, 9f, 15.2f, 13f, 15.2f)
        }

        Glyph.Printer -> {
            rect(7f, 3f, 10f, 5f, 1.4f)
            rect(3.4f, 7.8f, 17.2f, 8.4f, 2.2f)
            rect(7f, 13.4f, 10f, 7.2f, 1.2f)
            dot(17f, 10.8f, 0.9f)
        }

        Glyph.Card -> {
            rect(2.6f, 5.4f, 18.8f, 13.2f, 2.8f)
            ln(2.8f, 9.8f, 21.2f, 9.8f)
            ln(6.2f, 14.6f, 10.6f, 14.6f)
        }

        Glyph.DocCheck -> {
            outline {
                moveTo(6f * u, 3.4f * u); lineTo(14.4f * u, 3.4f * u); lineTo(18.4f * u, 7.4f * u)
                lineTo(18.4f * u, 20.6f * u); lineTo(6f * u, 20.6f * u); close()
            }
            linePath(14.4f, 3.4f, 14.4f, 7.4f, 18.4f, 7.4f)
            linePath(8.8f, 13.8f, 11f, 16f, 15.2f, 11.4f)
        }

        Glyph.Stamp -> {
            drawCircle(color, 4.8f * u, p(12f, 9.8f), style = Stroke(strokeWidth))
            linePath(10.2f, 7.6f, 10.2f, 12f, 13.8f, 7.6f, 13.8f, 12f)
            ln(6.6f, 18f, 17.4f, 18f)
        }

        Glyph.IdCard -> {
            rect(2.6f, 5.4f, 18.8f, 13.2f, 2.6f)
            drawCircle(color, 2f * u, p(8.2f, 10.6f), style = Stroke(strokeWidth))
            linePath(12.6f, 9.6f, 17f, 9.6f, 12.6f, 13f, 16f, 13f)
        }

        Glyph.People -> {
            drawCircle(color, 2.9f * u, p(8.6f, 9.4f), style = Stroke(strokeWidth))
            outline {
                moveTo(3.6f * u, 19.6f * u)
                cubicTo(3.6f * u, 16f * u, 5.8f * u, 13.8f * u, 8.6f * u, 13.8f * u)
                cubicTo(11.4f * u, 13.8f * u, 13.6f * u, 16f * u, 13.6f * u, 19.6f * u)
            }
            drawCircle(color, 2.2f * u, p(16.8f, 10.6f), style = Stroke(strokeWidth))
            outline {
                moveTo(14.6f * u, 19.6f * u)
                cubicTo(14.9f * u, 16.6f * u, 16.2f * u, 15.2f * u, 17.8f * u, 15.2f * u)
                cubicTo(19.2f * u, 15.2f * u, 20.2f * u, 16.2f * u, 20.4f * u, 17.8f * u)
            }
        }

        Glyph.Briefcase -> {
            rect(3f, 8f, 18f, 11.6f, 2.4f)
            outline {
                moveTo(9.2f * u, 8f * u); lineTo(9.2f * u, 6.2f * u)
                cubicTo(9.2f * u, 5.4f * u, 9.8f * u, 4.8f * u, 10.6f * u, 4.8f * u)
                lineTo(13.4f * u, 4.8f * u)
                cubicTo(14.2f * u, 4.8f * u, 14.8f * u, 5.4f * u, 14.8f * u, 6.2f * u)
                lineTo(14.8f * u, 8f * u)
            }
            ln(3.2f, 13.2f, 20.8f, 13.2f)
        }

        Glyph.Pencil -> {
            outline {
                moveTo(4f * u, 20f * u); lineTo(5.2f * u, 15.2f * u); lineTo(15.6f * u, 4.8f * u)
                lineTo(19.2f * u, 8.4f * u); lineTo(8.8f * u, 18.8f * u); close()
            }
            ln(13.6f, 6.8f, 17.2f, 10.4f)
        }

        Glyph.Headset -> {
            outline {
                moveTo(4f * u, 14.6f * u)
                cubicTo(4f * u, 9f * u, 7.6f * u, 4.8f * u, 12f * u, 4.8f * u)
                cubicTo(16.4f * u, 4.8f * u, 20f * u, 9f * u, 20f * u, 14.6f * u)
            }
            rect(3f, 13.4f, 3.8f, 5.2f, 1.6f)
            rect(17.2f, 13.4f, 3.8f, 5.2f, 1.6f)
        }

        Glyph.Grid -> {
            rect(3.8f, 3.8f, 7f, 7f, 2f)
            rect(13.2f, 3.8f, 7f, 7f, 2f)
            rect(3.8f, 13.2f, 7f, 7f, 2f)
            rect(13.2f, 13.2f, 7f, 7f, 2f)
        }

        /** 官方宫格「综合所得年度汇算」：白色卡片 + 加号 + 两行明细。 */
        Glyph.TileForm -> {
            rect(5.8f, 4.4f, 12.4f, 5.6f, 1.8f, solidShape = true)
            ln(6.4f, 13.4f, 9.8f, 13.4f)
            ln(8.1f, 11.7f, 8.1f, 15.1f)
            linePath(11.6f, 12.8f, 18.6f, 12.8f, 11.6f, 15.8f, 18.6f, 15.8f)
        }

        /** 官方宫格「收入纳税明细」：白色钱包 + ¥ 符号。 */
        Glyph.TileWallet -> {
            rect(3.8f, 5.4f, 16.4f, 13.2f, 2.8f)
            ln(3.8f, 9f, 20.2f, 9f)
            linePath(10.2f, 10.8f, 12f, 12.9f, 13.8f, 10.8f)
            ln(12f, 12.9f, 12f, 16.4f)
            ln(10.2f, 14.1f, 13.8f, 14.1f)
            ln(10.2f, 15.6f, 13.8f, 15.6f)
        }

        /** 官方宫格「纳税记录开具」：白色卡片 + 印章。 */
        Glyph.TileSeal -> {
            rect(5.2f, 4.2f, 13.6f, 6.2f, 1.8f, solidShape = true)
            solid {
                moveTo(9.9f * u, 11.4f * u); lineTo(14.1f * u, 11.4f * u)
                lineTo(14.8f * u, 15.2f * u); lineTo(9.2f * u, 15.2f * u); close()
            }
            ln(6.6f, 17.6f, 17.4f, 17.6f)
        }

        /** 设置用齿轮：细圆环 + 8 个外凸短齿，齿只长在环外，小尺寸下才不会糊成太阳。 */
        Glyph.Gear -> {
            drawCircle(color, 5.4f * u, p(12f, 12f), style = Stroke(strokeWidth * 0.9f))
            drawCircle(color, 2.2f * u, p(12f, 12f), style = Stroke(strokeWidth * 0.9f))
            for (i in 0 until 8) {
                rotate(degrees = i * 45f, pivot = p(12f, 12f)) {
                    drawRoundRect(
                        color = color,
                        topLeft = p(10.7f, 3.8f),
                        size = Size(2.6f * u, 3.2f * u),
                        cornerRadius = CornerRadius(0.9f * u, 0.9f * u)
                    )
                }
            }
        }

        Glyph.Check -> outline {
            moveTo(5.2f * u, 12.4f * u); lineTo(9.6f * u, 17f * u); lineTo(19f * u, 7.4f * u)
        }

        Glyph.ChevronRight -> outline {
            moveTo(9.4f * u, 5.6f * u); lineTo(15.8f * u, 12f * u); lineTo(9.4f * u, 18.4f * u)
        }

        Glyph.ChevronDown -> outline {
            moveTo(6.2f * u, 9.6f * u); lineTo(12f * u, 15.6f * u); lineTo(17.8f * u, 9.6f * u)
        }

        Glyph.Search -> {
            drawCircle(color, 6f * u, p(10.4f, 10.4f), style = Stroke(strokeWidth))
            ln(14.8f, 14.8f, 20f, 20f)
        }

        Glyph.Scan -> {
            val edge = 3f
            val arm = 5f
            ln(edge, edge + arm, edge, edge)
            ln(edge, edge, edge + arm, edge)
            ln(21f - arm, edge, 21f, edge)
            ln(21f, edge, 21f, edge + arm)
            ln(edge, 21f - arm, edge, 21f)
            ln(edge, 21f, edge + arm, 21f)
            ln(21f - arm, 21f, 21f, 21f)
            ln(21f, 21f, 21f, 21f - arm)
            ln(8.8f, 12f, 15.2f, 12f)
        }

        Glyph.Calendar -> {
            rect(3.4f, 5f, 17.2f, 15.6f, 2.6f)
            ln(3.6f, 9.6f, 20.4f, 9.6f)
            ln(8f, 3f, 8f, 6.2f)
            ln(16f, 3f, 16f, 6.2f)
        }

        Glyph.Close -> {
            ln(6.2f, 6.2f, 17.8f, 17.8f)
            ln(17.8f, 6.2f, 6.2f, 17.8f)
        }

        Glyph.Clock -> {
            drawCircle(color, 8.2f * u, p(12f, 12f), style = Stroke(strokeWidth))
            linePath(12f, 7.4f, 12f, 12.2f, 15.4f, 14f)
        }

        Glyph.Shield -> outline {
            moveTo(12f * u, 3f * u); lineTo(19.6f * u, 5.9f * u); lineTo(19.6f * u, 11.8f * u)
            cubicTo(19.6f * u, 16.4f * u, 16.4f * u, 19.8f * u, 12f * u, 21f * u)
            cubicTo(7.6f * u, 19.8f * u, 4.4f * u, 16.4f * u, 4.4f * u, 11.8f * u)
            lineTo(4.4f * u, 5.9f * u); close()
        }
    }
}

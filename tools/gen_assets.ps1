# Generates launcher icon assets (and the splash cover) for grsds_personal.
# Style reference: app/sampledata/cover1.jpg -- blue shield outline with the tax glyph, on white.
# Windows only (uses System.Drawing / GDI+).  Run:  powershell -ExecutionPolicy Bypass -File tools\gen_assets.ps1
# All text is fed in as code points so this file stays pure ASCII and survives any encoding.

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$res = Join-Path $PSScriptRoot '..\app\src\main\res'
$res = (Resolve-Path $res).Path
$tmp = Join-Path $env:TEMP 'grsds_icons'
New-Item -ItemType Directory -Force -Path $tmp | Out-Null

$TAX   = [string][char]0x7A0E                                                    # tax glyph
$TITLE = [string][char]0x4E2A + [char]0x4EBA + [char]0x6240 + [char]0x5F97 + [char]0x7A0E  # title
$SUB   = 'Individual Income Tax'

$blueLight = [System.Drawing.Color]::FromArgb(255, 0x3D, 0x8B, 0xFF)
$blueDeep  = [System.Drawing.Color]::FromArgb(255, 0x1B, 0x63, 0xD6)
$blueFlat  = [System.Drawing.Color]::FromArgb(255, 0x2C, 0x7B, 0xF2)
$subBlue   = [System.Drawing.Color]::FromArgb(255, 0x8A, 0xB2, 0xEC)
$white     = [System.Drawing.Color]::White
$hairline  = [System.Drawing.Color]::FromArgb(255, 0xE6, 0xEC, 0xF6)

function Get-ShieldPath([single]$cx, [single]$w, [single]$h, [single]$topY) {
    $left = $cx - $w / 2
    $right = $cx + $w / 2
    $bottom = $topY + $h
    $r = $w * 0.155
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $p.StartFigure()
    $p.AddLine([single]($left + $r), $topY, [single]($right - $r), $topY)
    $p.AddArc([single]($right - 2 * $r), $topY, [single](2 * $r), [single](2 * $r), 270, 90)
    $p.AddBezier(
        [single]$right, [single]($topY + $r),
        [single]$right, [single]($topY + $h * 0.40),
        [single]($right - $w * 0.10), [single]($bottom - $h * 0.16),
        [single]$cx, [single]$bottom)
    $p.AddBezier(
        [single]$cx, [single]$bottom,
        [single]($left + $w * 0.10), [single]($bottom - $h * 0.16),
        [single]$left, [single]($topY + $h * 0.40),
        [single]$left, [single]($topY + $r))
    $p.AddArc([single]$left, $topY, [single](2 * $r), [single](2 * $r), 180, 90)
    $p.CloseFigure()
    return $p
}

# Glyph as an outline path, scaled so its tight bounding box is $targetH tall and centred on ($cx,$cy).
function Get-GlyphPath([string]$text, [single]$targetH, [single]$cx, [single]$cy) {
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $fam = New-Object System.Drawing.FontFamily('Microsoft YaHei')
    $p.AddString($text, $fam, [int][System.Drawing.FontStyle]::Bold, 200.0,
        (New-Object System.Drawing.PointF(0, 0)), [System.Drawing.StringFormat]::GenericTypographic)
    $b = $p.GetBounds()
    $scale = $targetH / $b.Height
    $m = New-Object System.Drawing.Drawing2D.Matrix
    $m.Translate(-($b.X + $b.Width / 2), -($b.Y + $b.Height / 2))
    $m.Scale($scale, $scale, [System.Drawing.Drawing2D.MatrixOrder]::Append)
    $m.Translate($cx, $cy, [System.Drawing.Drawing2D.MatrixOrder]::Append)
    $p.Transform($m)
    return $p
}

function New-Canvas([int]$w, [int]$h) {
    $bmp = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    return @($bmp, $g)
}

function Get-BluePen([single]$width) {
    $pen = New-Object System.Drawing.Pen($blueFlat, $width)
    $pen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
    $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    return $pen
}

function Get-BlueBrush([single]$x, [single]$y, [single]$w, [single]$h) {
    $rect = New-Object System.Drawing.RectangleF($x, $y, $w, $h)
    return New-Object System.Drawing.Drawing2D.LinearGradientBrush($rect, $blueLight, $blueDeep, 90.0)
}

# Draws shield + glyph centred on ($cx,$cy), sized to $h.  Stroke width scales with the art.
# Pass $flatColor to draw the whole mark in one ink (used by the monochrome layer).
function Draw-Mark($g, [single]$cx, [single]$cy, [single]$h, [double]$strokeRatio, $brush, $flatColor) {
    $w = $h * 0.79
    $strokeW = $w * $strokeRatio
    $topY = $cy - $h / 2
    if ($null -eq $flatColor) {
        $pen = Get-BluePen $strokeW
    }
    else {
        $pen = New-Object System.Drawing.Pen($flatColor, $strokeW)
        $pen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
        $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
        $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    }
    $shield = Get-ShieldPath $cx $w $h $topY
    $g.DrawPath($pen, $shield)
    $glyphH = $w * 0.545
    $glyph = Get-GlyphPath $TAX $glyphH $cx ($topY + $h * 0.44)
    $g.FillPath($brush, $glyph)
    $shield.Dispose(); $glyph.Dispose(); $pen.Dispose()
}

# ---------- master renders (1024px) ----------
$S = 1024

# 1) square legacy icon: white rounded square, hairline border, blue mark
$master = Join-Path $tmp 'square_1024.png'
$c = New-Canvas $S $S; $bmp = $c[0]; $g = $c[1]
$r = $S * 0.20
$rounded = New-Object System.Drawing.Drawing2D.GraphicsPath
$rounded.AddArc(0, 0, [single]($r * 2), [single]($r * 2), 180, 90)
$rounded.AddArc([single]($S - $r * 2), 0, [single]($r * 2), [single]($r * 2), 270, 90)
$rounded.AddArc([single]($S - $r * 2), [single]($S - $r * 2), [single]($r * 2), [single]($r * 2), 0, 90)
$rounded.AddArc(0, [single]($S - $r * 2), [single]($r * 2), [single]($r * 2), 90, 90)
$rounded.CloseFigure()
$g.FillPath((New-Object System.Drawing.SolidBrush($white)), $rounded)
$g.DrawPath((New-Object System.Drawing.Pen($hairline, 3.0)), $rounded)
Draw-Mark $g ($S / 2) ($S / 2) 720 0.085 (Get-BlueBrush 0 0 $S $S)
$bmp.Save($master, [System.Drawing.Imaging.ImageFormat]::Png); $g.Dispose(); $bmp.Dispose()

# 2) round legacy icon
$masterRound = Join-Path $tmp 'round_1024.png'
$c = New-Canvas $S $S; $bmp = $c[0]; $g = $c[1]
$g.FillEllipse((New-Object System.Drawing.SolidBrush($white)), 0, 0, $S, $S)
Draw-Mark $g ($S / 2) ($S / 2) 660 0.085 (Get-BlueBrush 0 0 $S $S)
$bmp.Save($masterRound, [System.Drawing.Imaging.ImageFormat]::Png); $g.Dispose(); $bmp.Dispose()

# 3) adaptive foreground (transparent, art inside the 66% safe zone)
$masterFg = Join-Path $tmp 'fg_1024.png'
$c = New-Canvas $S $S; $bmp = $c[0]; $g = $c[1]
Draw-Mark $g ($S / 2) ($S / 2) 462 0.085 (Get-BlueBrush 0 0 $S $S)
$bmp.Save($masterFg, [System.Drawing.Imaging.ImageFormat]::Png); $g.Dispose(); $bmp.Dispose()

# 4) monochrome layer (alpha shape only, themed by the system)
$masterMono = Join-Path $tmp 'mono_1024.png'
$c = New-Canvas $S $S; $bmp = $c[0]; $g = $c[1]
$ink = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::Black)
Draw-Mark $g ($S / 2) ($S / 2) 462 0.085 $ink ([System.Drawing.Color]::Black)
$bmp.Save($masterMono, [System.Drawing.Imaging.ImageFormat]::Png); $g.Dispose(); $bmp.Dispose()

# ---------- density export ----------
function Export-Scaled([string]$src, [string]$dir, [string]$name, [int]$px, [int]$canvasPx) {
    $img = [System.Drawing.Image]::FromFile($src)
    $bmp = New-Object System.Drawing.Bitmap($px, $px, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.DrawImage($img, (New-Object System.Drawing.Rectangle(0, 0, $px, $px)))
    $g.Dispose()
    $target = Join-Path $dir ($name + '.png')
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $bmp.Save($target, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose(); $img.Dispose()
    Write-Host ("  {0,-46} {1,4}x{1}" -f ($target.Replace($res, '')), $px)
}

$densities = @(
    @{ d = 'mdpi'; launcher = 48; adaptive = 108 },
    @{ d = 'hdpi'; launcher = 72; adaptive = 162 },
    @{ d = 'xhdpi'; launcher = 96; adaptive = 216 },
    @{ d = 'xxhdpi'; launcher = 144; adaptive = 324 },
    @{ d = 'xxxhdpi'; launcher = 192; adaptive = 432 }
)

Write-Host 'writing mipmaps:'
foreach ($item in $densities) {
    $dir = Join-Path $res ('mipmap-' + $item.d)
    Export-Scaled $master      $dir 'ic_launcher'            $item.launcher 0
    Export-Scaled $masterRound $dir 'ic_launcher_round'      $item.launcher 0
    Export-Scaled $masterFg    $dir 'ic_launcher_foreground' $item.adaptive 0
    Export-Scaled $masterMono  $dir 'ic_launcher_monochrome' $item.adaptive 0
}

# ---------- splash cover (1080x2400) ----------
$W = 1080; $H = 2400
$c = New-Canvas $W $H; $bmp = $c[0]; $g = $c[1]
$g.Clear($white)

# faint scattered shields, like the original splash
$spots = @(
    @(150, 300, 86), @(392, 210, 68), @(700, 250, 62), @(905, 330, 78),
    @(160, 620, 72), @(880, 640, 86), @(300, 880, 60), @(760, 940, 68),
    @(180, 1180, 62), @(900, 1210, 66), @(520, 1290, 58), @(420, 470, 56)
)
$faintPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(13, 0x2C, 0x7B, 0xF2), 3.0)
$faintBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(9, 0x2C, 0x7B, 0xF2))
foreach ($s in $spots) {
    $sh = Get-ShieldPath ([single]$s[0]) ([single]($s[2] * 0.86)) ([single]$s[2]) ([single]($s[1] - $s[2] / 2))
    $g.DrawPath($faintPen, $sh)
    $g.FillPath($faintBrush, $sh)
    $sh.Dispose()
}

$markCy = 820.0
Draw-Mark $g 540 $markCy 268 0.085 (Get-BlueBrush 0 ($markCy - 134) $W 268)

$titleFont = New-Object System.Drawing.Font('Microsoft YaHei', 74.0, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$subFont = New-Object System.Drawing.Font('Microsoft YaHei', 34.0, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$center = New-Object System.Drawing.StringFormat
$center.Alignment = [System.Drawing.StringAlignment]::Center
$center.LineAlignment = [System.Drawing.StringAlignment]::Center
$titleBrush = New-Object System.Drawing.SolidBrush($blueFlat)
$subBrush = New-Object System.Drawing.SolidBrush($subBlue)
$g.DrawString($TITLE, $titleFont, $titleBrush, (New-Object System.Drawing.RectangleF(0, 1555, $W, 110)), $center)
$g.DrawString($SUB, $subFont, $subBrush, (New-Object System.Drawing.RectangleF(0, 1760, $W, 60)), $center)

$coverPath = Join-Path $res 'drawable-nodpi\cover.png'
New-Item -ItemType Directory -Force -Path (Split-Path $coverPath) | Out-Null
$bmp.Save($coverPath, [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()
Write-Host ("  {0,-46} {1}x{2}" -f '\drawable-nodpi\cover.png', $W, $H)

Write-Host 'done.'

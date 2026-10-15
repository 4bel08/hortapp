# Script to regenerate Android Adaptive Icons with proper Google Play Safe Zone padding
Add-Type -AssemblyName System.Drawing

function Get-ContentBoundingBox($bmp) {
    $minX = $bmp.Width; $maxX = 0; $minY = $bmp.Height; $maxY = 0
    $hasContent = $false
    for ($y = 0; $y -lt $bmp.Height; $y++) {
        for ($x = 0; $x -lt $bmp.Width; $x++) {
            $pixel = $bmp.GetPixel($x, $y)
            if ($pixel.A -gt 15) {
                $hasContent = $true
                if ($x -lt $minX) { $minX = $x }
                if ($x -gt $maxX) { $maxX = $x }
                if ($y -lt $minY) { $minY = $y }
                if ($y -gt $maxY) { $maxY = $y }
            }
        }
    }
    if (-not $hasContent) {
        return [System.Drawing.Rectangle]::FromLTRB(0, 0, $bmp.Width, $bmp.Height)
    }
    return [System.Drawing.Rectangle]::FromLTRB($minX, $minY, $maxX + 1, $maxY + 1)
}

function Create-PaddedForeground($srcImgPath, $destPath, $targetSize, $contentPercent) {
    $src = [System.Drawing.Bitmap]::FromFile($srcImgPath)
    $bbox = Get-ContentBoundingBox $src
    
    $dest = New-Object System.Drawing.Bitmap($targetSize, $targetSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($dest)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    $maxTargetDim = $targetSize * $contentPercent
    $scale = [math]::Min($maxTargetDim / $bbox.Width, $maxTargetDim / $bbox.Height)
    $drawW = [int]($bbox.Width * $scale)
    $drawH = [int]($bbox.Height * $scale)
    $drawX = [int](($targetSize - $drawW) / 2)
    $drawY = [int](($targetSize - $drawH) / 2)

    $destRect = New-Object System.Drawing.Rectangle($drawX, $drawY, $drawW, $drawH)
    $g.DrawImage($src, $destRect, $bbox, [System.Drawing.GraphicsUnit]::Pixel)

    $dest.Save($destPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $dest.Dispose()
    $src.Dispose()
}

function Create-LegacyIcon($srcImgPath, $destPath, $targetSize, $isRound) {
    $src = [System.Drawing.Bitmap]::FromFile($srcImgPath)
    $bbox = Get-ContentBoundingBox $src
    
    $dest = New-Object System.Drawing.Bitmap($targetSize, $targetSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($dest)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    $bgColor = [System.Drawing.Color]::FromArgb(255, 255, 248, 245)
    $brush = New-Object System.Drawing.SolidBrush($bgColor)
    if ($isRound) {
        $g.FillEllipse($brush, 0, 0, $targetSize - 1, $targetSize - 1)
    } else {
        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        $radius = [int]($targetSize * 0.2)
        $rect = New-Object System.Drawing.Rectangle(0, 0, $targetSize, $targetSize)
        $path.AddArc($rect.X, $rect.Y, $radius, $radius, 180, 90)
        $path.AddArc($rect.Right - $radius, $rect.Y, $radius, $radius, 270, 90)
        $path.AddArc($rect.Right - $radius, $rect.Bottom - $radius, $radius, $radius, 0, 90)
        $path.AddArc($rect.X, $rect.Bottom - $radius, $radius, $radius, 90, 90)
        $path.CloseFigure()
        $g.FillPath($brush, $path)
        $path.Dispose()
    }
    $brush.Dispose()

    $maxTargetDim = $targetSize * 0.65
    $scale = [math]::Min($maxTargetDim / $bbox.Width, $maxTargetDim / $bbox.Height)
    $drawW = [int]($bbox.Width * $scale)
    $drawH = [int]($bbox.Height * $scale)
    $drawX = [int](($targetSize - $drawW) / 2)
    $drawY = [int](($targetSize - $drawH) / 2)

    $destRect = New-Object System.Drawing.Rectangle($drawX, $drawY, $drawW, $drawH)
    $g.DrawImage($src, $destRect, $bbox, [System.Drawing.GraphicsUnit]::Pixel)

    $dest.Save($destPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $dest.Dispose()
    $src.Dispose()
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$baseRes = Join-Path $scriptDir "app\src\main\res"
$srcColor = Join-Path $projectRoot "icon-512.png"
$srcMono = Join-Path $projectRoot "icon-512-mono.png"

$adaptiveDensities = @(
    @{ Folder = "mipmap-mdpi"; AdaptiveSize = 108; LegacySize = 48 },
    @{ Folder = "mipmap-hdpi"; AdaptiveSize = 162; LegacySize = 72 },
    @{ Folder = "mipmap-xhdpi"; AdaptiveSize = 216; LegacySize = 96 },
    @{ Folder = "mipmap-xxhdpi"; AdaptiveSize = 324; LegacySize = 144 },
    @{ Folder = "mipmap-xxxhdpi"; AdaptiveSize = 432; LegacySize = 192 }
)

foreach ($d in $adaptiveDensities) {
    $dir = Join-Path $baseRes $d.Folder
    Create-PaddedForeground $srcColor (Join-Path $dir "ic_launcher_foreground.png") $d.AdaptiveSize 0.52
    Create-LegacyIcon $srcColor (Join-Path $dir "ic_launcher.png") $d.LegacySize $false
    Create-LegacyIcon $srcColor (Join-Path $dir "ic_launcher_round.png") $d.LegacySize $true
}

Create-PaddedForeground $srcMono (Join-Path $baseRes "drawable\ic_launcher_monochrome.png") 432 0.52
Write-Output "Android adaptive and monochrome icons regenerated with 52% safe zone padding."

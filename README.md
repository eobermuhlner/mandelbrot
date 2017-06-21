# mandelbrot

Interactive Mandelbrot Viewer and command line tool to generate zoom sequences into the Mandelbrot set.

## Interactive Viewer

The interactive viewer allows user interaction using mouse, touch gestures or the keyboard.

It is possible to zoom interactively down to 10<sup>-100</sup> and beyond, all calculations switch automatically from double to BigDecimal if more precision is needed.

Rendering is progressive and optimized so that the application stays responsive, although the complete rendering of an image on zoom level 100 may surpass 1 hour.

The viewer allows to create snapshot image files which will be rendered in the background.

Multiple color palette variations are supported.

To start the interactive viewer use the following command line:
```bash
java -cp mandelbrot.jar ch.obermuhlner.mandelbrot.javafx.MandelbrotApp
```


![Screenshot Mandelbrot Viewer](ch.obermuhlner.mandelbrot.docs/screenshots/mandelbrot_viewer_screenshot1.png?raw=true "Screenshot Mandelbrot Viewer")

## Command Line Tool

The command line tool allows to automatically create many images for a specific point in the mandelbrot at increasing zoom levels.

The generated images can be used to create a video that zooms deeper and deeper into the specified point.

You may specify to render a point of interest (that are also available in the interactive viewer):
```bash
java -cp mandelbrot.jar ch.obermuhlner.mandelbrot.cli.MandelbrotZoom --poi Thorns
```
You may also specify the complete rendering information (the example shows the same point as the "Thorns" example above:
```bash
java -cp mandelbrot.jar ch.obermuhlner.mandelbrot.cli.MandelbrotZoom \
    --x 0.615688188277165136862977361674265969958593022307313876044710397223212241218305144722407409388125158236774855883651489995471305785441350335740253105778 \
    --y  0.674900407359391397989165449336345186641209056492297641703764886106334430140801874852392546319746961769590518919533419668508561716801971179771345638618 \
    --zoomStart 5 \
    --zoomStep 0.1 \
    --paletteSeed 1 \
    --paletteStep 10 \
    --imageCount 100 \
    --directoryName Thorns
```
This will create a folder containing 100 images starting with a radius of 5, incrementing the zoom level by 0.1 every image.

You can create a video from these images using the `ffmpeg` tool:
```bash
ffmpeg -y -r 10 -start_number 0 -i mandelbrot%04d.png -s 800x800 -vcodec mpeg4 -q:v 1 mandelbrot.mp4
```

## Video Gallery 

[![Video - Wheels on Fire](https://img.youtube.com/vi/p3Zv8fSEsSg/0.jpg)](https://www.youtube.com/watch?v=p3Zv8fSEsSg "Wheels on Fire")

## Gallery

![Mandelbrot Curved Swords at zoom 10^-4](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_curved_swords_zoom4.png?raw=true "Mandelbrot Curved Swords at zoom 10^-4")
![Mandelbrot Curved Swords at zoom 10^-9](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_curved_swords_zoom9.png?raw=true "Mandelbrot Curved Swords at zoom 10^-9")
![Mandelbrot Curved Swords at zoom 10^-10](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_curved_swords_zoom10.png?raw=true "Mandelbrot Curved Swords at zoom 10^-10")
![Mandelbrot Curved Swords at zoom 10^-13](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_curved_swords_zoom13.png?raw=true "Mandelbrot Curved Swords at zoom 10^-13")
![Mandelbrot Curved Swords at zoom 10^-15](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_curved_swords_zoom15.png?raw=true "Mandelbrot Curved Swords at zoom 10^-15")

![Mandelbrot Jelly Fish at zoom 10^-10](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_jelly_fish_zoom10.png?raw=true "Mandelbrot Jelly Fish at zoom 10^-10")

![Mandelbrot Chaotic Spirals at zoom 10^-9](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_chaotic_spirals_zoom9.png?raw=true "Mandelbrot Chaotic Spirals at zoom 10^-9")

![Mandelbrot Deep Zoom 1 at zoom 10^-21](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_deep1_zoom21.png?raw=true "Mandelbrot Deep Zoom 1 at zoom 10^-21")
![Mandelbrot Deep Zoom 1 at zoom 10^-45](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_deep1_zoom45.png?raw=true "Mandelbrot Deep Zoom 1 at zoom 10^-45")

## Gallery Palettes

The Mandelbrot viewer and Zoom application support different palette algorithms.

### Palette Random Colors
![Mandelbrot Palette Random Colors](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_snail_shell_zoom10.png?raw=true "Mandelbrot Palette Random Colors")

### Palette Random Gray
![Mandelbrot Palette Random Gray](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_snail_shell_zoom10_random_gray.png?raw=true "Mandelbrot Palette Random Gray")

### Palette Random Pastell
![Mandelbrot Palette Random Pastell](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_snail_shell_zoom10_random_pastell.png?raw=true "Mandelbrot Palette Random Pastell")

### Palette Fire
![Mandelbrot Palette Fire](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_snail_shell_zoom10_fire.png?raw=true "Mandelbrot Palette Fire")

### Palette Water
![Mandelbrot Palette Water](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_snail_shell_zoom10_water.png?raw=true "Mandelbrot Palette Water")

### Palette Earth
![Mandelbrot Palette Earth](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_snail_shell_zoom10_earth.png?raw=true "Mandelbrot Palette Earth")

### Palette Air
![Mandelbrot Palette Air](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_snail_shell_zoom10_air.png?raw=true "Mandelbrot Palette Air")

### Palette Forest
![Mandelbrot Palette Forest](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_snail_shell_zoom10_forest.png?raw=true "Mandelbrot Palette Forest")

### Palette Starry Night
![Mandelbrot Palette Starry Night](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_snail_shell_zoom10_starry_night.png?raw=true "Mandelbrot Palette Starry Night")

### Palette Drawing
![Mandelbrot Palette Drawing](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_snail_shell_zoom10_drawing.png?raw=true "Mandelbrot Palette Drawing")

### Palette Rainbow
![Mandelbrot Palette Rainbow](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_snail_shell_zoom10_rainbow.png?raw=true "Mandelbrot Palette Rainbow")


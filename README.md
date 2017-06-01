# mandelbrot

Interactive Mandelbrot Viewer and command line tool to generate zoom sequences into the Mandelbrot set.

## Interactive Viewer

The interactive viewer allows user interaction using mouse interaction, touch gestures or the keyboard.

It is possible to zoom interactively down to 10^-100, all calculations switch automatically from double to BigDecimal if more precision is needed.

Rendering is optimized so that the application stays responsive, although the complete rendering of an image on zoom level 100 may surpass 1 hour.

The interactive viewer allows to create snapshot image files which will be rendered in the background.

![Screenshot Mandelbrot Viewer](ch.obermuhlner.mandelbrot.docs/screenshots/mandelbrot_snail_shell.png?raw=true)

## Command Line Tool

The command line tool allows to automatically create many images for a specific point in the mandelbrot at increasing zoom levels.

The generated images can be used to create a video that zooms deeper and deeper into the specified point.

You may specify to render a point of interest (that are also available in the interactive viewer):
```shell
java -cp mandelbrot.jar ch.obermuhlner.mandelbrot.cli.MandelbrotZoom --poi Thorns
```
You may also specify the complete rendering information (the example shows the same point as the "Thorns" example above:
```shell
java -cp mandelbrot.jar ch.obermuhlner.mandelbrot.cli.MandelbrotZoom \
    --x 0.615688188277165136862977361674265969958593022307313876044710397223212241218305144722407409388125158236774855883651489995471305785441350335740253105778 \
    --y  0.674900407359391397989165449336345186641209056492297641703764886106334430140801874852392546319746961769590518919533419668508561716801971179771345638618 \
    --zoomStart 5 \
    --zoomStep 0.1 \
    --paletteSeed 1 \
    --paletteStep 10 \
    --imageCount 100 \
    --directoryName Thorns \
```
This will create a folder containing 100 images.

You can create a video from these images using the `ffmpeg` tool:
```shell
ffmpeg -y -r 10 -start_number 0 -i mandelbrot%04d.png -s 800x800 -vcodec mpeg4 -q:v 1 mandelbrot.mp4
```

## Gallery

![Mandelbrot Curved Swords at zoom 10^-4](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_curved_swords_zoom4.png?raw=true)
![Mandelbrot Curved Swords at zoom 10^-9](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_curved_swords_zoom9.png?raw=true)
![Mandelbrot Curved Swords at zoom 10^-10](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_curved_swords_zoom10.png?raw=true)
![Mandelbrot Curved Swords at zoom 10^-13](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_curved_swords_zoom13.png?raw=true)
![Mandelbrot Curved Swords at zoom 10^-15](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_curved_swords_zoom15.png?raw=true)

![Mandelbrot Jelly Fish at zoom 10^-10](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_jelly_fish_zoom10.png?raw=true)

![Mandelbrot Chaotic Spirals at zoom 10^-9](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_chaotic_spirals_zoom9.png?raw=true)

![Mandelbrot Deep Zoom 1 at zoom 10^-21](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_deep1_zoom21.png?raw=true)
![Mandelbrot Deep Zoom 1 at zoom 10^-45](ch.obermuhlner.mandelbrot.docs/images/mandelbrot_deep1_zoom45.png?raw=true)

#!/bin/sh

for p in RandomColor RandomGray RandomPastell Fire Water Air Earth Forest StarryNight Drawing Rainbow LogRandomColor LogRandomGray
do
	mandelbrot --name "palette_example_$p" --paletteType $p --width 800 --height 800 palette_example.mandelbrot
done


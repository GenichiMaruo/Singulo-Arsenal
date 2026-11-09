"""Simplify computed lensing images into broad smooth color regions.

No drawn replacement contours are used. Warm/cool color fields from the numerical
render determine every colored region. Blur suppresses tiny stars, then threshold
bands flatten the radiance into an icon palette. Final downsampling antialiases it.
"""
import numpy as np
from PIL import Image, ImageFilter


def stylize(path):
    source = Image.open(path).convert('RGB')
    rgb = np.asarray(source.filter(ImageFilter.GaussianBlur(4.5)), dtype=np.float32) / 255
    # The disk and jets are separated by their computed emission color.
    warm = np.clip(rgb[..., 0] - rgb[..., 2] * 0.35, 0, 1)
    cool = np.clip(rgb[..., 2] - rgb[..., 0] * 0.50, 0, 1)
    image = np.empty_like(rgb, dtype=np.uint8)
    image[:] = (13, 10, 23)
    warm_palette = [(99, 49, 32), (175, 89, 36), (234, 154, 57), (255, 207, 117), (255, 244, 204)]
    for threshold, color in zip((0.08, 0.13, 0.22, 0.38, 0.55), warm_palette):
        image[warm > threshold] = color
    for threshold, color in [(0.18, (27, 85, 110)), (0.29, (63, 162, 190)), (0.42, (178, 236, 237))]:
        image[(cool > threshold) & (rgb[..., 2] > rgb[..., 0] * 1.10)] = color
    return Image.fromarray(image)


// Author: Daniel Binyamin
// Date: 3/1/2026
// Rev: 01
// Notes: N/A

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.stream.IntStream;

public class ImageRecolorEngine {

	public static BufferedImage recolor(
			BufferedImage input,
			int rampSteps,
			int contrastPercent,
			int gammaPercent,
			int metallicPercent,
			int baseR,
			int baseG,
			int baseB) {

		int width = input.getWidth();
		int height = input.getHeight();

		BufferedImage working = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		working.getGraphics().drawImage(input, 0, 0, null);

		BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		int[] inPixels = ((DataBufferInt) working.getRaster().getDataBuffer()).getData();
		int[] outPixels = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();

		double contrast = contrastPercent / 100.0;
		double gamma = gammaPercent / 100.0;
		double metallic = metallicPercent / 100.0;

		// ---- Build 3-color ramp ----
		int[][] ramp = buildRamp(rampSteps, baseR, baseG, baseB);

		// ---- Compute luminance range (parallel) ----
		double[] minMax = computeLuminanceRange(inPixels);
		double minL = minMax[0];
		double maxL = minMax[1];

		if (minL >= maxL) {
			minL = 0;
			maxL = 1;
		}

		final double fMinL = minL;
		final double fMaxL = maxL;

		// ---- Multithreaded recolor ----
		IntStream.range(0, inPixels.length).parallel().forEach(i -> {

			int argb = inPixels[i];
			int alpha = (argb >>> 24) & 0xFF;

			if (alpha == 0) {
				outPixels[i] = argb;
				return;
			}

			int r = (argb >> 16) & 0xFF;
			int g = (argb >> 8) & 0xFF;
			int b = argb & 0xFF;

			double L = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0;

			double t = (L - fMinL) / (fMaxL - fMinL);
			t = clamp(t, 0.0, 1.0);

			// Contrast
			t = (t - 0.5) * contrast + 0.5;

			// Gamma
			t = Math.pow(clamp(t, 0.0, 1.0), gamma);

			// Metallic curve
			if (metallic > 0) {
				if (t < 0.5) {
					t = Math.pow(t, 1 - metallic * 0.5);
				} else {
					t = 1 - Math.pow(1 - t, 1 - metallic * 0.5);
				}
			}

			t = clamp(t, 0.0, 1.0);

			int idx = (int) Math.round(t * (rampSteps - 1));
			int[] rgb = ramp[idx];

			outPixels[i] =
					(alpha << 24) |
					(rgb[0] << 16) |
					(rgb[1] << 8) |
					rgb[2];
		});

		return output;
	}

	// ----- Build proper 3-color ramp -----
	private static int[][] buildRamp(int steps, int baseR, int baseG, int baseB) {

		int[] shadow = { baseR / 4, baseG / 4, baseB / 4 };
		int[] mid = { baseR, baseG, baseB };
		int[] highlight = {
				clamp((int)(baseR * 1.35), 0, 255),
				clamp((int)(baseG * 1.35), 0, 255),
				clamp((int)(baseB * 1.35), 0, 255)
		};

		int[][] ramp = new int[steps][3];

		for (int i = 0; i < steps; i++) {
			double t = i / (double)(steps - 1);

			int[] color;

			if (t <= 0.5) {
				double u = t * 2;
				color = lerp(shadow, mid, u);
			} else {
				double u = (t - 0.5) * 2;
				color = lerp(mid, highlight, u);
			}

			ramp[i] = color;
		}

		return ramp;
	}

	private static int[] lerp(int[] a, int[] b, double t) {
		return new int[] {
				clamp((int)(a[0] + (b[0] - a[0]) * t), 0, 255),
				clamp((int)(a[1] + (b[1] - a[1]) * t), 0, 255),
				clamp((int)(a[2] + (b[2] - a[2]) * t), 0, 255)
		};
	}

	// ----- Fast luminance range -----
	private static double[] computeLuminanceRange(int[] pixels) {

		double minL = 1.0;
		double maxL = 0.0;

		for (int argb : pixels) {
			int alpha = (argb >>> 24) & 0xFF;
			if (alpha == 0) continue;

			int r = (argb >> 16) & 0xFF;
			int g = (argb >> 8) & 0xFF;
			int b = argb & 0xFF;

			double L = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0;

			minL = Math.min(minL, L);
			maxL = Math.max(maxL, L);
		}

		return new double[]{minL, maxL};
	}

	private static double clamp(double v, double min, double max) {
		return Math.max(min, Math.min(max, v));
	}

	private static int clamp(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}

	// ----- Save helper -----
	public static void saveImage(BufferedImage img, String path) throws IOException {
		ImageIO.write(img, "png", new File(path));
	}
}
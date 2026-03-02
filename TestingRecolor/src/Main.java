
// Author: Daniel Binyamin
// Date: 3/1/2026
// Rev: 01
// Notes: N/A

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {

		Scanner kboard = new Scanner(System.in);
		int rampsteps, contrast, gamma, metallic, r, g, b; // variables for the values of the recoloration
    	
        // 🔹 Put your FULL image path here
		
		
		System.out.println("Input the texture's path:");
		String inputPath = kboard.nextLine().trim();

		// Convert Windows backslashes to forward slashes
		inputPath = inputPath.replace("\\", "/");

		// Remove accidental surrounding quotes (very common)
		if (inputPath.startsWith("\"") && inputPath.endsWith("\"")) {
		    inputPath = inputPath.substring(1, inputPath.length() - 1);
		}
		    
        System.out.println("You have entered your inputPath as" + " " + inputPath);
        
        System.out.println("Paste output path:");
        String outputPath = kboard.nextLine().trim();

        // Convert Windows backslashes to forward slashes
       outputPath = outputPath.replace("\\", "/");

        // Remove accidental surrounding quotes (very common)
        if (outputPath.startsWith("\"") && outputPath.endsWith("\"")) {
            outputPath = outputPath.substring(1, outputPath.length() - 1);
        }
        
        System.out.println("You have entered your outputPath as" + " " + outputPath);
        
        System.out.println("Please enter the number of rampsteps. (20 is normal)");
        rampsteps = kboard.nextInt();
        
        System.out.println("Please enter the contrast %. (100 is normal)");
        contrast = kboard.nextInt();
        
        System.out.println("Please enter the gamma %. (100 is normal)");
        gamma = kboard.nextInt();
        
        System.out.println("Please enter the luster. (0-100)");
        metallic = kboard.nextInt();
        
        System.out.println("Please enter the color in the order of Red, Green, Blue. (0-255)");
        r = kboard.nextInt();
        g = kboard.nextInt();
        b = kboard.nextInt();

        BufferedImage input = ImageIO.read(new File(inputPath));

        BufferedImage result = ImageRecolorEngine.recolor(
                input,
                rampsteps,     // ramp steps
                contrast,    // contrast (100 = normal)
                gamma,     // gamma (100 = normal)
                metallic,     // metallic (0–100)
                r,    // base R
                g,    // base G
                b      // base B
        );

        ImageRecolorEngine.saveImage(result, outputPath);

        System.out.println("Done! Saved to: " + outputPath);
    }
}
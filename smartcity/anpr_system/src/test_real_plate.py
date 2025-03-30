import cv2
import numpy as np
from plate_recognizer import PlateRecognizer
import os
import sys

def main():
    """Test the ANPR system with a real license plate image."""
    # Check if image path is provided
    if len(sys.argv) > 1:
        image_path = sys.argv[1]
    else:
        image_path = "data/test_images/AA03BOJ.png"
        if not os.path.exists(image_path):
            print(f"Error: Image file not found at {image_path}")
            print("Please provide path to image as command line argument")
            return
    
    # Load the image
    image = cv2.imread(image_path)
    if image is None:
        print(f"Error: Could not read image from {image_path}")
        return
        
    # Initialize plate recognizer
    recognizer = PlateRecognizer()
    
    print(f"Processing image: {image_path}")
    
    # Process the image
    plate_text, plate_image = recognizer.recognize_plate(image)
    
    if plate_text:
        print(f"Successfully recognized plate: {plate_text}")
        # Save the extracted plate image
        output_path = os.path.join(os.path.dirname(image_path), "extracted_plate.jpg")
        cv2.imwrite(output_path, plate_image)
        print(f"Extracted plate image saved to: {output_path}")
    else:
        print("Failed to recognize plate")
    
    # Display the results
    cv2.imshow("Original Image", image)
    if plate_image is not None:
        cv2.imshow("Extracted Plate", plate_image)
    cv2.waitKey(0)
    cv2.destroyAllWindows()

if __name__ == "__main__":
    main() 
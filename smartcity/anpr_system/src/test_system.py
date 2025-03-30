import cv2
import numpy as np
from plate_recognizer import PlateRecognizer
import os

def create_test_image():
    """Create a test image with a simulated license plate."""
    # Create a white background
    image = np.ones((400, 600, 3), dtype=np.uint8) * 255
    
    # Draw a rectangle for the plate
    cv2.rectangle(image, (100, 150), (500, 250), (0, 0, 0), 2)
    
    # Add some text
    cv2.putText(image, "AB12 CDE", (120, 220), 
                cv2.FONT_HERSHEY_SIMPLEX, 2, (0, 0, 0), 2)
    
    return image

def main():
    """Test the ANPR system with a simulated image."""
    # Create test image
    test_image = create_test_image()
    
    # Save the test image
    os.makedirs("data/test_images", exist_ok=True)
    cv2.imwrite("data/test_images/test_plate.jpg", test_image)
    
    # Initialize plate recognizer
    recognizer = PlateRecognizer()
    
    # Process the image
    plate_text, plate_image = recognizer.recognize_plate(test_image)
    
    if plate_text:
        print(f"Successfully recognized plate: {plate_text}")
        # Save the extracted plate image
        cv2.imwrite("data/test_images/extracted_plate.jpg", plate_image)
    else:
        print("Failed to recognize plate")
    
    # Display the results
    cv2.imshow("Test Image", test_image)
    if plate_image is not None:
        cv2.imshow("Extracted Plate", plate_image)
    cv2.waitKey(0)
    cv2.destroyAllWindows()

if __name__ == "__main__":
    main() 
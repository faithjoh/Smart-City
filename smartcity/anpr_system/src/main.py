import cv2
import os
from plate_recognizer import PlateRecognizer
from firebase_handler import FirebaseHandler
from typing import Optional, Tuple
import argparse

class ANPRSystem:
    """
    Main ANPR system class that integrates plate recognition and Firebase operations.
    """
    
    def __init__(self, firebase_credentials_path: str):
        """
        Initialize the ANPR system.
        
        Args:
            firebase_credentials_path (str): Path to Firebase service account credentials
        """
        self.plate_recognizer = PlateRecognizer()
        self.firebase_handler = FirebaseHandler(firebase_credentials_path)
        
    def process_image(self, image_path: str, location: Optional[str] = None) -> Tuple[bool, str]:
        """
        Process an image to recognize license plates and save results.
        
        Args:
            image_path (str): Path to the image file
            location (Optional[str]): Location where the image was captured
            
        Returns:
            Tuple[bool, str]: (Success status, Message)
        """
        try:
            # Read the image
            image = cv2.imread(image_path)
            if image is None:
                return False, f"Failed to read image: {image_path}"
                
            # Recognize plate
            plate_text, plate_image = self.plate_recognizer.recognize_plate(image)
            
            if plate_text is None:
                return False, "No license plate detected in the image"
                
            # Save recognition result to Firebase
            doc_id = self.firebase_handler.save_plate_recognition(
                plate_number=plate_text,
                image_path=image_path,
                location=location
            )
            
            return True, f"Successfully recognized plate: {plate_text} (Doc ID: {doc_id})"
            
        except Exception as e:
            return False, f"Error processing image: {str(e)}"
            
def main():
    """
    Main entry point for the ANPR system.
    """
    parser = argparse.ArgumentParser(description='ANPR System for License Plate Recognition')
    parser.add_argument('--image', required=True, help='Path to the image file')
    parser.add_argument('--location', help='Location where the image was captured')
    parser.add_argument('--credentials', required=True, help='Path to Firebase service account credentials')
    
    args = parser.parse_args()
    
    # Initialize ANPR system
    anpr = ANPRSystem(args.credentials)
    
    # Process the image
    success, message = anpr.process_image(args.image, args.location)
    
    print(message)
    return 0 if success else 1

if __name__ == "__main__":
    exit(main()) 
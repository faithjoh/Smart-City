#!/usr/bin/env python3
"""
ANPR System Demo Script
This script provides a simple demo of the ANPR functionality.
It allows testing the license plate recognition with example images.
"""

import os
import sys
import argparse
import cv2
from uk_plate_recognizer import UKPlateRecognizer

def parse_arguments():
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(description="ANPR System Demo")
    parser.add_argument("--image", "-i", type=str, help="Path to the image file")
    parser.add_argument("--test_dir", "-t", type=str, help="Path to a directory with test images")
    parser.add_argument("--show", "-s", action="store_true", help="Show processed images")
    return parser.parse_args()

def process_single_image(image_path, recognizer, show_image=False):
    """Process a single image and show the results."""
    if not os.path.exists(image_path):
        print(f"Error: Image file not found: {image_path}")
        return False
    
    print(f"\nProcessing image: {image_path}")
    
    # Read the image
    image = cv2.imread(image_path)
    if image is None:
        print(f"Error: Could not read image: {image_path}")
        return False
    
    # Process the image
    results = recognizer.process_image(image_path)
    
    # Display results
    print("\nRecognition Results:")
    if not results:
        print("No license plates detected.")
        return False
    
    for plate_id, data in results.items():
        print(f"Plate: {plate_id}")
        print(f"  - Plate Number: {data['plate_number']}")
        print(f"  - Country Identifier: {data['country_identifier']}")
    
    # Show the image if requested
    if show_image:
        # Create a copy of the image for visualization
        visualization = image.copy()
        
        # Draw results on the image
        font = cv2.FONT_HERSHEY_SIMPLEX
        for plate_id, data in results.items():
            # If there were plate regions detected
            if 'plate_region' in data:
                x, y, w, h = data['plate_region']
                cv2.rectangle(visualization, (x, y), (x + w, y + h), (0, 255, 0), 2)
                cv2.putText(visualization, data['plate_number'], (x, y - 10), 
                            font, 0.8, (0, 255, 0), 2)
                
        cv2.imshow("ANPR Demo", visualization)
        cv2.waitKey(0)
        cv2.destroyAllWindows()
    
    return True

def process_directory(directory_path, recognizer, show_image=False):
    """Process all images in a directory."""
    if not os.path.exists(directory_path):
        print(f"Error: Directory not found: {directory_path}")
        return
    
    print(f"Processing all images in directory: {directory_path}")
    
    # Get all image files in the directory
    image_extensions = ['.jpg', '.jpeg', '.png', '.bmp']
    image_files = [f for f in os.listdir(directory_path) if any(f.lower().endswith(ext) for ext in image_extensions)]
    
    if not image_files:
        print(f"No image files found in directory: {directory_path}")
        return
    
    print(f"Found {len(image_files)} image files.")
    
    success_count = 0
    for image_file in image_files:
        image_path = os.path.join(directory_path, image_file)
        success = process_single_image(image_path, recognizer, show_image)
        if success:
            success_count += 1
    
    print(f"\nSummary: Successfully processed {success_count} out of {len(image_files)} images.")

def main():
    """Main function to run the demo."""
    args = parse_arguments()
    
    # Initialize the recognizer
    recognizer = UKPlateRecognizer()
    
    # Check if we have either an image or a directory
    if args.image:
        process_single_image(args.image, recognizer, args.show)
    elif args.test_dir:
        process_directory(args.test_dir, recognizer, args.show)
    else:
        # If no specific input is provided, check if we have a sample data directory
        if os.path.exists("../data"):
            print("No input specified. Using sample images from '../data' directory.")
            process_directory("../data", recognizer, args.show)
        else:
            print("Error: No input specified. Please provide an image file or a directory.")
            print("Usage: python anpr_demo.py --image path/to/image.jpg")
            print("   or: python anpr_demo.py --test_dir path/to/test/images")
            return

if __name__ == "__main__":
    main() 
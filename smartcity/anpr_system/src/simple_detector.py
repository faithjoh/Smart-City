import cv2
import numpy as np
import pytesseract
import re
import sys
from PIL import Image

def detect_and_recognize_plate(image_path):
    """
    Simple approach to detect and recognize a UK license plate.
    """
    # Load the image
    image = cv2.imread(image_path)
    if image is None:
        print(f"Error loading image: {image_path}")
        return None, None
    
    # Show original image
    cv2.imshow("Original Image", image)
    
    # Convert to grayscale
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    
    # Apply Gaussian blur to reduce noise
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    
    # Apply binary thresholding
    _, thresh = cv2.threshold(blurred, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    
    # Show thresholded image
    cv2.imshow("Thresholded Image", thresh)
    
    # Find edges
    edges = cv2.Canny(thresh, 50, 150)
    
    # Show edges
    cv2.imshow("Edges", edges)
    
    # Find contours
    contours, _ = cv2.findContours(edges.copy(), cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    
    # Create a copy of the original image to draw on
    image_with_contours = image.copy()
    cv2.drawContours(image_with_contours, contours, -1, (0, 255, 0), 2)
    cv2.imshow("All Contours", image_with_contours)
    
    # Sort contours by area, largest first
    contours = sorted(contours, key=cv2.contourArea, reverse=True)[:10]
    
    plate_contour = None
    plate_image = None
    
    # Loop through contours to find the license plate
    for contour in contours:
        # Get perimeter of contour
        perimeter = cv2.arcLength(contour, True)
        
        # Approximate the contour shape
        approx = cv2.approxPolyDP(contour, 0.02 * perimeter, True)
        
        # If the shape has 4 vertices, it could be a rectangle (license plate)
        if len(approx) >= 4 and len(approx) <= 6:
            x, y, w, h = cv2.boundingRect(contour)
            
            # Check aspect ratio for UK license plate
            aspect_ratio = float(w) / h
            if 2.0 < aspect_ratio < 7.0:
                plate_contour = contour
                plate_image = image[y:y+h, x:x+w]
                
                # Draw the contour on the original image
                cv2.rectangle(image, (x, y), (x + w, y + h), (0, 255, 0), 2)
                
                # Display the detected plate region
                cv2.imshow("Plate Region", image[y:y+h, x:x+w])
                break
    
    # If no contour matched our criteria, try direct OCR on the whole image
    if plate_image is None:
        print("No plate contour found, attempting direct OCR...")
        # Convert the thresholded image to PIL format
        pil_image = Image.fromarray(thresh)
        
        # Perform OCR with Tesseract
        text = pytesseract.image_to_string(pil_image, config='--psm 11 --oem 3')
        plate_text = clean_and_format_plate(text)
        
        return plate_text, image
    
    # Process the detected plate
    if plate_image is not None:
        # Convert to grayscale
        plate_gray = cv2.cvtColor(plate_image, cv2.COLOR_BGR2GRAY)
        
        # Thresholding to make the text clearer
        _, plate_thresh = cv2.threshold(plate_gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        
        # Convert to PIL Image for OCR
        plate_pil = Image.fromarray(plate_thresh)
        
        # Try multiple OCR configurations
        configs = [
            '--psm 7 --oem 3 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789',
            '--psm 8 --oem 3 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789',
            '--psm 6 --oem 3 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
        ]
        
        best_text = ""
        for config in configs:
            text = pytesseract.image_to_string(plate_pil, config=config)
            print(f"OCR with {config}: {text}")
            
            cleaned = clean_and_format_plate(text)
            if is_valid_uk_plate(cleaned):
                best_text = cleaned
                break
            
            if not best_text and cleaned:
                best_text = cleaned
        
        if best_text:
            return best_text, image
    
    return None, image

def clean_and_format_plate(text):
    """
    Clean and format the recognized text as a UK plate.
    """
    # Remove non-alphanumeric characters
    clean_text = ''.join(c for c in text if c.isalnum()).upper()
    
    # Look for UK plate pattern (e.g., AA00AAA or AA00 AAA)
    if len(clean_text) >= 7:
        # Try to extract the UK plate format
        for i in range(len(clean_text) - 6):
            candidate = clean_text[i:i+7]
            if re.match(r'^[A-Z]{2}\d{2}[A-Z]{3}$', candidate):
                return f"{candidate[:2]}{candidate[2:4]} {candidate[4:]}"
    
    # If specific pattern not found but length is 7, still format as UK plate
    if len(clean_text) == 7:
        return f"{clean_text[:2]}{clean_text[2:4]} {clean_text[4:]}"
    
    return clean_text

def is_valid_uk_plate(text):
    """
    Check if the text matches a UK license plate format.
    """
    # Remove spaces
    text = text.replace(" ", "")
    
    # Check format: two letters, two digits, three letters
    return bool(re.match(r'^[A-Z]{2}\d{2}[A-Z]{3}$', text))

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python simple_detector.py <image_path>")
        sys.exit(1)
    
    plate_text, _ = detect_and_recognize_plate(sys.argv[1])
    
    if plate_text:
        print(f"Recognized plate: {plate_text}")
    else:
        print("No license plate detected or recognized")
    
    cv2.waitKey(0)
    cv2.destroyAllWindows() 
import cv2
import numpy as np
import pytesseract
from PIL import Image
import imutils
import os
import re
from typing import Tuple, Optional

class PlateRecognizer:
    """
    A class for recognizing license plates in images.
    This class implements the ANPR (Automatic Number Plate Recognition) functionality.
    """
    
    def __init__(self):
        """
        Initialize the PlateRecognizer with default parameters.
        """
        self.min_area = 500  # Minimum area for plate detection
        self.max_area = 15000  # Maximum area for plate detection
        # UK license plate pattern: two letters, two numbers, three letters
        self.uk_plate_pattern = re.compile(r'^[A-Z]{2}\d{2}[A-Z]{3}$')
        
    def preprocess_image(self, image: np.ndarray) -> np.ndarray:
        """
        Preprocess the input image for better plate detection.
        
        Args:
            image (np.ndarray): Input image
            
        Returns:
            np.ndarray: Preprocessed image
        """
        # Convert to grayscale
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        
        # Apply bilateral filter to remove noise while keeping edges sharp
        gray = cv2.bilateralFilter(gray, 11, 17, 17)
        
        # Apply histogram equalization to enhance contrast
        gray = cv2.equalizeHist(gray)
        
        # Edge detection
        edged = cv2.Canny(gray, 30, 200)
        
        # Dilate to connect edges
        kernel = np.ones((3, 3), np.uint8)
        edged = cv2.dilate(edged, kernel, iterations=1)
        
        return edged
    
    def find_plate_contours(self, image: np.ndarray, original_image: np.ndarray) -> list:
        """
        Find contours that might contain license plates.
        
        Args:
            image (np.ndarray): Preprocessed image
            original_image (np.ndarray): Original image for size reference
            
        Returns:
            list: List of contours that might contain plates
        """
        # Find contours
        contours = cv2.findContours(image.copy(), cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
        contours = imutils.grab_contours(contours)
        
        # Sort contours by area in descending order
        contours = sorted(contours, key=cv2.contourArea, reverse=True)[:20]
        
        plate_contours = []
        h, w = original_image.shape[:2]
        total_area = h * w
        
        for contour in contours:
            area = cv2.contourArea(contour)
            
            # Filter by area (typical UK plate size)
            # UK plate should be approximately 1-10% of image area
            if 0.01 * total_area < area < 0.1 * total_area:
                # Check aspect ratio (width:height ~= 4.5:1 for UK plates)
                x, y, w, h = cv2.boundingRect(contour)
                aspect_ratio = float(w) / h
                
                if 3.0 < aspect_ratio < 6.0:  # Typical range for UK plates
                    plate_contours.append(contour)
                
        return plate_contours
    
    def extract_plate(self, image: np.ndarray, contour: np.ndarray) -> Optional[np.ndarray]:
        """
        Extract the license plate region from the image.
        
        Args:
            image (np.ndarray): Original image
            contour (np.ndarray): Contour of the plate region
            
        Returns:
            Optional[np.ndarray]: Extracted plate image or None if extraction fails
        """
        try:
            # Get the bounding rectangle
            x, y, w, h = cv2.boundingRect(contour)
            
            # Extract the plate region
            plate = image[y:y+h, x:x+w]
            
            # Resize for better OCR (height of ~50px)
            scale_factor = 50.0 / h
            plate = cv2.resize(plate, None, fx=scale_factor, fy=scale_factor, interpolation=cv2.INTER_CUBIC)
            
            return plate
            
        except Exception as e:
            print(f"Error extracting plate: {str(e)}")
            return None
    
    def enhance_plate_image(self, plate_img: np.ndarray) -> np.ndarray:
        """
        Enhance the plate image for better OCR accuracy.
        
        Args:
            plate_img (np.ndarray): Extracted plate image
            
        Returns:
            np.ndarray: Enhanced plate image
        """
        # Convert to grayscale
        gray = cv2.cvtColor(plate_img, cv2.COLOR_BGR2GRAY)
        
        # Apply Gaussian blur
        blurred = cv2.GaussianBlur(gray, (5, 5), 0)
        
        # Apply adaptive thresholding
        thresh = cv2.adaptiveThreshold(
            blurred, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, 
            cv2.THRESH_BINARY_INV, 11, 2
        )
        
        # Apply morphological operations to remove noise
        kernel = np.ones((3, 3), np.uint8)
        opening = cv2.morphologyEx(thresh, cv2.MORPH_OPEN, kernel, iterations=1)
        
        # Invert back to black text on white background
        return cv2.bitwise_not(opening)
    
    def format_uk_plate(self, text: str) -> str:
        """
        Format recognized text as a UK license plate if possible.
        
        Args:
            text (str): Recognized text
            
        Returns:
            str: Formatted UK plate or original text
        """
        # Remove all non-alphanumeric characters
        clean_text = ''.join(c for c in text if c.isalnum()).upper()
        
        # Check if it matches UK pattern
        if len(clean_text) == 7:
            # Format as AA00 AAA
            return f"{clean_text[:2]}{clean_text[2:4]} {clean_text[4:]}"
        
        return clean_text
    
    def recognize_plate(self, image: np.ndarray) -> Tuple[Optional[str], Optional[np.ndarray]]:
        """
        Recognize the license plate from an image.
        
        Args:
            image (np.ndarray): Input image
            
        Returns:
            Tuple[Optional[str], Optional[np.ndarray]]: 
                - Recognized plate text (or None if recognition fails)
                - Extracted plate image (or None if extraction fails)
        """
        # Make a copy of the original image
        original = image.copy()
        
        # Preprocess the image
        processed = self.preprocess_image(original)
        
        # Find potential plate contours
        contours = self.find_plate_contours(processed, original)
        
        if not contours:
            return None, None
            
        # Try to extract and recognize plate from each contour
        best_confidence = 0
        best_text = None
        best_plate_img = None
        
        for contour in contours:
            plate_img = self.extract_plate(original, contour)
            
            if plate_img is not None:
                # Enhance the plate image
                enhanced = self.enhance_plate_image(plate_img)
                
                # Perform OCR with multiple configurations
                configs = [
                    '--psm 7 --oem 1 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789',
                    '--psm 8 --oem 1 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789',
                    '--psm 13 --oem 1 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
                ]
                
                for config in configs:
                    # Convert to PIL Image for OCR
                    plate_pil = Image.fromarray(enhanced)
                    
                    # Get detailed OCR data
                    ocr_data = pytesseract.image_to_data(
                        plate_pil, config=config, output_type=pytesseract.Output.DICT
                    )
                    
                    # Process OCR results
                    for i, conf in enumerate(ocr_data['conf']):
                        if conf > 0:  # Only consider results with confidence > 0
                            text = ocr_data['text'][i]
                            if text.strip() and float(conf) > best_confidence:
                                best_confidence = float(conf)
                                best_text = text
                                best_plate_img = plate_img
                
        if best_text:
            # Format the recognized text as a UK plate
            formatted_text = self.format_uk_plate(best_text)
            return formatted_text, best_plate_img
        
        return None, None 
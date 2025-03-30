import cv2
import pytesseract
from PIL import Image
import re
import sys

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
    
    return clean_text

def process_image(image_path):
    """
    Direct OCR processing of an image without contour detection.
    """
    # Load the image
    image = cv2.imread(image_path)
    if image is None:
        print(f"Error loading image: {image_path}")
        return None, None
    
    # Display original image
    cv2.imshow("Original Image", image)
    
    # Convert to grayscale
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    
    # Apply thresholding to increase contrast
    _, thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY | cv2.THRESH_OTSU)
    
    # Display processed image
    cv2.imshow("Processed Image", thresh)
    
    # Convert to PIL Image for OCR
    pil_image = Image.fromarray(thresh)
    
    # Create configuration for UK plates
    custom_config = r'--psm 6 --oem 3 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
    
    # Perform OCR
    text = pytesseract.image_to_string(pil_image, config=custom_config)
    
    # Clean and format the recognized text
    formatted_text = clean_and_format_plate(text)
    
    print(f"Raw OCR result: {text}")
    print(f"Formatted plate: {formatted_text}")
    
    # Focus on the plate region if possible
    # (This is a simplified approach, not using contour detection)
    h, w = image.shape[:2]
    # Assume plate is in the center part of the image
    center_region = image[int(h*0.3):int(h*0.7), int(w*0.1):int(w*0.9)]
    
    if center_region.size > 0:
        cv2.imshow("Center Region", center_region)
    
    return formatted_text, image

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python direct_ocr.py <image_path>")
        sys.exit(1)
    
    text, _ = process_image(sys.argv[1])
    print(f"Final result: {text}")
    
    cv2.waitKey(0)
    cv2.destroyAllWindows() 
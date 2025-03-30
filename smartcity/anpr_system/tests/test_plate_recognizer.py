import pytest
import cv2
import numpy as np
from src.plate_recognizer import PlateRecognizer

def test_plate_recognizer_initialization():
    """Test if PlateRecognizer initializes correctly."""
    recognizer = PlateRecognizer()
    assert recognizer.min_area == 100
    assert recognizer.max_area == 5000

def test_preprocess_image():
    """Test image preprocessing."""
    recognizer = PlateRecognizer()
    
    # Create a test image
    test_image = np.zeros((100, 100, 3), dtype=np.uint8)
    processed = recognizer.preprocess_image(test_image)
    
    assert processed.shape == (100, 100)
    assert processed.dtype == np.uint8

def test_find_plate_contours():
    """Test contour detection."""
    recognizer = PlateRecognizer()
    
    # Create a test image with a rectangle
    test_image = np.zeros((100, 100), dtype=np.uint8)
    cv2.rectangle(test_image, (20, 20), (80, 80), 255, -1)
    
    contours = recognizer.find_plate_contours(test_image)
    assert len(contours) > 0

def test_extract_plate():
    """Test plate extraction."""
    recognizer = PlateRecognizer()
    
    # Create a test image with a rectangle
    test_image = np.zeros((100, 100, 3), dtype=np.uint8)
    cv2.rectangle(test_image, (20, 20), (80, 80), (255, 255, 255), -1)
    
    # Create a test contour
    contour = np.array([[20, 20], [80, 20], [80, 80], [20, 80]], dtype=np.int32)
    
    extracted = recognizer.extract_plate(test_image, contour)
    assert extracted is not None
    assert extracted.shape[0] > 0 and extracted.shape[1] > 0 
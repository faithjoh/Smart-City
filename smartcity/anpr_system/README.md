# ANPR (Automatic Number Plate Recognition) System

This is the ANPR system component of the Smart City Parking System. It provides functionality for recognizing license plates from images and storing the results in Firebase.

## Features

- Automatic license plate detection and recognition
- Image preprocessing for better recognition accuracy
- Firebase integration for storing recognition results
- Support for UK license plate format
- **Recognition of GB identifiers and EU symbols on license plates**
- Command-line interface for easy usage

## UK License Plate Recognition Features

The system includes specialized functionality for UK license plates:

1. **UK License Plate Format Recognition**
   - Recognizes standard UK license plate format (e.g., AA00 AAA)
   - Handles various fonts and character spacings
   - Applies format validation for UK plates

2. **Country/Region Identifier Recognition**
   - Detects the blue EU flag section on the left side of the plate
   - Identifies "GB" text within the country identifier section
   - Distinguishes between UK plates with and without EU symbols

3. **Comprehensive Plate Information**
   - Returns both the plate number and country identifier information
   - Provides confidence scores for recognition accuracy
   - Supports multiple plate detection in a single image

## Achievement

✅ **Successfully implemented and tested the ANPR system with UK plates**  
- Successfully recognized test plate: **AA03 BOJ**
- Successfully detected country identifier: **GB**
- The system can distinguish between different country identifiers (GB, EU, etc.)

## Prerequisites

- Python 3.8 or higher
- OpenCV
- Tesseract OCR
- Firebase Admin SDK
- Other dependencies listed in requirements.txt

## Installation

1. Clone the repository
2. Install Tesseract OCR:
   - On macOS: `brew install tesseract`
   - On Ubuntu: `sudo apt-get install tesseract-ocr`
   - On Windows: Download and install from [GitHub](https://github.com/UB-Mannheim/tesseract/wiki)

3. Install Python dependencies:
   ```bash
   pip install -r requirements.txt
   ```

4. Set up Firebase:
   - Create a Firebase project
   - Download the service account key file
   - Place the key file in a secure location

## Usage

### Demo Script

For a quick demonstration of the ANPR functionality, use the provided demo script:

```bash
# Process a single image
python src/anpr_demo.py --image path/to/image.jpg

# Process a directory of images
python src/anpr_demo.py --test_dir path/to/images/directory

# Show processed images with visualization
python src/anpr_demo.py --image path/to/image.jpg --show

# Process sample images in the data directory
python src/anpr_demo.py
```

The demo script will display the recognized license plate numbers and country identifiers for each processed image.

### UK License Plate Recognition

```bash
python src/uk_plate_recognizer.py path/to/image.jpg
```

This will:
1. Detect license plates in the image
2. Recognize the plate number
3. Identify the country/region (GB/EU)
4. Display and print the results

### Standard ANPR System

```bash
python src/main.py --image path/to/image.jpg --location "London" --credentials path/to/firebase-credentials.json
```

### As a Module

```python
from src.uk_plate_recognizer import UKPlateRecognizer

# Initialize the recognizer
recognizer = UKPlateRecognizer()

# Process an image
results = recognizer.process_image('path/to/image.jpg')

# Access results
for plate_id, data in results.items():
    print(f"Plate Number: {data['plate_number']}")
    print(f"Country Identifier: {data['country_identifier']}")
```

## Testing

Run the tests using pytest:

```bash
pytest tests/
```

## Project Structure

```
anpr_system/
├── src/
│   ├── main.py                 # Main entry point
│   ├── plate_recognizer.py     # Core plate recognition logic
│   ├── uk_plate_recognizer.py  # UK-specific plate recognition 
│   ├── firebase_handler.py     # Firebase integration
│   ├── direct_ocr.py           # Direct OCR processing
│   ├── anpr_demo.py            # Demo script for testing
│   └── simple_detector.py      # Simplified detector for testing
├── tests/
│   └── test_plate_recognizer.py
├── data/                       # Directory for test images
│   ├── AA03BOJ.png             # Sample UK license plate image
│   ├── ZK09KXO.png             # Sample UK license plate image
│   ├── ZM09LIW.png             # Sample UK license plate image
│   └── ZN15KCW.png             # Sample UK license plate image
└── requirements.txt
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details. 
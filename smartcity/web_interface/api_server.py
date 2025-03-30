#!/usr/bin/env python
# API Server for Smart City ANPR Admin Panel

import os
import sys
import json
import tempfile
from flask import Flask, request, jsonify
from flask_cors import CORS
import uuid

# Add ANPR system path to Python path
sys.path.append('../anpr_system/src')

# Import ANPR components
try:
    from uk_plate_recognizer import UKPlateRecognizer
except ImportError:
    print("Error: Could not import UKPlateRecognizer. Make sure the anpr_system is accessible.")
    sys.exit(1)

# Initialize Flask app
app = Flask(__name__)
CORS(app)  # Enable CORS for all routes

# Initialize ANPR system
anpr = UKPlateRecognizer()

@app.route('/api/anpr-process', methods=['POST'])
def anpr_process():
    """Process the uploaded image using the ANPR system."""
    if 'image' not in request.files:
        return jsonify({"error": "No image provided"}), 400
    
    try:
        # Get the uploaded file
        uploaded_file = request.files['image']
        
        # Create a temporary file to save the uploaded image
        with tempfile.NamedTemporaryFile(delete=False, suffix='.jpg') as temp_file:
            temp_path = temp_file.name
            uploaded_file.save(temp_path)
        
        # Process the image with the ANPR system
        results = anpr.process_image(temp_path)
        
        # Clean up the temporary file
        os.unlink(temp_path)
        
        # Check if any plate was detected
        if not results:
            return jsonify({
                "plate_number": "UNKNOWN",
                "country_identifier": "UNKNOWN",
                "confidence": 0.0
            })
        
        # Get the first detected plate
        first_plate_key = list(results.keys())[0]
        plate_data = results[first_plate_key]
        
        # Return the recognition results
        return jsonify({
            "plate_number": plate_data["plate_number"],
            "country_identifier": plate_data["country_identifier"],
            "confidence": 0.74  # Placeholder confidence value
        })
        
    except Exception as e:
        print(f"Error processing image: {str(e)}")
        return jsonify({
            "error": f"Error processing image: {str(e)}",
            "plate_number": "ERROR",
            "country_identifier": "UNKNOWN",
            "confidence": 0.0
        }), 500

if __name__ == '__main__':
    # Default port 5000
    port = int(os.environ.get('PORT', 5000))
    
    print(f"Starting ANPR API server on port {port}")
    print("Available endpoints:")
    print("  POST /api/anpr-process - Process an image with ANPR")
    
    # Start the server
    app.run(host='0.0.0.0', port=port, debug=True) 
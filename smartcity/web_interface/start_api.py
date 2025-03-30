#!/usr/bin/env python
"""
ANPR API Server - Cross-Platform Starter
Just double-click this script to start the ANPR system on any operating system.
"""

import os
import sys
import webbrowser
import platform
import socket
from pathlib import Path

# Print header
os_name = platform.system()
print("\n=====================================================")
print(f"Starting ANPR System on {os_name}")
print("=====================================================")

# Add ANPR system path
current_dir = Path(__file__).parent.absolute()
anpr_path = current_dir.parent / "anpr_system" / "src"
sys.path.append(str(anpr_path))

# Function to check if a port is in use
def is_port_in_use(port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        return s.connect_ex(('localhost', port)) == 0

# Find an available port starting from 5000
def find_available_port(start_port=5000, max_attempts=10):
    port = start_port
    for _ in range(max_attempts):
        if not is_port_in_use(port):
            return port
        port += 1
    return start_port + max_attempts  # Return a higher port if all checked are in use

# Install required packages
try:
    # Try to import Flask
    from flask import Flask, request, jsonify
    from flask_cors import CORS
    print("Flask dependencies already installed.")
except ImportError:
    print("Installing Flask dependencies...")
    try:
        import pip
        pip.main(['install', 'flask', 'flask-cors'])
        from flask import Flask, request, jsonify
        from flask_cors import CORS
        print("Flask dependencies installed successfully.")
    except Exception as e:
        print(f"Error installing dependencies: {e}")
        print("Please install Flask manually: pip install flask flask-cors")
        input("Press Enter to exit...")
        sys.exit(1)

# Try to import ANPR system components
try:
    from uk_plate_recognizer import UKPlateRecognizer
except ImportError:
    print("Error: Could not import UKPlateRecognizer.")
    print(f"ANPR system path: {anpr_path}")
    print("Please check that anpr_system directory exists and contains the required files.")
    input("Press Enter to exit...")
    sys.exit(1)

# Create Flask app
app = Flask(__name__)
CORS(app)  # Enable CORS for all routes

# Initialize ANPR system
print("Initializing ANPR system...")
try:
    anpr = UKPlateRecognizer()
    print("ANPR system initialized successfully!")
except Exception as e:
    print(f"Error initializing ANPR system: {e}")
    input("Press Enter to exit...")
    sys.exit(1)

@app.route('/api/anpr-process', methods=['POST'])
def anpr_process():
    """Process the uploaded image"""
    if 'image' not in request.files:
        return jsonify({"error": "No image provided"}), 400
    
    try:
        # Get the uploaded file
        uploaded_file = request.files['image']
        
        # Log file info
        print(f"Processing image file: {uploaded_file.filename}, " 
              f"Content type: {uploaded_file.content_type}, "
              f"Size: {uploaded_file.content_length or 'unknown'} bytes")
        
        # Create a temporary file to save the uploaded image
        import tempfile
        with tempfile.NamedTemporaryFile(delete=False, suffix='.jpg') as temp_file:
            temp_path = temp_file.name
            uploaded_file.save(temp_path)
        
        # Process the image with the ANPR system
        print(f"Processing image: {temp_path}")
        
        try:
            # Try using the UKPlateRecognizer
            results = anpr.process_image(temp_path)
            
            # If no results, try a fallback approach
            if not results:
                print("No plate detected with main algorithm, trying fallback method...")
                
                # Use a simpler direct OCR approach
                import cv2
                import pytesseract
                from PIL import Image
                
                # Read the image
                img = cv2.imread(temp_path)
                
                # Convert to grayscale
                gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
                
                # Apply thresholding
                _, thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
                
                # Use pytesseract to extract text
                text = pytesseract.image_to_string(thresh, config='--psm 7')
                print(f"Fallback OCR detected text: {text}")
                
                # Format the text - remove spaces, newlines, etc.
                text = ''.join(text.split())
                
                # Check if the text looks like a UK license plate
                import re
                uk_plate_pattern = re.compile(r'[A-Z]{2}\d{2}[A-Z]{3}')
                match = uk_plate_pattern.search(text)
                
                if match:
                    plate_text = match.group(0)
                    # Format as AA00 AAA
                    plate_text = f"{plate_text[:4]} {plate_text[4:]}"
                    print(f"Formatted plate text: {plate_text}")
                    
                    # Create a results dictionary
                    results = {
                        "plate_0": {
                            "plate_number": plate_text,
                            "country_identifier": "GB",
                            "region": None,
                            "bbox": None
                        }
                    }
        except Exception as process_error:
            print(f"Error during image processing: {str(process_error)}")
            
            # As a last resort, try to detect text in the image using simple OCR
            try:
                print("Trying basic OCR as last resort...")
                text = pytesseract.image_to_string(Image.open(temp_path))
                
                # Log the raw OCR result
                print(f"Raw OCR result: {text}")
                
                # Look for patterns that might be license plates
                import re
                plate_candidates = re.findall(r'[A-Z0-9]{2,7}', text)
                if plate_candidates:
                    # Use the longest candidate as our best guess
                    best_candidate = max(plate_candidates, key=len)
                    print(f"Best license plate candidate: {best_candidate}")
                    
                    # Create a results dictionary
                    results = {
                        "plate_0": {
                            "plate_number": best_candidate,
                            "country_identifier": "UNKNOWN",
                            "region": None,
                            "bbox": None
                        }
                    }
                else:
                    results = None
            except Exception as ocr_error:
                print(f"Error in basic OCR: {str(ocr_error)}")
                results = None
        
        # Clean up the temporary file
        try:
            os.unlink(temp_path)
        except Exception as e:
            print(f"Warning: Could not delete temporary file {temp_path}: {e}")
        
        # Check if any plate was detected
        if not results:
            print("No license plate detected by any method")
            return jsonify({
                "plate_number": "UNKNOWN",
                "country_identifier": "UNKNOWN",
                "confidence": 0.0
            })
        
        # Get the first detected plate
        first_plate_key = list(results.keys())[0]
        plate_data = results[first_plate_key]
        
        print(f"Detected license plate: {plate_data.get('plate_number', 'UNKNOWN')}, "
              f"Country: {plate_data.get('country_identifier', 'UNKNOWN')}")
        
        # Use actual ANPR detection results rather than hardcoded values
        plate_number = plate_data.get('plate_number', 'UNKNOWN')
        
        # Return the recognition results
        return jsonify({
            "plate_number": plate_number,
            "country_identifier": plate_data.get('country_identifier', 'UNKNOWN'),
            "confidence": 0.74  # Placeholder confidence value
        })
        
    except Exception as e:
        print(f"Error processing image: {str(e)}")
        
        # Additional error details for debugging
        import traceback
        traceback.print_exc()
        
        return jsonify({
            "error": f"Error processing image: {str(e)}",
            "plate_number": "ERROR",
            "country_identifier": "UNKNOWN",
            "confidence": 0.0
        }), 500

# Function to open the web page in the default browser
def open_browser(url):
    try:
        webbrowser.open(url)
        print(f"Opening {url} in default browser...")
    except Exception as e:
        print(f"Could not open browser automatically: {e}")
        print(f"Please manually open: {url}")

if __name__ == '__main__':
    # Find available ports for API and web server
    api_port = find_available_port(5000)
    web_port = find_available_port(8000)
    
    print(f"API server will use port: {api_port}")
    print(f"Web server will use port: {web_port}")
    
    # Update the API endpoint in the JavaScript file
    js_file_path = current_dir / "src" / "plate-reader.js"
    if js_file_path.exists():
        try:
            with open(js_file_path, 'r') as file:
                js_content = file.read()
            
            # Replace the API endpoint port
            import re
            updated_js = re.sub(
                r'const endpoint = \'http://localhost:\d+/api/anpr-process\';', 
                f"const endpoint = 'http://localhost:{api_port}/api/anpr-process';", 
                js_content
            )
            
            with open(js_file_path, 'w') as file:
                file.write(updated_js)
            
            print(f"Updated API endpoint in {js_file_path}")
        except Exception as e:
            print(f"Warning: Could not update API endpoint in JavaScript file: {e}")
            print(f"You may need to manually update the API endpoint to port {api_port} in {js_file_path}")
    
    print("\n=====================================================")
    print("          ANPR API Server starting...               ")
    print("=====================================================")
    print(f"API Server will be available at: http://localhost:{api_port}")
    print(f"Web interface will be available at: http://localhost:{web_port}/public/index.html")
    print("Available endpoints:")
    print(f"  POST http://localhost:{api_port}/api/anpr-process - Process an image with ANPR")
    print("\nAfter servers start, your browser should open automatically.")
    print("If not, please open the web interface URL manually.")
    print("=====================================================")
    
    # Start web server in a separate thread
    import threading
    def run_web_server():
        import http.server
        import socketserver
        
        os.chdir(current_dir)
        handler = http.server.SimpleHTTPRequestHandler
        
        try:
            with socketserver.TCPServer(("", web_port), handler) as httpd:
                print(f"Web server running at http://localhost:{web_port}")
                httpd.serve_forever()
        except Exception as e:
            print(f"Error starting web server: {e}")
            print("You may need to access the API directly or try a different port.")
    
    # Start web server thread
    web_thread = threading.Thread(target=run_web_server)
    web_thread.daemon = True
    web_thread.start()
    
    # Open browser automatically after a few seconds
    def delayed_browser_open():
        import time
        time.sleep(2)  # Give servers time to start
        open_browser(f'http://localhost:{web_port}/public/index.html')
    
    browser_thread = threading.Thread(target=delayed_browser_open)
    browser_thread.daemon = True
    browser_thread.start()
    
    # Start API server
    try:
        app.run(host='0.0.0.0', port=api_port, debug=False)
    except Exception as e:
        print(f"Error starting API server: {e}")
        print("Please check if the port is available or try a different port.")
        input("Press Enter to exit...") 
#!/bin/bash
# Startup script for Smart City ANPR System

# Print header
echo "==============================================="
echo "         Smart City ANPR System Startup         "
echo "==============================================="

# Step 1: Check for Python
echo "Checking for Python..."
if ! command -v python3 &> /dev/null; then
    echo "Python 3 is not installed. Please install Python 3 first."
    exit 1
fi
echo "Python 3 found."

# Step 2: Create virtual environment
echo "Creating virtual environment..."
python3 -m venv anpr_env
source anpr_env/bin/activate

# Step 3: Install dependencies
echo "Installing dependencies..."
pip install flask flask-cors opencv-python numpy pytesseract pillow
pip install -r ../anpr_system/requirements.txt

# Step 4: Start API server
echo "Starting API server on port 5000..."
python api_server.py &
API_PID=$!
echo "API server running with PID: $API_PID"

# Step 5: Start web server
echo "Starting web server on port 8000..."
python -m http.server 8000 &
WEB_PID=$!
echo "Web server running with PID: $WEB_PID"

echo "==============================================="
echo "ANPR System is now running!"
echo "Web interface: http://localhost:8000"
echo "API endpoint: http://localhost:5000/api/anpr-process"
echo "==============================================="
echo "Press Ctrl+C to stop the servers"

# Handle shutdown
trap "kill $API_PID $WEB_PID; echo 'Servers stopped.'; exit 0" INT

# Keep script running
wait 
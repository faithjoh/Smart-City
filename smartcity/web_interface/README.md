# Smart City ANPR Web Interface

This is the web management interface for the Smart City ANPR system. It allows administrators to process license plate images, view recognition results, and manage the database of recognized plates.

## One-Click Real ANPR System Startup

We provide a simple one-click solution that starts the real ANPR system with just a double-click:

1. Simply double-click the `start_api.py` file
2. The system will automatically:
   - Start the ANPR API server
   - Start the Web server
   - Open the application interface in your browser

This allows you to use the real ANPR system for license plate recognition, rather than simulated data.

## Features

- Upload and process license plate images using real ANPR system
- View recognition results with plate numbers and country identifiers
- Store results in a Firebase database
- View and filter database entries
- Dashboard with system metrics

## Using the Real ANPR System

This web interface is now integrated with the actual ANPR system. To use it:

### Option 1: One-Click Starter (Recommended)

We've provided a simple Python script that handles everything for you:

1. Double-click the `start_api.py` file
2. The script will:
   - Start the ANPR API server
   - Start the web server
   - Open the application in your browser

### Option 2: Manual Setup

If you prefer to set things up manually:

1. Create a Python virtual environment:
   ```
   python3 -m venv anpr_env
   source anpr_env/bin/activate
   ```

2. Install required dependencies:
   ```
   pip install flask flask-cors opencv-python numpy pytesseract pillow
   pip install -r ../anpr_system/requirements.txt
   ```

3. Start the API server:
   ```
   python api_server.py
   ```

4. In a new terminal, start the web server:
   ```
   python -m http.server 8000
   ```

5. Open your browser and go to: `http://localhost:8000/public/index.html`

## How It Works

1. When you upload an image in the web interface, it is sent to the Flask API server
2. The API server uses the `UKPlateRecognizer` class from the ANPR system to process the image
3. The ANPR system analyzes the image using computer vision techniques
4. Recognition results are returned to the web interface and displayed
5. Results are saved to the Firebase database for future reference
6. For each plate, a random exit time (1-20 seconds) is generated with a fee of £5 per second

## Troubleshooting

- If you encounter errors with the API server, check the terminal running the API server for error messages
- Ensure that tessseract-ocr is installed on your system (required for OCR functionality)
- Make sure all paths in api_server.py are correct for your system
- Check browser console for JavaScript errors
- Verify that your Firebase configuration is correct

## Project Structure

```
web_interface/
├── api_server.py          # API server for ANPR integration
├── start_anpr_system.sh   # Startup script for the entire system
├── public/                # Public facing web files
│   ├── index.html         # Main HTML file
│   ├── favicon.ico        # Site favicon
│   └── styles/            # CSS stylesheets
├── src/                   # JavaScript source code
│   ├── main.js            # Main application code
│   ├── dashboard.js       # Dashboard functionality
│   ├── plate-reader.js    # License plate reader
│   ├── database-viewer.js # Database viewing functionality
│   └── firebase-config.js # Firebase configuration
└── README.md              # Documentation
```

## Security Considerations

- Ensure Firebase security rules are properly configured
- Add proper authentication before deployment
- Validate uploads on the server to prevent malicious files

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details. 
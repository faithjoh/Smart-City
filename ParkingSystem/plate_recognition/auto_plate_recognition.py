import os
import glob
import time
import re
import random
import math
import cv2
import easyocr
from datetime import datetime

# Firebase Admin SDK
import firebase_admin
from firebase_admin import credentials, firestore

##############################################
# 1. Fuzzy Parsing: Corret common confusions: 0-O, 5-S
##############################################

def fuzzy_parse_uk_plate(plate_raw):
    """
    Performs fuzzy parsing on a license plate by removing spaces and converting to uppercase,
    and corrects common confusions:
      - In letter areas, replace '0' with 'O' and '5' with 'S'.
      - In digit areas, replace 'O' with '0' and 'S' with '5'.
    Only processes if the plate (without spaces) is 7 characters long; otherwise, returns the original string.
    """
    plate = plate_raw.upper().replace(" ", "")
    if len(plate) != 7:
        return plate

    LETTER_FIX = {
        '0': 'O',
        '5': 'S'
    }
    DIGIT_FIX = {
        'O': '0',
        'S': '5'
    }

    prefix = list(plate[:2])    # First 2 characters (should be letters)
    digits = list(plate[2:4])     # Next 2 characters (should be digits)
    suffix = list(plate[4:])      # Last 3 characters (should be letters)


    for i in range(len(prefix)):
        if prefix[i] in LETTER_FIX:
            prefix[i] = LETTER_FIX[prefix[i]]
    for i in range(len(digits)):
        if digits[i] in DIGIT_FIX:
            digits[i] = DIGIT_FIX[digits[i]]
    for i in range(len(suffix)):
        if suffix[i] in LETTER_FIX:
            suffix[i] = LETTER_FIX[suffix[i]]

    return "".join(prefix) + "".join(digits) + "".join(suffix)

def process_final_plate(plate_str):
    """
    If the recognized result contains a region code (e.g., "GB", "UK", "EU", etc.) and the plate,
    then split them, apply fuzzy parsing on the plate part, and combine them.
    For example: "GB LyS0 QEY" becomes "GB LY50 QEY".
    If only the plate is present, directly normalize it.
    """
    possible_regions = {"GB", "UK", "EU", "FR", "DE"}
    parts = plate_str.strip().split(maxsplit=1)
    if len(parts) == 2 and parts[0].upper() in possible_regions:
        region = parts[0].upper()
        raw_plate = parts[1]
        fuzzy_parsed = fuzzy_parse_uk_plate(raw_plate)
        if len(fuzzy_parsed) == 7:
            return region + " " + fuzzy_parsed[:4] + " " + fuzzy_parsed[4:]
        else:
            return region + " " + fuzzy_parsed
    else:
        fuzzy_parsed = fuzzy_parse_uk_plate(plate_str)
        if len(fuzzy_parsed) == 7:
            return fuzzy_parsed[:4] + " " + fuzzy_parsed[4:]
        else:
            return fuzzy_parsed

##############################################
# 2. License Plate Recognition Function (Entry Process)
##############################################

def recognize_plate(image_path):
    print("Reading image:", image_path)
    img = cv2.imread(image_path)
    if img is None:
        print("Failed to load image. Check the path.")
        return None

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    reader = easyocr.Reader(['en'])
    result = reader.readtext(gray, detail=0)
    print("EasyOCR raw output:", result)

    possible_region_codes = {"GB", "UK", "EU", "FR", "DE"}
    region_code = None
    plate_candidate = None

    # Simple strategy: choose candidate text that is between 5 and 10 characters long (ignoring spaces)
    for text in result:
        text_up = text.upper().strip()
        print("Detected text:", text_up)
        if text_up in possible_region_codes:
            region_code = text_up
        else:
            if 5 <= len(text_up.replace(" ", "")) <= 10 and re.search(r"[A-Z0-9]", text_up):
                plate_candidate = text_up

    if not plate_candidate:
        print("No valid plate found in this image.")
        return None

    final_str = plate_candidate
    if region_code:
        final_str = region_code + " " + plate_candidate

    final_plate = process_final_plate(final_str)
    print("Final recognized plate:", final_plate)
    return final_plate

##############################################
# 3. Simulate Exit Process Function
##############################################

def simulate_exit(db, doc_ref, entry_time_str, plate_number):
    """
    Simulates the vehicle exit process:
      - Randomly wait between 5 to 10 seconds.
      - Calculate the parking duration in seconds.
      - Charge at a rate of £5 per second (rounding up).
      - Update the Firestore record with exitTime and paidAmount.
    """
    delay_seconds = random.randint(5, 10)
    print(f"[simulate_exit] Waiting {delay_seconds} seconds before simulating exit...")
    time.sleep(delay_seconds)

    try:
        entry_dt = datetime.strptime(entry_time_str, "%Y-%m-%d %H:%M:%S")
    except Exception as e:
        print(f"[simulate_exit] Error parsing entryTime: {e}")
        return

    exit_dt = datetime.now()
    duration_seconds = (exit_dt - entry_dt).total_seconds()
    cost_per_second = 5
    total_cost = cost_per_second * math.ceil(duration_seconds)
    exit_time_str = exit_dt.strftime("%Y-%m-%d %H:%M:%S")

    doc_ref.update({
        "exitTime": exit_time_str,
        "paidAmount": total_cost
    })
    print(f"[simulate_exit] Plate {plate_number} exited. Duration: {duration_seconds:.2f} sec, Fee: £{total_cost}.")

##############################################
# 4. Initialize Firebase Admin SDK
##############################################

def init_firebase():
    """
    Initialize the Firebase Admin SDK using the serviceAccountKey.json file.
    Returns a Firestore client object.
    """
    cred = credentials.Certificate(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../serviceAccountKey.json"))
    firebase_admin.initialize_app(cred)
    db = firestore.client()
    return db

##############################################
# 5. Main Process: Automatic Batch Processing (Entry and Simulated Exit)
##############################################

def process_folder_periodically(db, image_folder, interval_minutes, max_iterations=5):
    exts = ("*.png", "*.jpg", "*.jpeg", "*.bmp", "*.tiff")
    image_files = []
    for ext in exts:
        image_files.extend(glob.glob(os.path.join(image_folder, ext)))
    if not image_files:
        print("No images found in folder:", image_folder)
        return

    print(f"Found {len(image_files)} images in folder: {image_folder}")

    idx = 0
    count = 0
    while count < max_iterations:
        current_image = image_files[idx]
        print(f"\nProcessing image ({count+1}/{max_iterations}): {current_image}")
        plate = recognize_plate(current_image)
        if plate:
            entry_time_str = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())
            # Write the entry record to Firestore (exitTime is None, paidAmount is 0)
            doc_ref = db.collection('plateNumber').add({
                "plateNumber": plate,
                "entryTime": entry_time_str,
                "exitTime": None,
                "paidAmount": 0
            })
            # add() may return a tuple (WriteResult, DocumentReference), so take the DocumentReference.
            doc_ref = doc_ref[1] if isinstance(doc_ref, tuple) else doc_ref
            print(f"[Entry] Plate {plate} entered at {entry_time_str}. Document ID: {doc_ref.id}")

            # Simulate exit: wait 5-10 seconds then update the record
            simulate_exit(db, doc_ref, entry_time_str, plate)
        else:
            print("No valid plate detected; skipping Firestore write.")

        count += 1
        idx = (idx + 1) % len(image_files)
        if count < max_iterations:
            print(f"Waiting {interval_minutes} minutes before processing next image...\n")
            time.sleep(interval_minutes)
    print(f"Processed {max_iterations} images. Program terminated.")

##############################################
# 6. Main Entry
##############################################

if __name__ == "__main__":
    db = init_firebase()
    # Specify the path to the folder containing license plate images
    folder_path = "/Users/faith/Desktop/SmartCityUKLicencePlateDataset/whiteplate_normal"
    # Set the interval (in minutes) between processing images (e.g., 2 minutes)
    interval_minutes = 2
    # Set the maximum number of images to process (simulate entry and exit)
    max_iterations = 5
    process_folder_periodically(db, folder_path, interval_minutes, max_iterations)

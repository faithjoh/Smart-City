import firebase_admin
from firebase_admin import credentials, firestore
from datetime import datetime
from typing import Optional, Dict, Any

class FirebaseHandler:
    """
    A class for handling Firebase operations related to ANPR system.
    """
    
    def __init__(self, service_account_path: str):
        """
        Initialize Firebase handler with service account credentials.
        
        Args:
            service_account_path (str): Path to the Firebase service account key file
        """
        cred = credentials.Certificate(service_account_path)
        firebase_admin.initialize_app(cred)
        self.db = firestore.client()
        
    def save_plate_recognition(self, 
                             plate_number: str, 
                             image_path: str, 
                             location: Optional[str] = None) -> str:
        """
        Save plate recognition result to Firebase.
        
        Args:
            plate_number (str): Recognized plate number
            image_path (str): Path to the image file
            location (Optional[str]): Location where the plate was recognized
            
        Returns:
            str: Document ID of the saved record
        """
        # Create a new document in the 'plate_recognition' collection
        doc_ref = self.db.collection('plate_recognition').document()
        
        # Prepare the data
        data = {
            'plate_number': plate_number,
            'image_path': image_path,
            'location': location,
            'timestamp': datetime.now(),
            'status': 'active'
        }
        
        # Save to Firebase
        doc_ref.set(data)
        
        return doc_ref.id
        
    def get_plate_recognition(self, doc_id: str) -> Optional[Dict[str, Any]]:
        """
        Retrieve plate recognition record from Firebase.
        
        Args:
            doc_id (str): Document ID of the record
            
        Returns:
            Optional[Dict[str, Any]]: Record data if found, None otherwise
        """
        doc_ref = self.db.collection('plate_recognition').document(doc_id)
        doc = doc_ref.get()
        
        if doc.exists:
            return doc.to_dict()
        return None
        
    def update_plate_status(self, doc_id: str, status: str) -> bool:
        """
        Update the status of a plate recognition record.
        
        Args:
            doc_id (str): Document ID of the record
            status (str): New status to set
            
        Returns:
            bool: True if update was successful, False otherwise
        """
        try:
            doc_ref = self.db.collection('plate_recognition').document(doc_id)
            doc_ref.update({
                'status': status,
                'updated_at': datetime.now()
            })
            return True
        except Exception as e:
            print(f"Error updating plate status: {str(e)}")
            return False 
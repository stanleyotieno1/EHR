import requests
import json
from datetime import datetime, timedelta

# --- Configuration ---
BASE_URL = "http://localhost:8000/api"
# Make sure your application is running on port 8000.

# --- Sample Data (Replace with data from your database) ---
# You must create these users in your database first for the test to work.
DOCTOR_CREDENTIALS = {"workId": "DR001", "password": "password123"} # Replace with a real doctor
PATIENT_CREDENTIALS = {"identifier": "owen@mail.com", "password": "1234admin"} # Replace with a real patient
RECEPTIONIST_CREDENTIALS = {"workId": "REC001", "password": "password123"} # Replace with a real receptionist

# Store IDs retrieved during the test flow
DOCTOR_ID = 4 # Replace with the actual ID of the doctor above
PATIENT_ID = 3 # Replace with the actual ID of the patient above
SLOT_ID = None
APPOINTMENT_ID = None

# Create a session object to persist cookies
session = requests.Session()

def print_step(title):
    """Prints a formatted step title."""
    print("\n" + "="*50)
    print(f"STEP: {title}")
    print("="*50)

def check_response(response, step_name):
    """Checks the HTTP response and prints success or failure."""
    print(f"  Request: {response.request.method} {response.request.url}")
    if response.request.body:
        # Avoid printing large or binary bodies
        try:
            body = json.loads(response.request.body)
            print(f"  Payload: {json.dumps(body, indent=2)}")
        except (json.JSONDecodeError, TypeError):
            print("  Payload: (non-json body)")

    if 200 <= response.status_code < 300:
        print(f"  SUCCESS: {step_name} (Status: {response.status_code})")
        try:
            # Handle empty responses
            if response.text:
                print(f"  Response: {json.dumps(response.json(), indent=2)}")
                return response.json()
            else:
                 print("  Response: (empty)")
                 return None
        except json.JSONDecodeError:
            print(f"  Response: {response.text}")
            return response.text
    else:
        print(f"  !!! FAILURE: {step_name} (Status: {response.status_code})")
        print(f"  Response: {response.text}")
        exit(1) # Exit the script on failure

def login(role, credentials):
    """Logs in a user or staff and stores the cookie in the session."""
    print_step(f"Logging in as {role.upper()}")
    if role == "doctor" or role == "receptionist":
        url = f"{BASE_URL}/v1/auth/staff/login"
    elif role == "patient":
        url = f"{BASE_URL}/v1/auth/users/login"
    else:
        raise ValueError("Invalid role specified")

    response = session.post(url, json=credentials)
    check_response(response, f"Login as {role}")
    print("  Cookie 'jwtToken' received and stored in session.")

def create_appointment_slot():
    """Doctor creates an available time slot."""
    global SLOT_ID
    print_step("Doctor creates an appointment slot")
    url = f"{BASE_URL}/v1/appointments/slots"
    
    # Create a slot for tomorrow at 10 AM
    start_time = datetime.now() + timedelta(days=1)
    start_time = start_time.replace(hour=10, minute=0, second=0, microsecond=0)
    end_time = start_time + timedelta(minutes=30)
    
    payload = {
        "doctorId": DOCTOR_ID,
        "startTime": start_time.isoformat(),
        "endTime": end_time.isoformat()
    }
    
    response = session.post(url, json=payload)
    data = check_response(response, "Create slot")
    SLOT_ID = data.get("id")
    if not SLOT_ID:
        print("  !!! FAILURE: Could not get slot ID from response.")
        exit(1)

def book_appointment():
    """Patient books the created slot."""
    global APPOINTMENT_ID
    print_step("Patient books the appointment slot")
    if not SLOT_ID:
        print("  !!! FAILURE: SLOT_ID not set. Cannot book appointment.")
        exit(1)
        
    url = f"{BASE_URL}/v1/appointments/book"
    payload = {
        "slotId": SLOT_ID,
        "notes": "Automated test: Patient feels a slight fever."
    }
    
    response = session.post(url, json=payload)
    data = check_response(response, "Book appointment")
    APPOINTMENT_ID = data.get("id")
    if not APPOINTMENT_ID:
        print("  !!! FAILURE: Could not get appointment ID from response.")
        exit(1)

def add_doctor_notes():
    """Doctor adds notes to the completed appointment."""
    print_step("Doctor adds notes to the appointment")
    if not APPOINTMENT_ID:
        print("  !!! FAILURE: APPOINTMENT_ID not set. Cannot add notes.")
        exit(1)
        
    url = f"{BASE_URL}/v1/appointments/{APPOINTMENT_ID}/doctor-notes"
    payload = "Automated test: Patient was advised to take paracetamol and rest. - Dr. Test"
    headers = {"Content-Type": "text/plain"}
    
    response = session.put(url, data=payload, headers=headers)
    check_response(response, "Add doctor notes")

def get_available_slots():
    """Fetches and prints available appointment slots."""
    print_step("Get available appointment slots")
    url = f"{BASE_URL}/v1/appointments/slots/available"
    # Fetch for next 3 days
    from_time = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
    to_time = from_time + timedelta(days=3)
    params = {
        "from": from_time.isoformat(),
        "to": to_time.isoformat()
    }
    response = session.get(url, params=params)
    data = check_response(response, "Fetch available slots")
    if isinstance(data, list):
        print(f"  Found {len(data)} available slots.")

def get_patient_appointments():
    """Fetches and prints appointments for the current patient."""
    print_step("Get patient's appointments")
    url = f"{BASE_URL}/v1/appointments/patients/{PATIENT_ID}"
    response = session.get(url)
    data = check_response(response, "Fetch patient appointments")
    if isinstance(data, list):
        print(f"  Found {len(data)} appointments for patient {PATIENT_ID}.")

def get_doctor_appointments():
    """Fetches and prints appointments for the current doctor."""
    print_step("Get doctor's appointments")
    url = f"{BASE_URL}/v1/appointments/doctors/{DOCTOR_ID}/appointments"
    response = session.get(url)
    data = check_response(response, "Fetch doctor appointments")
    if isinstance(data, list):
        print(f"  Found {len(data)} appointments for doctor {DOCTOR_ID}.")

def update_appointment_status(status="CANCELLED"):
    """Updates the status of a previously booked appointment."""
    print_step(f"Update appointment {APPOINTMENT_ID} status to {status}")
    if not APPOINTMENT_ID:
        print("  !!! FAILURE: APPOINTMENT_ID not set. Cannot update status.")
        exit(1)

    url = f"{BASE_URL}/v1/appointments/{APPOINTMENT_ID}/status"
    params = {"newStatus": status}
    
    response = session.put(url, params=params)
    check_response(response, f"Update appointment status")


def run_standard_booking_scenario():
    """The standard flow of a doctor creating a slot and a patient booking it."""
    global APPOINTMENT_ID, SLOT_ID # Ensure we reset global state for this scenario
    APPOINTMENT_ID = None
    SLOT_ID = None
    
    login("doctor", DOCTOR_CREDENTIALS)
    create_appointment_slot()
    
    login("patient", PATIENT_CREDENTIALS)
    book_appointment()
    get_available_slots() # Check that the slot is no longer available
    get_patient_appointments()
    
    login("doctor", DOCTOR_CREDENTIALS)
    get_doctor_appointments()
    add_doctor_notes()
    update_appointment_status("COMPLETED") # Mark as completed

def run_walkin_scenario():
    """Tests the flow of a receptionist creating an appointment for a new walk-in patient."""
    login("doctor", DOCTOR_CREDENTIALS)
    
    print_step("Doctor creates a new slot for the walk-in test")
    url = f"{BASE_URL}/v1/appointments/slots"
    start_time = datetime.now() + timedelta(days=2)
    start_time = start_time.replace(hour=11, minute=0, second=0, microsecond=0)
    end_time = start_time + timedelta(minutes=30)
    payload = {
        "doctorId": DOCTOR_ID,
        "startTime": start_time.isoformat(),
        "endTime": end_time.isoformat()
    }
    response = session.post(url, json=payload)
    data = check_response(response, "Create slot for walk-in test")
    walkin_slot_id = data.get("id")
    if not walkin_slot_id:
        print("  !!! FAILURE: Could not get slot ID for walk-in test.")
        exit(1)

    login("receptionist", RECEPTIONIST_CREDENTIALS)

    print_step("Receptionist creates a walk-in appointment")
    url = f"{BASE_URL}/v1/appointments/walkin"

    unique_id = int(datetime.now().timestamp())
    
    walkin_payload = {
        "firstName": "Walkin",
        "lastName": f"Patient{unique_id}",
        "email": f"walkin.patient{unique_id}@example.com",
        "phoneNumber": f"+1555{unique_id}",
        "dateOfBirth": "1988-08-08",
        "gender": "MALE",
        "address": "456 Walkin Ave, Sometown, USA",
        "bloodGroup": "A_NEGATIVE",
        "genotype": "AS",
        "maritalStatus": "MARRIED",
        "occupation": "Consultant",
        "slotId": walkin_slot_id,
        "notes": "Automated test: Walk-in patient, complaining of chest pain."
    }
    
    response = session.post(url, json=walkin_payload)
    check_response(response, "Create walk-in appointment")


def main():
    """Runs the end-to-end API tests."""
    
    print("\n" + "#"*60)
    print("### RUNNING SCENARIO 1: Standard Booking & Management")
    print("#"*60)
    run_standard_booking_scenario()
    
    print("\n" + "#"*60)
    print("### RUNNING SCENARIO 2: Receptionist Books Walk-in")
    print("#"*60)
    run_walkin_scenario()

    print("\n" + "*"*60)
    print("### ALL E2E TEST SCENARIOS COMPLETED SUCCESSFULLY! ###")
    print("*"*60)


if __name__ == "__main__":
    print("Starting EHR Appointment Feature API Test...")
    print("IMPORTANT: Ensure your Spring Boot application is running on port 8000.")
    print("IMPORTANT: Ensure the sample users (doctor, patient, receptionist) exist in your database.\n")
    main()
import json
import urllib.request

def check_location(location_id: str):
    url = "https://robycohen.pythonanywhere.com/api/location/list"
    headers = {
        "Content-Type": "application/json",
        "Authorization": "Basic d2Frd2F3Ondha3dhdzEyMw==",
        "User-Agent": "okhttp/4.9.0"
    }
    payload = json.dumps({"location_id": [location_id]}).encode("utf-8")
    
    req = urllib.request.Request(url, data=payload, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req) as response:
            status = response.status
            body = response.read().decode("utf-8")
            print(f"Status: {status}")
            print(f"Response: {body}")
            data = json.loads(body)
            for item in data.get("result", []):
                print(f"-> ID: {item.get('location_id')}")
                print(f"-> Lokasi: {item.get('location_name')}")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    print("Testing terminal ID: EPT74560")
    check_location("EPT74560")


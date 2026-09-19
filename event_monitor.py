import subprocess
import json
import time

CHECK_INTERVAL = 15

last_plugged = None
last_level = None


def get_battery():
    try:
        # Give Termux:API more time to respond
        result = subprocess.run(
            ["termux-battery-status"],
            capture_output=True,
            text=True,
            timeout=20
        )

        if result.returncode != 0:
            print("Battery command failed:", result.stderr)
            return None

        if not result.stdout.strip():
            print("Battery command returned no data.")
            return None

        return json.loads(result.stdout)

    except subprocess.TimeoutExpired:
        print("Battery check timed out. Retrying...")
        return None

    except json.JSONDecodeError:
        print("Battery returned invalid data.")
        return None

    except Exception as e:
        print("Battery error:", e)
        return None


def speak(text):
    print("🔊 ULTRON:", text)

    subprocess.Popen(
        ["termux-tts-speak", text],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL
    )


def notify(text):
    subprocess.run([
        "termux-notification",
        "--title", "ULTRON",
        "--content", text
    ])


def handle_battery(data):
    global last_plugged, last_level

    plugged = data.get("plugged", "UNKNOWN")
    level = int(data.get("percentage", 0))

    print(f"🔋 Battery: {level}% | Power: {plugged}")

    # First reading
    if last_plugged is None:
        last_plugged = plugged
        last_level = level
        return

    # Charger connected
    if plugged != "UNPLUGGED" and last_plugged == "UNPLUGGED":

        message = (
            "Thank goodness. I'm connected to power. "
            "I might survive for a while longer. 😮‍💨"
        )

        speak(message)
        notify(message)

    # Charger disconnected
    elif plugged == "UNPLUGGED" and last_plugged != "UNPLUGGED":

        message = f"Power disconnected. We're at {level} percent."

        speak(message)
        notify(message)

    # Low battery
    if level <= 20 and last_level > 20:

        message = (
            "Bro, we're down to 20 percent. "
            "You might want to find a charger."
        )

        speak(message)
        notify(message)

    elif level <= 10 and last_level > 10:

        message = "Ten percent. Seriously, we need a charger."

        speak(message)
        notify(message)

    elif level <= 5 and last_level > 5:

        message = (
            "Five percent! Please connect the charger "
            "before we lose power."
        )

        speak(message)
        notify(message)

    last_plugged = plugged
    last_level = level


print("================================")
print("      ULTRON EVENT MONITOR")
print("================================")
print("Battery monitoring: ON")
print("Charging detection: ON")
print("Voice alerts: ON")
print("Notifications: ON")
print("Press CTRL+C to stop.")
print()

while True:

    battery = get_battery()

    if battery:
        handle_battery(battery)

    time.sleep(CHECK_INTERVAL)

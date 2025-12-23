from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.chrome.options import Options
import sys

# Configure Headless Chrome
options = Options()
options.add_argument("--headless")
options.add_argument("--no-sandbox")
options.add_argument("--disable-dev-shm-usage")

# Default port 8080, can be overridden if your python script accepts args
target_url = "http://localhost:8080"

print(f"Starting E2E Test against {target_url}")

try:
    driver = webdriver.Chrome(options=options)
    driver.get(target_url)

    # Find the H1 element by ID
    heading = driver.find_element(By.ID, "welcome-message").text
    print(f"Detected Heading: {heading}")

    if "Welcome to the JDK 21 App" in heading:
        print("SUCCESS: UI Validation Passed")
        sys.exit(0)
    else:
        print("FAILURE: Heading text did not match")
        sys.exit(1)

except Exception as e:
    print(f"ERROR: {e}")
    sys.exit(1)

finally:
    if 'driver' in locals():
        driver.quit()
import os

from locust import HttpUser, task, between, constant_pacing


AUTH_PORT = os.getenv("AUTH_PORT", "8180")
GATEWAY_PORT = os.getenv("GATEWAY_PORT", "8087")
AUTH_BASE_URL = os.getenv("AUTH_BASE_URL", f"http://localhost:{AUTH_PORT}")
GATEWAY_BASE_URL = os.getenv("GATEWAY_BASE_URL", f"http://localhost:{GATEWAY_PORT}")

DEFAULT_USERNAME = os.getenv("CIRCLEGUARD_USERNAME", "super_admin")
DEFAULT_PASSWORD = os.getenv("CIRCLEGUARD_PASSWORD", "password")


class CircleGuardBaseUser(HttpUser):
    wait_time = between(1, 3)

    def login(self, username=DEFAULT_USERNAME, password=DEFAULT_PASSWORD):
        response = self.client.post(
            "/api/v1/auth/login",
            json={"username": username, "password": password},
            name="auth_login",
        )
        response.raise_for_status()
        payload = response.json()
        return payload["token"], payload["anonymousId"]

    def generate_qr(self, token):
        response = self.client.get(
            "/api/v1/auth/qr/generate",
            headers={"Authorization": f"Bearer {token}"},
            name="auth_generate_qr",
        )
        response.raise_for_status()
        payload = response.json()
        return payload["qrToken"]

    def validate_gate(self, qr_token, name="gate_validate"):
        response = self.client.post(
            "/api/v1/gate/validate",
            json={"token": qr_token},
            name=name,
        )
        response.raise_for_status()
        return response.json()


class AuthLoginLoadUser(CircleGuardBaseUser):
    """Stress test focused on the authentication endpoint."""

    host = AUTH_BASE_URL
    wait_time = constant_pacing(2)

    @task
    def login_throughput(self):
        self.login()


class CampusEntryJourneyUser(CircleGuardBaseUser):
    """End-to-end journey: login -> QR generation -> gate validation."""

    host = AUTH_BASE_URL

    @task
    def complete_campus_entry_journey(self):
        token, _anonymous_id = self.login()
        qr_token = self.generate_qr(token)

        gate_response = self.client.post(
            f"{GATEWAY_BASE_URL}/api/v1/gate/validate",
            json={"token": qr_token},
            name="gateway_validate_qr",
        )
        gate_response.raise_for_status()


class TamperedQrStressUser(CircleGuardBaseUser):
    """Negative flow: validate a tampered QR token against Gateway."""

    host = AUTH_BASE_URL
    wait_time = constant_pacing(1)

    @task
    def reject_tampered_qr(self):
        token, _anonymous_id = self.login()
        qr_token = self.generate_qr(token)
        tampered_token = qr_token[:-1] + ("A" if qr_token[-1] != "A" else "B")

        response = self.client.post(
            f"{GATEWAY_BASE_URL}/api/v1/gate/validate",
            json={"token": tampered_token},
            name="gateway_validate_tampered_qr",
        )
        response.raise_for_status()
        body = response.json()
        if body.get("valid") is True:
            raise AssertionError("Tampered QR token should not validate as valid")


if __name__ == "__main__":
    # Run with: locust -f performance/locustfile.py
    pass
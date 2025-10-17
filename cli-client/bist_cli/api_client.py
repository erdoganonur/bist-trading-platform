"""
HTTP API Client for BIST Trading Platform.
Handles all REST API communication with proper error handling and token management.
"""

import json
from typing import Any, Dict, Optional
from datetime import datetime, timedelta

import httpx
from rich.console import Console

from .config import get_settings
from .utils import get_stored_token, store_token, clear_tokens


console = Console()


class APIClient:
    """HTTP client for BIST Trading Platform API."""

    def __init__(self, base_url: Optional[str] = None, debug: bool = False):
        """
        Initialize API client.

        Args:
            base_url: API base URL. If None, uses settings.
            debug: Enable debug logging
        """
        settings = get_settings()
        self.base_url = base_url or settings.api_base_url
        self.timeout = httpx.Timeout(30.0, connect=10.0)
        self.debug = debug

        self._access_token: Optional[str] = None
        self._refresh_token: Optional[str] = None
        self._token_expiry: Optional[datetime] = None

        # Load stored tokens
        self._load_stored_tokens()

    def _load_stored_tokens(self) -> None:
        """Load tokens from secure storage."""
        access_token = get_stored_token("access_token")
        refresh_token = get_stored_token("refresh_token")

        if access_token:
            self._access_token = access_token
            console.print("[dim]Loaded stored access token[/dim]")

        if refresh_token:
            self._refresh_token = refresh_token

    def _get_headers(self, authenticated: bool = False) -> Dict[str, str]:
        """
        Get HTTP headers for request.

        Args:
            authenticated: Include Authorization header

        Returns:
            Dictionary of headers
        """
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json",
            "User-Agent": "BIST-CLI/1.0.0"
        }

        if authenticated and self._access_token:
            headers["Authorization"] = f"Bearer {self._access_token}"

        return headers

    def _handle_response(self, response: httpx.Response) -> Dict[str, Any]:
        """
        Handle HTTP response.

        Args:
            response: HTTP response

        Returns:
            Parsed JSON response

        Raises:
            APIError: If request failed
        """
        try:
            response.raise_for_status()
            return response.json()
        except httpx.HTTPStatusError as e:
            error_msg = f"HTTP {e.response.status_code}"

            try:
                error_data = e.response.json()
                if "message" in error_data:
                    error_msg = error_data["message"]
                elif "error" in error_data:
                    error_msg = error_data["error"]
            except Exception:
                error_msg = e.response.text or error_msg

            raise APIError(error_msg, status_code=e.response.status_code) from e
        except json.JSONDecodeError as e:
            raise APIError(f"Invalid JSON response: {str(e)}") from e

    def login(self, username: str, password: str) -> Dict[str, Any]:
        """
        Login to BIST platform and get JWT tokens.

        Args:
            username: User's username
            password: User's password

        Returns:
            Login response with tokens and user info

        Raises:
            APIError: If login fails
        """
        url = f"{self.base_url}/api/v1/auth/login"
        payload = {
            "username": username,
            "password": password
        }

        with httpx.Client(timeout=self.timeout) as client:
            response = client.post(url, json=payload, headers=self._get_headers())
            data = self._handle_response(response)

            # Store tokens
            if "accessToken" in data:
                self._access_token = data["accessToken"]
                store_token("access_token", self._access_token)

            if "refreshToken" in data:
                self._refresh_token = data["refreshToken"]
                store_token("refresh_token", self._refresh_token)

            # Calculate token expiry (default 15 minutes)
            self._token_expiry = datetime.now() + timedelta(minutes=15)

            return data

    def refresh_access_token(self) -> Dict[str, Any]:
        """
        Refresh access token using refresh token.

        Returns:
            Refresh response with new access token

        Raises:
            APIError: If refresh fails
        """
        if not self._refresh_token:
            raise APIError("No refresh token available")

        url = f"{self.base_url}/api/v1/auth/refresh"
        headers = {
            **self._get_headers(),
            "Authorization": f"Bearer {self._refresh_token}"
        }

        with httpx.Client(timeout=self.timeout) as client:
            response = client.post(url, headers=headers)
            data = self._handle_response(response)

            if "accessToken" in data:
                self._access_token = data["accessToken"]
                store_token("access_token", self._access_token)
                self._token_expiry = datetime.now() + timedelta(minutes=15)

            return data

    def get(self, endpoint: str, params: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """
        Make GET request.

        Args:
            endpoint: API endpoint (e.g., "/api/v1/users/profile")
            params: Query parameters

        Returns:
            Response data

        Raises:
            APIError: If request fails
        """
        url = f"{self.base_url}{endpoint}"

        if self.debug:
            console.print(f"[dim]→ GET {url}[/dim]")
            if params:
                console.print(f"[dim]  Params: {params}[/dim]")

        with httpx.Client(timeout=self.timeout) as client:
            response = client.get(
                url,
                params=params,
                headers=self._get_headers(authenticated=True)
            )

            if self.debug:
                console.print(f"[dim]← Status: {response.status_code}[/dim]")
                try:
                    resp_json = response.json()
                    console.print(f"[dim]  Response: {json.dumps(resp_json, indent=2)}[/dim]")
                except:
                    console.print(f"[dim]  Response: {response.text[:200]}...[/dim]")

            return self._handle_response(response)

    def post(
        self,
        endpoint: str,
        data: Optional[Dict[str, Any]] = None,
        authenticated: bool = True
    ) -> Dict[str, Any]:
        """
        Make POST request.

        Args:
            endpoint: API endpoint
            data: Request body
            authenticated: Include auth token

        Returns:
            Response data

        Raises:
            APIError: If request fails
        """
        url = f"{self.base_url}{endpoint}"

        with httpx.Client(timeout=self.timeout) as client:
            response = client.post(
                url,
                json=data,
                headers=self._get_headers(authenticated=authenticated)
            )
            return self._handle_response(response)

    def put(self, endpoint: str, data: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Make PUT request."""
        url = f"{self.base_url}{endpoint}"

        with httpx.Client(timeout=self.timeout) as client:
            response = client.put(
                url,
                json=data,
                headers=self._get_headers(authenticated=True)
            )
            return self._handle_response(response)

    def delete(self, endpoint: str) -> Dict[str, Any]:
        """Make DELETE request."""
        url = f"{self.base_url}{endpoint}"

        with httpx.Client(timeout=self.timeout) as client:
            response = client.delete(
                url,
                headers=self._get_headers(authenticated=True)
            )
            return self._handle_response(response)

    def logout(self) -> None:
        """Logout and clear tokens."""
        try:
            # Call logout endpoint if available
            self.post("/api/v1/auth/logout")
        except Exception:
            pass  # Best effort
        finally:
            self._access_token = None
            self._refresh_token = None
            self._token_expiry = None
            clear_tokens()

    def is_authenticated(self) -> bool:
        """Check if user is authenticated."""
        return self._access_token is not None

    def test_connection(self) -> bool:
        """
        Test API connection.

        Returns:
            True if API is reachable and healthy
        """
        try:
            url = f"{self.base_url}/actuator/health"
            with httpx.Client(timeout=httpx.Timeout(5.0)) as client:
                response = client.get(url)
                return response.status_code == 200
        except Exception:
            return False


class APIError(Exception):
    """API request error."""

    def __init__(self, message: str, status_code: Optional[int] = None):
        """
        Initialize API error.

        Args:
            message: Error message
            status_code: HTTP status code if available
        """
        super().__init__(message)
        self.message = message
        self.status_code = status_code

    def __str__(self) -> str:
        """String representation."""
        if self.status_code:
            return f"API Error ({self.status_code}): {self.message}"
        return f"API Error: {self.message}"

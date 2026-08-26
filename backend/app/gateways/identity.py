from typing import Protocol

import firebase_admin
from firebase_admin import auth
from firebase_admin.exceptions import FirebaseError

from app.domain.auth import AuthenticatedIdentity


class InvalidIdentityTokenError(ValueError):
    pass


class IdentityTokenVerifier(Protocol):
    def verify(self, token: str) -> AuthenticatedIdentity: ...


class FirebaseIdentityTokenVerifier:
    def __init__(self, project_id: str | None = None) -> None:
        try:
            self._app = firebase_admin.get_app()
        except ValueError:
            options = {"projectId": project_id} if project_id else None
            self._app = firebase_admin.initialize_app(options=options)

    def verify(self, token: str) -> AuthenticatedIdentity:
        try:
            decoded = auth.verify_id_token(token, app=self._app)
            firebase_uid = str(decoded["uid"]).strip()
            if not firebase_uid:
                raise ValueError("Firebase UID is missing.")
            return AuthenticatedIdentity(
                firebase_uid=firebase_uid,
                email=self._optional_text(decoded.get("email")),
                display_name=self._optional_text(decoded.get("name")),
            )
        except (FirebaseError, KeyError, TypeError, ValueError) as error:
            raise InvalidIdentityTokenError("Firebase ID token is not valid.") from error

    @staticmethod
    def _optional_text(value: object) -> str | None:
        text = str(value).strip() if value is not None else ""
        return text or None

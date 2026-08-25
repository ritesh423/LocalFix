from typing import Protocol

import firebase_admin
from firebase_admin import messaging
from firebase_admin.exceptions import FirebaseError


class PushDeliveryError(RuntimeError):
    pass


class PushGateway(Protocol):
    def send(
        self,
        firebase_installation_id: str,
        title: str,
        body: str,
        data: dict[str, str],
    ) -> str: ...


class FirebasePushGateway:
    def __init__(self, project_id: str | None = None) -> None:
        try:
            self._app = firebase_admin.get_app()
        except ValueError:
            options = {"projectId": project_id} if project_id else None
            self._app = firebase_admin.initialize_app(options=options)

    def send(
        self,
        firebase_installation_id: str,
        title: str,
        body: str,
        data: dict[str, str],
    ) -> str:
        try:
            return messaging.send(
                messaging.Message(
                    notification=messaging.Notification(title=title, body=body),
                    android=messaging.AndroidConfig(
                        notification=messaging.AndroidNotification(
                            channel_id="localfix_updates"
                        )
                    ),
                    data=data,
                    fid=firebase_installation_id,
                ),
                app=self._app,
            )
        except (FirebaseError, ValueError) as error:
            raise PushDeliveryError(str(error)) from error

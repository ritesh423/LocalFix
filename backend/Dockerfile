FROM python:3.13.13-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

RUN addgroup --system localfix && adduser --system --ingroup localfix localfix

COPY requirements.txt ./
RUN pip install --no-cache-dir --requirement requirements.txt

COPY alembic ./alembic
COPY alembic.ini ./alembic.ini
COPY app ./app

USER localfix

EXPOSE 8000

CMD ["sh", "-c", "alembic upgrade head && python -m app.cli.bootstrap_personal && exec uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000} --proxy-headers --forwarded-allow-ips='*'"]

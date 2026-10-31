"""Ajustes solo para `manage.py test`. La aplicación sigue usando PostgreSQL en runtime."""

import os

os.environ.setdefault('SECRET_KEY', 'test-secret-key-evaluacion-resultados')
os.environ.setdefault('DEBUG', 'True')
os.environ.setdefault('POSTGRES_DB', 'test_unused')
os.environ.setdefault('POSTGRES_USER', 'test')
os.environ.setdefault('POSTGRES_PASSWORD', 'test')
os.environ.setdefault('POSTGRES_HOST', 'localhost')
os.environ.setdefault('POSTGRES_PORT', '5432')

from .settings import *  # noqa: E402,F403

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.sqlite3',
        'NAME': ':memory:',
    }
}

EMAIL_BACKEND = 'django.core.mail.backends.locmem.EmailBackend'
PASSWORD_HASHERS = ['django.contrib.auth.hashers.MD5PasswordHasher']

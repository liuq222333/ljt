def create_app():
    from .api import create_app as _create_app
    return _create_app()


try:
    app = create_app()
except ModuleNotFoundError:
    app = None


__all__ = ["app", "create_app"]

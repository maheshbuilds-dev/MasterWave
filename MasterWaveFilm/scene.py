"""MasterWave cinematic scene - product launch film using user screenshots."""
from engine import BigText, Card, Counter, Film, PenMark, Route

ACCENT = (198, 92, 46)
INK = (34, 32, 30)
WHITE = (245, 245, 245)

def build_film(doc):
    L, E, W = doc.L, doc.E, doc.W
    film = Film(doc.duration, fps=30, look="charcoal")

    # 1 Title card
    film.add(BigText("MASTERWAVE", -4000, -2250, 1200, L(1) + 0.05, rgb=WHITE, shadow=True, type_on=0.8))
    film.add(BigText("The Premium Music Player", -4000, -800, 400, L(1) + 0.3, rgb=(180, 180, 180), shadow=True, type_on=0.5))

    # 2 Home screen
    film.add(BigText("FREE & ANONYMOUS", -1000, 500, 600, L(2) + 0.2, rgb=WHITE, shadow=True, type_on=0.7))
    film.add(BigText("No subscriptions. No paywalls. Just music.", -1000, 1100, 300, L(2) + 0.4, rgb=(180, 180, 180), shadow=True, type_on=0.5))

    # 3 Glassy UI
    film.add(BigText("REAL-TIME LYRICS", 3000, 500, 600, L(3) + 0.2, rgb=WHITE, shadow=True, type_on=0.7))
    film.add(BigText("Powered by YouTube Music", 3000, 1100, 300, L(3) + 0.4, rgb=(180, 180, 180), shadow=True, type_on=0.5))

    # 4 Liquid Glass UI
    film.add(BigText("LIQUID GLASS UI", 7000, 500, 600, L(4) + 0.2, rgb=WHITE, shadow=True, type_on=0.7))
    film.add(BigText("Obsidian black • Deep crimson accents", 7000, 1100, 300, L(4) + 0.4, rgb=(180, 180, 180), shadow=True, type_on=0.5))

    # 5 CTA
    film.add(BigText("DOWNLOAD NOW", 11000, 2000, 900, L(5) + 0.2, rgb=ACCENT, shadow=True, type_on=0.6))

    # Camera movement through the scenes
    film.shot(0.0, -4000, -2250, 0.4)
    film.shot(E(1) + 0.4, -4000, -2250, 0.25)
    film.shot(L(2) + 0.2, -1000, 500, 0.4)
    film.shot(E(2) - 0.1, -1000, 500, 0.3)
    film.shot(L(3) + 0.2, 3000, 500, 0.4)
    film.shot(E(3) - 0.1, 3000, 500, 0.3)
    film.shot(L(4) + 0.2, 7000, 500, 0.4)
    film.shot(E(4) - 0.1, 7000, 500, 0.3)
    film.shot(L(5) + 0.2, 11000, 2000, 0.3)
    film.shot(film.duration, 11000, 2000, 0.2)

    return film

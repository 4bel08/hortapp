# Gestor de l'hort – Guia per compilar l'APK a GitHub

Aquest projecte conté el codi natiu d'Android complet a la carpeta `android/` i un flux de treball automatitzat a `.github/workflows/build-apk.yml`.

Com que no necessiteu tenir instal·lat Android Studio ni cap SDK al vostre Surface Laptop 7, **GitHub compilarà l'APK per vosaltres al núvol de franc en 2 minuts**.

---

### Pas 1: Pujar el projecte a GitHub

1. Aneu a [github.com](https://github.com) i creeu un nou repositori (pot ser **públic** o **privat**), per exemple anomenat `hort-android`.
2. Si teniu **Git** o **GitHub Desktop** instal·lat, podeu fer:
   ```bash
   git init
   git add .
   git commit -m "Initial Android PWA project with Edge-to-Edge and Widget"
   git branch -M main
   git remote add origin https://github.com/EL_VOSTRE_USUARI/hort-android.git
   git push -u origin main
   ```
   *(Si no teniu Git instal·lat, podeu instal·lar-lo ràpidament amb `winget install Git.Git` o obrir la carpeta a [GitHub Desktop](https://desktop.github.com/)).*

---

### Pas 2: Obtenir l'APK compilat

1. Tan bon punt pugeu el codi a GitHub, aneu a la pestanya **Actions** del vostre repositori.
2. Veureu una tasca en curs anomenada **"Build Native APK"**.
3. En ~2 minuts finalitzarà amb èxit (marca verda).
4. Feu clic a l'execució i a sota, a la secció **Artifacts**, trobareu el fitxer **`Gestor-de-l-hort-Debug-APK`**.
5. Descarregueu el fitxer ZIP, descomprimiu-lo i transferiu el fitxer `.apk` al vostre telèfon Android (o descarregueu-lo directament des del navegador del mòbil a GitHub).
6. Toqueu el fitxer `.apk` al mòbil i premeu **Instal·la**!

---

### Funcionalitats natives incloses

- **Vista Edge-to-Edge**: S'estén per darrere de la barra d'estat i de navegació amb gestos, adaptant la brillantor de les icones segons el mode fosc o clar.
- **Icona temàtica (Material You - Android 13+)**: Compatible amb la tematització de colors de fons de pantalla del sistema gràcies a la capa monocromàtica.
- **Dreceres d'aplicació (App Shortcuts)**: En mantenir premuda la icona al llançador d'Android apareixen accessos directes a *Plànol*, *Resum*, *Historial*, *Calendari* i *Planter*.
- **Widget natiu de pantalla d'inici**: Afegiu el giny del *Gestor de l'hort* a la vostra pantalla d'inici per consultar els cultius actius i obrir ràpidament les seccions principals.
- **Selector de fitxers i descàrregues**: El botó d'importar còpies de seguretat en JSON obre el selector de fitxers natiu d'Android, i l'exportació de dades obre el menú natiu de compartir/desar.
- **Totalment fora de línia (Offline)**: Tots els fitxers de l'aplicació s'allotgen a l'emmagatzematge intern de l'aplicació natiu mitjançant `WebViewAssetLoader`.

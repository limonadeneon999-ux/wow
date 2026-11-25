# Android Auto : ce qui est possible, ce qui ne l'est pas

Ce document est la réponse honnête à la question « est-ce que je peux remplacer Android Auto
par ma propre app, et afficher toutes mes apps dessus ? ».

---

## 1. Comment Android Auto fonctionne vraiment

Android Auto n'est pas une app qui dessine l'écran de la voiture. C'est un **protocole de
projection** entre deux ordinateurs :

```
  TELEPHONE                                        VOITURE (autoradio)
+---------------------------+                    +---------------------------+
| app Android Auto          |   USB / Wi-Fi      | recepteur Android Auto    |
| (com.google.android.      | -----------------> | (firmware du constructeur)|
|  projection.gearhead)     |  modeles + flux    |                           |
|                           | <----------------- |  DESSINE l'interface      |
| envoie des MODELES :      |   toucher, boutons |  avec SON propre style    |
|  - liste / grille         |                    |                           |
|  - carte navigation       |                    |                           |
|  - lecteur media          |                    |                           |
+---------------------------+                    +---------------------------+
```

Conséquence directe : **les pixels que tu vois sur l'écran de la voiture sont dessinés par
l'autoradio**, pas par le téléphone. Le téléphone n'envoie que des « modèles » (une liste, une
carte, un lecteur) et des événements tactiles. C'est pour ça qu'Android Auto n'a pas la même
tête dans une Kia, une Porsche ou une Renault : chaque constructeur dessine les modèles à sa façon.

Donc « changer l'apparence d'Android Auto » depuis le téléphone, c'est limité à ce que le
protocole autorise (jour/nuit, quelques couleurs de carte, la disposition des modèles). Le reste
appartient au firmware de l'autoradio.

## 2. La signature Google : le mur

Tu as vu juste. La poignée de main Android Auto repose sur des certificats :

- le téléphone présente une chaîne de certificats que l'autoradio d'origine **vérifie** ;
- cette chaîne est émise dans l'écosystème Google (Play Services / GMS) ;
- un APK modifié puis **re-signé** avec une autre clé ne passe pas : l'autoradio refuse la
  connexion, ou Play Services refuse de démarrer la projection.

On ne peut pas obtenir la clé privée de Google. Donc :

> **On ne peut pas installer « ta version d'Android Auto » sur un autoradio d'origine et
> projeter dessus.** Ce n'est pas une question de root, de patch ou de talent : c'est de la
> cryptographie asymétrique.

Le root ne casse pas cette barrière. Il donne d'autres leviers (voir §3).

## 3. Ce que le root permet réellement

Sur un téléphone rooté (Magisk / KernelSU / APatch), avec un autoradio d'origine :

1. **Débloquer les « sources inconnues »** dans les réglages développeur d'Android Auto.
   → Les apps tierces peuvent apparaître, **mais** elles doivent quand même implémenter les
   modèles Android Auto (Car App Library, ou app média / messagerie). Une app « normale » ne
   s'affiche toujours pas.
2. **Installer des apps de miroir** (Screen2Auto, ScreenOnAuto, AA Mirror, Fermata Auto,
   CarStream, KM Auto de KoalaMirror…). → Le téléphone est affiché en miroir sur l'écran de la
   voiture : **là, toutes les apps apparaissent**, y compris YouTube, Netflix, ton launcher, etc.
   C'est la voie pratique pour « toutes les apps ».
3. **Modules LSPosed / Zygisk** pour retourner des drapeaux cachés (Phenotype Patcher,
   AA AIO Tweaker…). → Déblocage de fonctions, pas de remplacement de l'interface.
4. **Remplacer l'autoradio** si c'est un autoradio Android aftermarket (ou une voiture AAOS) :
   là, aucune limite, c'est un Android normal.

Limites à connaître : Google durcit régulièrement Android Auto ; le résultat dépend de la
version d'AA, du modèle de téléphone et de l'autoradio. Le miroir peut être capricieux en
sans-fil (latence, résolution), et toucher l'écran en roulant est encadré par la loi.

## 4. Les trois chemins vers « ma propre interface de voiture »

| Chemin | Matériel | Effort | Résultat |
|---|---|---|---|
| **A. Miroir d'écran** | Téléphone rooté + autoradio d'origine | Faible | Ton app en plein écran sur l'écran AA, toutes les apps accessibles |
| **B. Autoradio Android / AAOS** | Autoradio aftermarket ou voiture AAOS | Faible | Ton app comme accueil natif, intégration parfaite |
| **C. Récepteur ouvert** | Raspberry Pi + Crankshaft / OpenAuto | Élevé | Remplacement complet du protocole AA côté récepteur |

Le chemin **C** est le seul qui « remplace Android Auto » de bout en bout. Il existe des projets
qui réécrivent le protocole côté téléphone (par ex. BetterAuto) et qui fonctionnent **uniquement**
sur des récepteurs ouverts (Crankshaft, OpenAuto), précisément parce que ces récepteurs
n'exigent pas le certificat Google. Sur un autoradio de série, ils ne peuvent pas se connecter.

## 5. Ce que fait Car Studio

Car Studio prend le chemin **A** (et marche aussi en **B**) : c'est une interface de voiture
complète, très personnalisable, qui s'affiche en plein écran sur l'écran de la voiture via le
miroir, ou qui devient l'accueil d'un autoradio Android.

Ce n'est pas de la triche : c'est la seule voie qui donne *à la fois* toutes les apps *et* une
interface entièrement à toi, sans clé Google.

## 6. Questions pour affiner

1. Téléphone : modèle, version d'Android, méthode de root (Magisk / KernelSU / APatch) ?
2. LSPosed installé ? Version d'Android Auto ?
3. Voiture : marque / modèle / année. Autoradio d'origine, aftermarket Android, ou AAOS ?
4. Android Auto filaire ou sans fil ? Écran tactile ou molette ?
5. Ce qui compte le plus : (a) accéder à toutes les apps, (b) le look, (c) des apps précises
   (YouTube, Netflix, Waze…), (d) des widgets véhicule (OBD) ?
6. Format de l'écran : résolution et orientation (paysage large, portrait, split) ?
